# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## このプロジェクトの目的と開発方針（重要）

このリポジトリは、動くアプリを完成させることに加えて、**開発者（ユーザー）自身がプログラミングを学ぶための教材**でもある。Claude Code はこの前提を踏まえて振る舞うこと。

- タスクを黙って最後まで実装して渡すのではなく、**なぜそう書くのか**（設計判断・トレードオフ・関連する Java/JavaFX/JDBC/SQL の概念）を説明しながら進める。
- 学習効果のあるまとまった実装（新機能のロジック、バグの根本原因に関わる修正など）は、全部を代わりに書き切らず、ユーザー自身が書く部分を意図的に残す。残す箇所には `TODO(human)` を付け、何を・なぜ実装する必要があるか、参考になる既存コードや考え方のヒントを添える。
- 単純な定型作業（フォーマット修正、明らかなタイポ、依存バージョンの更新など）は学習効果が薄いのでこの限りではない。都度、これは説明重視でいくべきか／サッと片付けてよいかを判断する。
- バグ修正では、パッチを当てるだけでなく「なぜそのバグが起きたか」を説明する。
- 新しい概念（例: HikariCP のコネクションプール、JavaFX のプロパティバインディング、JPMS の `module-info.java` など）を扱うときは、このコードベースでどう使われているかに触れつつ簡潔に解説する。
- 大きなリファクタより、説明とセットの小さく段階的な変更を優先する。

## プロジェクト概要

JavaFX 製のデスクトップ TODO アプリ（パッケージ `jp.itigotti`）。データは SQLite に永続化する。ソースはリポジトリ直下ではなく `todo-app/`（Maven モジュールルート）配下にある。

## コマンド

以下はすべて `pom.xml` のある `todo-app/` ディレクトリで実行する。

```bash
cd todo-app
```

- 全テスト実行: `xvfb-run -a mvn test`（Linux/ヘッドレス環境向け — TestFX の UI テストにはディスプレイが必要なため `xvfb-run` で仮想ディスプレイを用意する）
- 単一テストクラスの実行: `xvfb-run -a mvn test -Dtest=TodoDAOTest`
- 単一テストメソッドの実行: `xvfb-run -a mvn test -Dtest=TodoDAOTest#testCreate`
- ポータブル版（APP_IMAGE）のビルド: `xvfb-run -a mvn clean package -Dapp.type=portable` → 成果物は `target/dist/`
- インストーラー版（Linux では `.deb`）のビルド: `xvfb-run -a mvn clean package -Dapp.type=installer`
- ビルドしたポータブル版の実行: `target/dist/TodoApp/bin/todo-app`

Windows/macOS、またはディスプレイのある実行環境では `xvfb-run -a` は不要。

Maven の `app.type` プロパティ（`portable`/`installer`）は `pom.xml` 内の OS ごとのプロファイル（`linux-portable`、`windows-installer` など）を選択し、`jpackage.type`（`APP_IMAGE`/`DEB`/`MSI`/`PKG`）と `app.mode` を設定する。`app.mode` はアプリ起動時に `-Dtodo.app.mode` としてアプリ側に渡される。

## Git ワークフロー

このリポジトリでの変更は、GitHub issue 単位で次の流れで進める。

1. **issue化**: 対応する issue がまだなければ先に作る（レビューで見つかった問題点なども、着手前にissueとして切り出す）。
2. **ブランチ作成**: `master` から作業ブランチを切ってから着手する。`master` に直接コミットしない。ブランチ名は種類のプレフィックス + 内容の短い説明（例: `fix/initializeDB-log-dburl`、`feature/add-xxx`、`test/add-xxx`）。既存ブランチの命名（`fix/`、`feature/`、`patch/`、`test/` など）に合わせる。
3. **作業・コミット**: 変更後は関連テストを実行して確認してからコミットする。
4. **PR作成**: 作業ブランチを push し、`master` 向けに PR を作成する。PR本文に `Fixes #<issue番号>` を含め、マージ時に issue が自動クローズされるようにする。

`gh` CLI（`gh issue create`、`gh pr create`）を使う。

## アーキテクチャ

MVC パターン、単一パッケージ `jp.itigotti`、DI フレームワークを使わず手動で結線している：

