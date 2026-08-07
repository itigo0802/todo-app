# Requirements Document

## Introduction

本ドキュメントは、JavaFX + SQLite で構築された Todo アプリケーション（`jp.itigotti` パッケージ）に存在する 9 件の問題点を修正するための要件を定義する。
問題はリソースリーク・テスト品質・スキーマ不整合・UI フィードバック欠如・関心の分離・Connection 管理・モジュール名の誤りにわたる。
修正後も既存の機能（タスクの追加・削除・完了状態トグル・一覧表示）は維持される。

---

## Glossary

- **TodoDAO**: SQLite データベースへの CRUD 操作を担うデータアクセスオブジェクト。`jp.itigotti.TodoDAO`。
- **TodoListLogic**: タスクリストのビジネスロジックを担うクラス。`jp.itigotti.TodoListLogic`。
- **TodoItemModel**: 1 件の Todo タスクを表す JavaFX プロパティモデル。`jp.itigotti.TodoItemModel`。
- **Controller**: JavaFX の FXML コントローラ。UI イベントを処理し TodoListLogic に委譲する。`jp.itigotti.Controller`。
- **Main**: JavaFX Application のエントリーポイント。`jp.itigotti.Main`。
- **ConnectionPool**: HikariCP が提供するデータベース接続プール。
- **ListenerRegistry**: `TodoListLogic` 内部で管理する、完了状態リスナーが登録済みの TodoItemModel ID の集合（`Set<Integer>`）。
- **TestDAO**: テスト専用の一時ファイル上の SQLite データベースに接続する TodoDAO インスタンス。
- **MainTest**: `MainTest.java` に定義された TestFX ベースの UI 統合テストクラス。
- **ModuleName**: `module-info.java` に記述する Java モジュール識別子。

---

## Requirements

### Requirement 1: ResultSet リークの修正

**User Story:** As a 開発者, I want `TodoDAO` の `create()` および `findAll()` メソッドで ResultSet が確実にクローズされること, so that データベース接続リソースが枯渇しない。

#### Acceptance Criteria

1. WHEN `TodoDAO.create()` が呼び出され INSERT が成功したとき, THE TodoDAO SHALL 生成キーを取得する ResultSet を try-with-resources ブロック内でクローズする。
2. WHEN `TodoDAO.findAll()` が呼び出されたとき, THE TodoDAO SHALL SELECT クエリの ResultSet を try-with-resources ブロック内でクローズする。
3. WHEN 例外が発生したとき, THE TodoDAO SHALL ResultSet・PreparedStatement・Connection のクローズを try-with-resources により保証する。

---

### Requirement 2: MainTest の本番 DB 分離

**User Story:** As a 開発者, I want `MainTest` がテスト専用の一時データベースを使用すること, so that テスト実行が本番の `todo.db` を変更しない。

#### Acceptance Criteria

1. WHEN `MainTest` がテストを実行するとき, THE MainTest SHALL `Main.init()` を直接呼び出さず、テスト専用の TestDAO を差し込む手段（コンストラクタインジェクションまたはシステムプロパティ）を使用して起動する。
2. WHEN `MainTest` のテストが終了したとき, THE MainTest SHALL テスト実行前後でプロジェクトルートの `todo.db` のレコード数を変更しない。
3. WHEN `MainTest` が実行されるとき, THE MainTest SHALL JUnit 5 の `@TempDir` または同等の一時ディレクトリ機構を使用してテスト DB ファイルを配置する。

---

### Requirement 3: JUnit バージョンの修正

**User Story:** As a 開発者, I want `pom.xml` の JUnit Jupiter 依存バージョンが正しい 5.x 系であること, so that テストがビルドおよび実行に成功する。

#### Acceptance Criteria

1. THE pom.xml SHALL `junit-jupiter` のバージョンを `5.x.x` 系の正式リリースバージョン（例: `5.11.4`）に設定する。
2. WHEN `mvn test` を実行したとき, THE Maven SHALL JUnit Jupiter のアーティファクト解決に成功し、テストが起動する。
3. THE pom.xml SHALL `6.0.3` というバージョン指定を含まない。

---

### Requirement 4: expiration_date スキーマの NOT NULL 化とマイグレーション

**User Story:** As a 開発者, I want `todo_items.expiration_date` カラムが NOT NULL 制約を持ち、既存 DB でも安全にマイグレーションされること, so that UI の必須入力とスキーマが一致し、NULL 値による不整合が起きない。

#### Acceptance Criteria

1. WHEN `TodoDAO.initializeDB()` が呼び出されたとき, THE TodoDAO SHALL `expiration_date` カラムが NOT NULL 制約を持つ `todo_items` テーブルを作成する。
2. WHEN `TodoDAO.initializeDB()` が呼び出され `todo_items` テーブルが既に存在するとき, THE TodoDAO SHALL `expiration_date` カラムに NULL 値が存在する行を検出し、それらの行の `expiration_date` を当日の日付（`LocalDate.now()`）で補完するマイグレーション処理を実行する。
3. WHEN マイグレーションが完了した後, THE TodoDAO SHALL `expiration_date` カラムへの NULL 挿入を拒否する。
4. IF `expiration_date` が null の TodoItemModel に対して `TodoDAO.create()` が呼び出されたとき, THEN THE TodoDAO SHALL `IllegalArgumentException` をスローする。

