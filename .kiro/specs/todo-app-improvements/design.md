# Design Document

## Overview

本ドキュメントは、`jp.itigotti` パッケージの Todo アプリケーションに存在する 9 件の問題点を修正するための設計を定義する。
修正対象は以下のとおり：ResultSet リーク、MainTest の本番 DB 汚染、JUnit バージョン誤り、スキーマ不整合、削除時 UI フィードバック欠如、テストカバレッジ不足、Connection 管理（HikariCP 導入）、関心の分離（リスナー管理移動）、モジュール名の誤り。

---

## Architecture

既存のレイヤー構成を維持する。

```
UI Layer      : Main (JavaFX Application), Controller (FXML Controller)
Business Layer: TodoListLogic
Data Layer    : TodoDAO (HikariCP + SQLite)
Model         : TodoItemModel
Utility       : StringUtil
```

主な構造変更：

- `TodoDAO` が `DriverManager` から `HikariDataSource` に移行し、`AutoCloseable` を実装する。
- `TodoItemModel` からリスナー管理フィールドを除去し、`TodoListLogic` が `Set<Integer>` の `ListenerRegistry` で管理する。
- `MainTest` が `@TempDir` + コンストラクタインジェクションで本番 DB と分離される。

---

## Components

### TodoDAO

- `HikariDataSource` フィールドを持ち、全 CRUD で `dataSource.getConnection()` を使用する。
- `DriverManager.getConnection()` の直接呼び出しを持たない。
- `AutoCloseable` を実装し `close()` で `HikariDataSource` を閉じる。
- コンストラクタは `()` と `(String customDbUrl)` の 2 つを提供し、テストでカスタム URL を渡せる。
- `initializeDB()` で `expiration_date NOT NULL` 制約付きテーブルを作成し、既存 NULL 行を `LocalDate.now()` で補完するマイグレーションを実行する。
- `create(TodoItemModel)` で `expiration_date` が `null` の場合は `IllegalArgumentException` をスローする。
- `create()` および `findAll()` で `ResultSet` を try-with-resources でクローズする。

### TodoListLogic

- `Set<Integer>` 型の `listenerRegistry` フィールドを追加する。
- `setupItemListener()` でリスナー登録前に `listenerRegistry.contains(item.getId())` をチェックし、未登録の場合のみリスナーを追加して ID を登録する。
- `refresh()` 時に `listenerRegistry` はクリアしない（ID ベースで重複チェックするため）。

### TodoItemModel

- `hasCompletedListener`、`isListenerInstalled()`、`setListenerInstalled()` を削除する。
- それ以外のプロパティ（`id`、`task`、`expirationDate`、`completed`）は変更なし。

### Controller

- `handleDeleteAction()` で選択アイテムが `null` の場合、`AlertType.WARNING` ダイアログを表示する。
  - タイトル: `"削除エラー"`
  - コンテンツテキスト: タスクを選択するよう促すメッセージ

### MainTest

- `@TempDir` で一時ディレクトリを生成し、`TodoDAO(customDbUrl)` を経由して `Controller` を差し込む。
- `Main.init()` を直接呼び出さず、`Main.start(Stage)` に必要な `TodoDAO` をコンストラクタインジェクションで渡す。

### pom.xml / module-info.java

- `junit-jupiter` バージョンを `5.11.4` に変更。
- `com.zaxxer:HikariCP` を `compile` スコープで追加。
- `module-info.java` のモジュール名を `jp.itigotti` に変更。
- `javafx-maven-plugin` および `jpackage-maven-plugin` の参照を `jp.itigotti/jp.itigotti.Main` に更新。

---

## Interfaces

### TodoDAO

```java
public class TodoDAO implements AutoCloseable {
    public TodoDAO();
    public TodoDAO(String customDbUrl);
    public void initializeDB();
    public List<TodoItemModel> findAll();
    public TodoItemModel create(TodoItemModel item);   // expiration_date == null -> IllegalArgumentException
    public TodoItemModel update(TodoItemModel item);
    public boolean delete(TodoItemModel item);
    @Override public void close();
}
```

