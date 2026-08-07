# Implementation Plan: Todo App Improvements

## Overview

9 件の問題点（ResultSet リーク、MainTest の本番 DB 汚染、JUnit バージョン誤り、スキーマ不整合、削除時 UI フィードバック欠如、テストカバレッジ不足、HikariCP 導入、リスナー管理移動、モジュール名修正）をインクリメンタルに修正する。
まずビルド基盤を整え、データ層・ビジネス層・UI 層の順に修正し、最後に統合テストで検証する。

---

## Tasks

- [x] 1. pom.xml とモジュール定義の修正
  - [x] 1.1 `pom.xml` の JUnit Jupiter バージョンを `5.11.4` に変更し、HikariCP 依存 (`com.zaxxer:HikariCP:6.3.0`) を `compile` スコープで追加する
    - `junit-jupiter` の `<version>6.0.3</version>` を `<version>5.11.4</version>` へ書き換える
    - `<dependency>` ブロックに `com.zaxxer:HikariCP:6.3.0` (compile scope) を追加する
    - _Requirements: 3.1, 3.3, 7.1_
  - [x] 1.2 `module-info.java` のモジュール名を `jp.itigotti` に変更し、HikariCP モジュールを requires に追加する
    - `module jp.itigotti.module` を `module jp.itigotti` に書き換える
    - `requires com.zaxxer.hikari;` を追加する
    - _Requirements: 9.1_
  - [x] 1.3 `pom.xml` の javafx-maven-plugin `<mainClass>` および jpackage-maven-plugin `<module>` を `jp.itigotti/jp.itigotti.Main` に更新する
    - _Requirements: 9.2, 9.3_

- [x] 2. TodoDAO — HikariCP 移行・ResultSet 修正・スキーマ修正
  - [x] 2.1 `TodoDAO` に `HikariDataSource` フィールドを追加し、コンストラクタ `()` と `(String customDbUrl)` でそれぞれプールを初期化する。`DriverManager.getConnection()` をすべて `dataSource.getConnection()` に置き換え、`AutoCloseable` を実装する
    - `HikariConfig` を生成して `jdbcUrl`・`driverClassName` を設定し `HikariDataSource` を生成する
    - 全 CRUD メソッドの try-with-resources の接続取得を `dataSource.getConnection()` に変更する
    - `close()` メソッドを追加して `dataSource.close()` を呼ぶ
    - _Requirements: 7.2, 7.3, 7.4, 7.5_
  - [x] 2.2 `initializeDB()` のスキーマを `expiration_date NOT NULL` 制約付きに変更し、既存 NULL 行を当日付で補完するマイグレーション処理を追加する
    - `CREATE TABLE IF NOT EXISTS` の `expiration_date` カラムを `DATE NOT NULL` に変更する
    - テーブル作成後、`expiration_date IS NULL` の行を検出して `LocalDate.now()` で UPDATE するマイグレーションを追加する
    - _Requirements: 4.1, 4.2, 4.3_
  - [x] 2.3 `create()` で `expiration_date` が null の場合に `IllegalArgumentException` をスローするガードを追加し、`findAll()` および `create()` の ResultSet を try-with-resources でクローズする
    - `item.getExpirationDate() == null` の場合に `throw new IllegalArgumentException(...)` を追加する
    - `findAll()` の `ResultSet rs = pStmt.executeQuery()` を try-with-resources に変換する
    - `create()` の `ResultSet rs = pStmt.getGeneratedKeys()` を try-with-resources に変換する
    - _Requirements: 1.1, 1.2, 1.3, 4.4_
  - [x]* 2.4 `TodoDAOTest` に `update()` の正常系・`delete()` の正常系・`create()` に null 期限日を渡した異常系の単体テストを追加する
    - `testUpdate_既存レコードを上書きできる()`: create 後に task・expirationDate・completed を変更して update し、findAll で値が更新されていることを検証する
    - `testDelete_指定IDが削除される()`: create 後に delete し、findAll の結果に含まれないことを検証する
    - `testCreate_expirationDateがnullの場合IllegalArgumentException()`: null 期限日で create し例外がスローされることを検証する
    - _Requirements: 6.3, 6.4, 6.5_
  - [ ]* 2.5 Property 1（NULL 期限日マイグレーション後の完全性）のプロパティテストを `TodoDAOTest` に追加する
    - **Property 1: NULL 期限日マイグレーション後の完全性**
    - **Validates: Requirements 4.2**
    - jqwik (`net.jqwik:jqwik`) または単純なパラメータ化テストで、0〜N 件の NULL 行を持つ既存テーブルに `initializeDB()` を呼び出した後、`findAll()` が返す全アイテムの `expirationDate` が非 null であることを検証する
  - [ ]* 2.6 Property 2（null 期限日での create() は必ず拒否）のプロパティテストを `TodoDAOTest` に追加する
    - **Property 2: null 期限日での create() は必ず拒否**
    - **Validates: Requirements 4.4**
    - 任意の有効なタスク文字列と null 期限日の組み合わせで `create()` が `IllegalArgumentException` をスローすることを検証する
  - [ ]* 2.7 Property 4（update ラウンドトリップ）のプロパティテストを `TodoDAOTest` に追加する
    - **Property 4: update ラウンドトリップ**
    - **Validates: Requirements 6.3**
    - DB に登録済みのアイテムに対して任意のフィールド値を変更して `update()` を呼び出した後、`findAll()` で同一 ID のアイテムが更新後の値を持つことを検証する
  - [ ]* 2.8 Property 5（delete 後の不在保証）のプロパティテストを `TodoDAOTest` に追加する
    - **Property 5: delete 後の不在保証**
    - **Validates: Requirements 6.4**
    - DB に登録済みのアイテムのセットから任意の 1 件を `delete()` した後、`findAll()` の結果にその ID が含まれないことを検証する

