# 環境変数とパラメータ

## アプリケーション

本番を含むすべての環境で必須。`application.properties` に既定値を持たないため、接続先 URL を指定しないと起動に失敗する。

| 環境変数 | 説明 | 例 |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL の JDBC URL | `jdbc:postgresql://<ホスト>:5432/<データベース>` |
| `SPRING_DATASOURCE_USERNAME` | 接続ユーザー | - |
| `SPRING_DATASOURCE_PASSWORD` | 接続パスワード | - |

そのほかの Spring Boot のプロパティも、同じ規則の環境変数で上書きできる。

## 開発環境

開発コンテナと PostgreSQL に `.devcontainer/compose.yaml` で設定する値。いずれもローカル開発専用で、本番では使わない。

| サービス | 環境変数 | 値 | 説明 |
|---|---|---|---|
| `app` | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db:5432/app` | 開発用データベースの接続先 |
| `app` | `SPRING_DATASOURCE_USERNAME` | `app` | 接続ユーザー |
| `app` | `SPRING_DATASOURCE_PASSWORD` | `app` | 接続パスワード |
| `db` | `POSTGRES_USER` | `app` | PostgreSQL の管理ユーザー |
| `db` | `POSTGRES_PASSWORD` | `app` | 管理ユーザーのパスワード |
| `db` | `POSTGRES_DB` | `app` | 初回起動時に作るデータベース |

`POSTGRES_*` は DB の volume を初めて作るときにのみ反映される。変更後は [README](../README.md#データベース) の手順で初期化する。

## テスト

| 設定場所 | パラメータ | 値 | 説明 |
|---|---|---|---|
| `build.gradle.kts` の `test` タスク | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db:5432/app_test` | テストの接続先。ユーザーとパスワードは開発環境の値を使う |
| `.tbls.yml` | `dsn` | `postgres://app:app@db:5432/app_test?sslmode=disable` | ER 図の生成元 |

## イメージのビルド

`.devcontainer/Dockerfile` のビルド引数。変更するときは、同じファイルの SHA-256 も合わせて更新する。

| ビルド引数 | 既定値 | 説明 |
|---|---|---|
| `TYPOS_VERSION` | `1.52.0` | typos のバージョン |
| `TBLS_VERSION` | `1.96.1` | tbls のバージョン |

## Claude Code

| 環境変数 | 説明 |
|---|---|
| `CLAUDE_PROJECT_DIR` | プロジェクトのルート。Claude Code が設定し、Stop hook が参照する |
| `AGENT_APP_KEY` | GitHub App の秘密鍵のパス。既定値は `~/.config/claude-code-coding-agent/key.pem` |
| `GH_TOKEN` | `scripts/agent-github-token.sh` で発行した App のトークン。`gh` と `git push` が参照する |