- **`Main`** — JavaFX の `Application` エントリポイント。`init()` 内で、`setDao()` で DAO が事前に注入されていない限り（テストで使用）`TodoDAO` を生成し `initializeDB()` を呼ぶ。`start()` では `scene.fxml` を、DAO 付きで `Controller` を生成するカスタム `controllerFactory` とともにロードする（FXML のデフォルトの引数なしコンストラクタ生成をあえて回避し、DAO を注入するため）。`stop()` で DAO をクローズする。
- **`Controller`** — FXML コントローラー（`@FXML` フィールドは `scene.fxml` の `fx:id` にバインドされる）。コンストラクタで `TodoListLogic` インスタンスを保持する。UI イベント（`handleAddAction`、`handleDeleteAction`）とテーブルセルの描画・整形（日付フォーマットは `yyyy/MM/dd`、完了状態はチェックボックスセル）を担当する。期限日の入力は柔軟に受け付ける：`DatePicker` で選択した値、またはフリー入力されたテキストを `ISO_LOCAL_DATE` → `yyyy/MM/dd` の順にパース（`resolveExpirationDate` 参照）。不正な入力は呼び出し元に例外を投げるのではなく `Alert` で表示する。
- **`TodoListLogic`** — `Controller` と `TodoDAO` の間に位置するビジネスロジック。テーブルビューが直接バインドする `ObservableList<TodoItemModel>` を保持する。`addTodoItem`/`refresh` の際に各アイテムの `completedProperty` にリスナーを登録し（`WeakHashMap` で管理し二重登録を防止）、完了状態の変更を即座に `TodoDAO.update` へ反映する。つまりテーブルのチェックボックスを切り替えると、明示的な保存操作なしにプロパティリスナー経由で即座に DB へ書き込まれる。
- **`TodoItemModel`** — TableView の各カラムが直接バインドする JavaFX Bean（`SimpleIntegerProperty`/`SimpleStringProperty` など）。`equals`/`hashCode` は DB の `id` が採番された後はそれを使う（`id == 0` は未保存を意味し、その場合は同一性比較にフォールバックする）。`create()` で id が採番される前の未保存アイテムが observable list に存在し得るため、この挙動が意味を持つ。
- **`TodoDAO`** — 全 SQL を素の JDBC で実行（ORM なし）。HikariCP のコネクションプールを使用し、`maximumPoolSize=1` に固定（SQLite は単一ライターのため）。DB のパス・URL は `todo.app.mode` に基づき static イニシャライザで一度だけ決定される：インストーラーモードでは `~/.todo-app/todo.db`、それ以外は実行ディレクトリ直下の `./todo.db`。JDBC URL を明示的に受け取る第二コンストラクタもあり、全テストが実 DB ではなく一時ファイルを指すのに使っている。`initializeDB()` はテーブル作成に加え、`expiration_date` が NULL の既存行を当日付に補完するマイグレーションも行う。
- **`StringUtil`** — `truncateForLog` のみ。ユーザー入力のタスク文字列をログ出力する際、ログが肥大化しないよう切り詰めるのに使う。

### 典型的な操作のデータフロー
UI イベント（FXML の `onAction`）→ `Controller` が入力を検証・パース → `TodoListLogic`（検証し `TodoDAO` を呼び、`ObservableList` を変更）→ JavaFX のプロパティバインディングにより TableView が自動更新。

### モジュールシステム
このアプリはモジュール化されている（`src/main/java/module-info.java`）。`main` から使う新しい依存ライブラリを追加する場合は対応する `requires` を、FXML のリフレクションアクセスが必要なパッケージには `javafx.fxml` への `opens` を追加する必要がある。

### テストの規約
- テスト名は日本語で、挙動を表す `メソッド名_条件()` 形式になっている — 新規テストもこの規約に従うこと。
- DAO/ロジック/UI のテストはすべて、`TodoDAO(String customDbUrl)` コンストラクタを使い `@TempDir` 配下の SQLite ファイルに対して独自の `TodoDAO` を生成している — テストを実際の `todo.db` に向けてはならない。
- `ControllerTest` と `MainTest` は TestFX の `ApplicationTest`/`ApplicationExtension` を使い、コントローラーのメソッドを直接呼ぶのではなく `FxRobot`（`clickOn`、`write`、`#fxid` によるルックアップ）経由で UI を操作する。
- ヘッドレスでのテスト/CI 実行には Linux 上で `xvfb-run` が必要（ディスプレイがないため） — 詳細は上記コマンドの項を参照。

### ロギング
SLF4J + Logback を使用し、`src/main/resources/logback.xml` で設定する。ログ出力先はデフォルトで `./logs/`。インストーラーモードでは `Main.init()` で設定される `todo.log.dir` システムプロパティにより `~/.todo-app/logs` にリダイレクトされる。ログは30日分ローテーションして保持される。

### CI
`.github/workflows/maven.yml` は `master` への push/PR をトリガーに `{linux, windows, macos} x {portable, installer}` の全6通りをビルドし、それぞれの `target/dist/` を artifact としてアップロードする。
