# Todo App

JavaFXで作られたシンプルなデスクトップ TODO 管理アプリケーションです。タスクと期限を登録・管理でき、データは SQLite に永続化されます。

## 機能

- タスクの追加（タスク名と期限を指定）
- タスクの完了状態をチェックボックスで管理
- タスクを選択して削除
- 期限の昇順でタスクを表示
- アプリ再起動後もデータを保持（SQLite）

## 動作環境

| OS      | ポータブル版 | インストーラー形式 |
|---------|------------|----------------|
| Linux   | APP_IMAGE  | DEB            |
| Windows | APP_IMAGE  | MSI            |
| macOS   | APP_IMAGE  | PKG            |

## ビルド

### 前提条件

- JDK 25 以上
- Maven 3.x

### ポータブル版（APP_IMAGE）

```bash
cd todo-app
mvn clean package -Dapp.type=portable
```

ビルド成果物は `todo-app/target/dist/` に出力されます。

### インストーラー版

```bash
cd todo-app
mvn clean package -Dapp.type=installer
```

- Linux: `.deb` パッケージ
- Windows: `.msi` インストーラー（Windows 環境でのみビルド可能）
- macOS: `.pkg` インストーラー（macOS 環境でのみビルド可能）

> **Linux でのテスト実行について**  
> ヘッドレス環境では `xvfb-run -a mvn clean package` を使用してください。テストのみ実行する場合は `xvfb-run -a mvn test` を使用してください。

## 実行方法

### ポータブル版

```bash
todo-app/target/dist/TodoApp/bin/todo-app
```

### インストーラー版

インストール後、OS のアプリケーションメニューから **TodoApp** を起動してください。

## データ・ログの保存場所

| 種別 | ポータブル版 | インストーラー版 |
|------|------------|----------------|
| DB   | `./todo.db`（実行ディレクトリ直下） | `~/.todo-app/todo.db` |
| ログ | `./logs/todo-app.log` | `~/.todo-app/logs/todo-app.log` |

ログは 30 日分ローテーションして保存されます。

## アーキテクチャ

MVC パターンで実装されています。

```
jp.itigotti
├── Main.java           # アプリケーションエントリポイント、DB 初期化
├── Controller.java     # UI イベントハンドラ（JavaFX FXML コントローラー）
├── TodoListLogic.java  # ビジネスロジック（追加・削除・更新）
├── TodoItemModel.java  # データモデル（JavaFX プロパティ）
├── TodoDAO.java        # SQLite アクセス（CRUD）、HikariCP接続プール管理
└── StringUtil.java     # ユーティリティ
```

## 依存ライブラリ

| ライブラリ | バージョン | 用途 |
|-----------|----------|------|
| JavaFX    | 25       | UI フレームワーク |
| sqlite-jdbc | 3.53.2.1 | SQLite ドライバ |
| SLF4J     | 2.0.18   | ロギング API |
| Logback   | 1.6.1    | ロギング実装 |
| JUnit Jupiter | 5.11.4 | テスト |
| HikariCP  | 6.3.0    | DB接続プール |
| Mockito   | 5.21.0   | モック |
| TestFX    | 4.0.18   | JavaFX UI テスト |
| AssertJ   | 3.25.3   | アサーション |

## CI/CD

GitHub Actions でマルチプラットフォームビルドを自動実行しています（`.github/workflows/maven.yml`）。

`master` ブランチへの push・PR をトリガーに、Linux / Windows / macOS それぞれのポータブル版とインストーラー版（計 6 種）をビルドし、Artifact としてアップロードします。