---

### Requirement 5: 削除未選択時のユーザーフィードバック

**User Story:** As a ユーザー, I want 削除ボタン押下時にタスクが選択されていない場合に警告ダイアログが表示されること, so that 操作が無視されたと誤認しない。

#### Acceptance Criteria

1. WHEN 削除ボタンが押下され TableView に選択中のアイテムが存在しないとき, THE Controller SHALL AlertType.WARNING のダイアログを表示し、タスクを選択するよう促すメッセージを含める。
2. WHEN 削除ボタンが押下され選択中のアイテムが存在するとき, THE Controller SHALL ダイアログを表示せず `TodoListLogic.removeTodoItem()` を呼び出す。
3. THE Controller SHALL 削除未選択時のダイアログに「削除エラー」のタイトルと選択を促すコンテンツテキストを設定する。

---

### Requirement 6: テストカバレッジの拡充

**User Story:** As a 開発者, I want Controller・バリデーション異常系・update/delete の単体テストが追加されること, so that 修正後のコードの品質が検証可能になる。

#### Acceptance Criteria

1. THE テストスイート SHALL `Controller.handleDeleteAction()` が選択なし時に警告 Alert を表示することを検証するテストを含む。
2. THE テストスイート SHALL `TodoListLogic.addTodoItem()` に null タスク・空文字タスク・null 期限日を渡したとき `IllegalArgumentException` がスローされることを検証するテストを含む。
3. THE テストスイート SHALL `TodoDAO.update()` が既存レコードを正しく上書きし更新後の値を返すことを検証するテストを含む。
4. THE テストスイート SHALL `TodoDAO.delete()` が指定 ID のレコードを削除し `findAll()` の結果に含まれないことを検証するテストを含む。
5. WHEN 追加テストが `TodoDAO` を使用するとき, THE テストスイート SHALL TestDAO（`@TempDir` ベースの一時 DB）を使用し本番 DB を参照しない。

---

### Requirement 7: HikariCP による Connection Pool の導入

**User Story:** As a 開発者, I want `TodoDAO` が HikariCP 接続プールを使用してデータベース接続を取得すること, so that 毎回のコネクション生成コストが削減され、接続管理が一元化される。

#### Acceptance Criteria

1. THE pom.xml SHALL HikariCP の依存（`com.zaxxer:HikariCP`）を `compile` スコープで追加する。
2. WHEN `TodoDAO` が初期化されたとき, THE TodoDAO SHALL HikariCP の `HikariDataSource` を生成し、全ての CRUD メソッドで `dataSource.getConnection()` を使用して接続を取得する。
3. THE TodoDAO SHALL `DriverManager.getConnection()` の直接呼び出しを含まない。
4. WHEN テスト用 `TodoDAO` が初期化されたとき, THE TodoDAO SHALL カスタム JDBC URL を受け取り同 URL を使用した ConnectionPool を生成する。
5. WHEN `TodoDAO` が不要になったとき, THE TodoDAO SHALL `close()` メソッドにより `HikariDataSource` を閉じることができる（`AutoCloseable` を実装する）。

---

### Requirement 8: hasCompletedListener の TodoItemModel からの除去

**User Story:** As a 開発者, I want 完了状態リスナーの登録済み管理が `TodoItemModel` ではなく `TodoListLogic` の内部で行われること, so that モデルクラスがプレゼンテーション関心事を持たなくなる。

#### Acceptance Criteria

1. THE TodoItemModel SHALL `hasCompletedListener` フィールド、`isListenerInstalled()` メソッド、および `setListenerInstalled()` メソッドを含まない。
2. THE TodoListLogic SHALL `Set<Integer>` 型の ListenerRegistry フィールドを内部に保持し、`setupItemListener()` 呼び出し時に `item.getId()` を使って重複登録を防ぐ。
3. WHEN `TodoListLogic.refresh()` が複数回呼び出されたとき, THE TodoListLogic SHALL 同一 ID の TodoItemModel に対して完了状態リスナーを 2 回以上登録しない。
4. WHEN `TodoListLogic.addTodoItem()` が呼び出されたとき, THE TodoListLogic SHALL 新規アイテムの ID を ListenerRegistry に登録し、リスナーを 1 回だけ追加する。

---

### Requirement 9: モジュール名の修正

**User Story:** As a 開発者, I want `module-info.java` のモジュール名が `jp.itigotti` に変更され、`pom.xml` の参照も同期されること, so that モジュール識別子がパッケージ階層と一致し、保守性が向上する。

#### Acceptance Criteria

1. THE module-info.java SHALL モジュール名を `jp.itigotti` と宣言する（現行の `jp.itigotti.module` から変更）。
2. THE pom.xml SHALL javafx-maven-plugin の `<mainClass>` に `jp.itigotti/jp.itigotti.Main` を指定する。
3. THE pom.xml SHALL jpackage-maven-plugin の `<module>` に `jp.itigotti/jp.itigotti.Main` を指定する。
4. WHEN `mvn package` を実行したとき, THE Maven SHALL モジュール名変更後もビルドが成功しアプリケーションイメージが生成される。