- [ ] 3. チェックポイント — データ層テストの確認
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. TodoItemModel — hasCompletedListener の除去
  - [x] 4.1 `TodoItemModel` から `hasCompletedListener` フィールド、`isListenerInstalled()` メソッド、`setListenerInstalled()` メソッドを削除する
    - フィールド宣言 `private boolean hasCompletedListener = false;` を削除する
    - `isListenerInstalled()` と `setListenerInstalled()` メソッドを削除する
    - _Requirements: 8.1_

- [x] 5. TodoListLogic — ListenerRegistry の導入
  - [x] 5.1 `TodoListLogic` に `Set<Integer>` 型の `listenerRegistry` フィールドを追加し、`setupItemListener()` で `listenerRegistry.contains(item.getId())` による重複チェックを実装する
    - `private final Set<Integer> listenerRegistry = new HashSet<>();` を追加する
    - `setupItemListener()` 内の `item.isListenerInstalled()` の参照を `listenerRegistry.contains(item.getId())` に置き換え、登録後は `listenerRegistry.add(item.getId())` を呼ぶ
    - `item.setListenerInstalled(true)` の呼び出しを削除する
    - _Requirements: 8.2, 8.3, 8.4_
  - [ ]* 5.2 `TodoListLogicTest` に addTodoItem() の異常系テスト（null タスク・空文字・null 期限日）を追加する
    - `addTodoItem_nullタスクでIllegalArgumentException()`: null を渡したとき例外がスローされリストサイズが 0 のままであることを検証する
    - `addTodoItem_空文字タスクでIllegalArgumentException()`: 空文字を渡したとき例外がスローされることを検証する
    - `addTodoItem_null期限日でIllegalArgumentException()`: null 期限日を渡したとき例外がスローされることを検証する
    - _Requirements: 6.2_
  - [ ]* 5.3 Property 3（addTodoItem() のバリデーション）のプロパティテストを `TodoListLogicTest` に追加する
    - **Property 3: addTodoItem() のバリデーション**
    - **Validates: Requirements 6.2**
    - null・空文字・空白のみ・null 期限日の組み合わせで `addTodoItem()` を呼び出したとき `IllegalArgumentException` がスローされ、リストサイズが変化しないことを検証する
  - [ ]* 5.4 Property 6（リスナー重複登録防止）のプロパティテストを `TodoListLogicTest` に追加する
    - **Property 6: リスナー重複登録防止**
    - **Validates: Requirements 8.2, 8.3, 8.4**
    - `refresh()` を N 回呼び出した後、同一アイテムの completed を 1 回トグルしたとき `TodoDAO.update()` がちょうど 1 回呼ばれることを Mockito でスパイして検証する

- [x] 6. Controller — 削除未選択時の警告ダイアログ追加
  - [x] 6.1 `Controller.handleDeleteAction()` に選択アイテムが null の場合 `AlertType.WARNING` ダイアログを表示するコードを追加する
    - `if (selected != null)` の `else` ブランチを追加し、`Alert` を生成してタイトル `"削除エラー"`、ヘッダ null、コンテンツテキストにタスク選択を促すメッセージを設定する
    - _Requirements: 5.1, 5.2, 5.3_
  - [ ]* 6.2 `ControllerTest` を新規作成し、`handleDeleteAction()` で選択なし時に WARNING ダイアログが表示されることを TestFX で検証するテストを追加する
    - TestFX でアプリを起動し、何も選択せずに削除ボタンをクリックしたとき `AlertType.WARNING` のダイアログが表示されることを検証する
    - _Requirements: 6.1_

- [x] 7. MainTest の本番 DB 分離
  - [x] 7.1 `MainTest` を修正し、`@TempDir` で生成した一時ディレクトリに TestDAO を生成して `Main.start(Stage)` にコンストラクタインジェクションで渡すよう変更する
    - `Main.init()` の直接呼び出しを除去する
    - `TodoDAO dao = new TodoDAO(tempDbUrl)` を生成し `Main` コンストラクタまたは setter で渡す形に変更する（`Main` クラスへの対応する変更も含む）
    - _Requirements: 2.1, 2.2, 2.3_

- [ ] 8. 最終チェックポイント — 全テストの確認
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- `*` 付きのサブタスクはオプションであり、MVP を優先する場合はスキップ可能
- Property テストの追加には jqwik または JUnit 5 の `@ParameterizedTest` を使用する（jqwik を使う場合は pom.xml への依存追加が必要）
- 各タスクは前のタスクの成果物に依存しているため、順序通りに実行すること
- タスク 1 完了後に `mvn compile` でビルドが通ることを確認してから次へ進む
- `TodoItemModel` の変更（タスク 4.1）は `TodoListLogic` の変更（タスク 5.1）と密結合しているため、両方を同一 PR または同一コミットで対応することを推奨する

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3", "2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3"] },
    { "id": 3, "tasks": ["2.4", "2.5", "2.6", "2.7", "2.8", "4.1"] },
    { "id": 4, "tasks": ["5.1"] },
    { "id": 5, "tasks": ["5.2", "5.3", "5.4", "6.1"] },
    { "id": 6, "tasks": ["6.2", "7.1"] }
  ]
}
```