### TodoListLogic

```java
public class TodoListLogic {
    private final Set<Integer> listenerRegistry = new HashSet<>();
    public TodoListLogic(TodoDAO dao);
    public ObservableList<TodoItemModel> getTodoItems();
    public void addTodoItem(String task, LocalDate expirationDate);
    public void removeTodoItem(TodoItemModel item);
    public void refresh();
    private void setupItemListener(TodoItemModel item);  // listenerRegistry でガード
}
```

### TodoItemModel (変更後)

```java
public class TodoItemModel {
    // id, task, expirationDate, completed プロパティのみ
    // hasCompletedListener / isListenerInstalled / setListenerInstalled を削除
}
```

---

## Data Models

### todo_items テーブル (変更後)

| カラム          | 型      | 制約                          |
|----------------|---------|-------------------------------|
| id             | INTEGER | PRIMARY KEY AUTOINCREMENT     |
| task           | TEXT    | NOT NULL                      |
| expiration_date| DATE    | NOT NULL                      |
| is_completed   | BOOLEAN | NOT NULL DEFAULT false        |

### マイグレーションフロー（initializeDB()）

1. `CREATE TABLE IF NOT EXISTS` で上記スキーマのテーブルを作成。
2. `todo_items` が既に存在する場合、`expiration_date IS NULL` の行を検出。
3. 検出された行に対して `UPDATE todo_items SET expiration_date = ? WHERE expiration_date IS NULL` を実行し `LocalDate.now()` を適用。
4. 処理後は全行の `expiration_date` が非 NULL となる。

---

## Error Handling

| 状況 | 対応 |
|------|------|
| `create()` に `expiration_date = null` | `IllegalArgumentException` をスロー（呼び出し元の `TodoListLogic` で入力済みバリデーション済みだが、DAO 層でも防御） |
| SQL 例外 | `RuntimeException` でラップしてスロー（既存の挙動を維持） |
| 削除ボタン押下時に選択なし | `AlertType.WARNING` ダイアログを表示、例外なし |
| `HikariDataSource` 取得失敗 | `RuntimeException` でラップ（HikariCP の標準挙動） |
| リスナー登録の重複 | `listenerRegistry` で防止、例外なし |

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: NULL 期限日マイグレーション後の完全性

*For any* `todo_items` テーブル内の NULL 行セット（0 件以上）に対し、`initializeDB()` を呼び出した後は `findAll()` が返す全アイテムの `expirationDate` が非 null であること。

**Validates: Requirements 4.2**

---

### Property 2: null 期限日での create() は必ず拒否

*For any* 有効なタスク文字列と `null` の期限日の組み合わせに対して、`TodoDAO.create()` は `IllegalArgumentException` をスローすること。

**Validates: Requirements 4.4**

---

### Property 3: addTodoItem() のバリデーション

*For any* null・空文字・空白のみの文字列、あるいは null の期限日を `TodoListLogic.addTodoItem()` に渡したとき、`IllegalArgumentException` がスローされ、タスクリストのサイズが変化しないこと。

**Validates: Requirements 6.2**

---

### Property 4: update ラウンドトリップ

*For any* データベースに登録済みの `TodoItemModel` に対して任意のフィールド値（task・expirationDate・completed）を変更して `update()` を呼び出した後、`findAll()` で取得した同一 ID のアイテムが更新後の値を持つこと。

**Validates: Requirements 6.3**

---

### Property 5: delete 後の不在保証

*For any* データベースに登録済みの `TodoItemModel` のセットから任意の 1 件を `delete()` で削除した後、`findAll()` の結果にその ID が含まれないこと。

**Validates: Requirements 6.4**

---

### Property 6: リスナー重複登録防止

*For any* `TodoListLogic` インスタンスに対して `refresh()` または `addTodoItem()` を任意の回数呼び出した後、同一 ID の `TodoItemModel` に対して完了状態変更リスナーが 1 回だけ登録されていること（completed を 1 回トグルしたとき `TodoDAO.update()` がちょうど 1 回呼び出される）。

**Validates: Requirements 8.2, 8.3, 8.4**
