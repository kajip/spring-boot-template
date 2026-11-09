# spring-boot-template

Spring Boot アプリ開発用テンプレート。ビルドとテストは Dev Container（開発環境を収めたコンテナ）上で実行するため、ローカルに Java は不要。

Claude Code（文書中の Agent）に Pull Request を作らせ、変更箇所に応じて無人マージと人のレビューを振り分ける構成も備える。

- Java 25 / Spring Boot 4.1.2 / Gradle Wrapper（Kotlin DSL）
- PostgreSQL 18 / Spring Data JDBC / Flyway
- `GET /` が `Hello, World!` を、`GET /greetings` が DB の内容を JSON で返すサンプル付き

## ドキュメント

読む順序の目安は次のとおり。

| 目的 | 読むもの |
|---|---|
| このリポジトリで開発する | この README |
| テンプレートから複製する | [GitHub の設定手順](doc/github-setup.md) |
| Agent を動かす | [Agent による開発フロー](doc/agent-workflow.md)、[ガードの説明](doc/guards.md) |

文書の一覧は次のとおり。

- [ディレクトリ構造](doc/directory-structure.md)
- [環境変数とパラメータ](doc/parameters.md)
- [Agent による開発フロー](doc/agent-workflow.md)
- [ガードの説明](doc/guards.md)
- [GitHub の設定手順](doc/github-setup.md)（このテンプレートから複製したときに実施する）
- [Agent 実行コンテナの設計](doc/agent-container-design.md)（未実装）
- [クラス図](doc/autogen/class-diagram.md)
- [ER 図とテーブル定義](doc/autogen/schema/README.md)
- [API 仕様](doc/autogen/openapi.json)

## 必要なもの

開発に必要なもの：

- Docker（colima 等。colima なら `colima start` で起動しておく）と BuildKit（`docker buildx version` で確認）
- VS Code と Dev Containers 拡張、または Node.js（[Dev Container CLI](https://github.com/devcontainers/cli) を `npx` で使う）

Agent を動かす、または GitHub の設定を行う場合に追加で必要なもの：

- ログイン済みの `gh`（`gh auth status` で確認）
- `jq`、`openssl`、`curl`
- Node.js（Stop hook が Dev Container CLI を使う）

プロジェクトはホームディレクトリ配下に置く。`/private/tmp` では Dev Container の起動に失敗した実績がある。

## 使い方

### VS Code

コマンドパレットから `Dev Containers: Reopen in Container` を実行し、コンテナ内のターミナルで以下を実行する。

```sh
./gradlew test      # テスト
./gradlew build     # 全検査を含むビルド（JAR だけなら bootJar）
./gradlew bootRun   # 起動（http://localhost:8080）
```

8080 番ポートはホストへ転送される。

### Dev Container CLI

`npx @devcontainers/cli` でも実行できる（要 Node.js）。

```sh
npx @devcontainers/cli up --workspace-folder .
npx @devcontainers/cli exec --workspace-folder . ./gradlew test
```

CLI はポート転送を行わないため、起動したアプリへのホストからのアクセスには VS Code を使う。

## パッケージ構成

機能ごとにパッケージを分け、その中に層を置く。ルールは ArchUnit で検査する。

```text
org.kajip.app
├── SpringBootTemplateApplication  起動クラスのみをルートに置く
└── <機能>/
    ├── web/                       Controller
    ├── domain/                    ドメインモデル
    └── persistence/               Repository
```

- `domain` は `web` と `persistence` に依存しない。
- `persistence` は `web` に依存しない。
- 機能の間で依存を循環させない。
- フィールド注入と標準出力を使わない。

`web` から `persistence` への直接の依存は許す。処理が増えた機能では、層を追加するときに `ArchitectureTests` も合わせて変更する。

## データベース

Dev Container を開くと、`.devcontainer/compose.yaml` により開発コンテナ（`app`）と PostgreSQL（`db`）が同時に起動する。

| データベース | 用途 | 接続先の指定 |
|---|---|---|
| `app` | 開発時の起動 | `compose.yaml` の `SPRING_DATASOURCE_*` |
| `app_test` | Gradle からのテストと PIT | `build.gradle.kts` の `Test` タスクと `pitest` |

- スキーマは Flyway で管理し、起動時に `src/main/resources/db/migration` を適用する。
- IDE からテストを直接実行すると、接続先の上書きが効かず `app` に繋がる。`app_test` を使うには `./gradlew test` で実行する。
- データは volume に永続化され、コンテナを作り直しても残る。

データを初期化するときは、ホストでコンテナを止めて DB の volume を削除し、Dev Container を起動し直す。`down -v` は共有の Gradle キャッシュも削除対象になるため使わない。

```sh
docker compose --project-name <プロジェクトフォルダ名>_devcontainer -f .devcontainer/compose.yaml down
docker volume rm <プロジェクトフォルダ名>_devcontainer_db-data
```

## 静的解析

コンテナ内で `./gradlew check` を実行すると、テストと以下の検査をまとめて実行し、違反があれば失敗する。

| 対象 | ツール | 設定 |
|---|---|---|
| コンパイラ警告 | `javac -Xlint:all -Werror` | `build.gradle.kts` |
| タイポ | [typos](https://github.com/crate-ci/typos) | `.devcontainer/Dockerfile` で導入 |
| Bean の定義漏れ | `SpringBootTemplateApplicationTests` | 既定の設定で起動できるかを検証 |
| コード規約 | PMD | `config/pmd/ruleset.xml` |
| 書式 | Spotless（google-java-format） | `build.gradle.kts` |
| テスト漏れ | JaCoCo（行カバレッジ 80% 以上） | `build.gradle.kts` |
| テストの検証漏れ | PIT（ミューテーションスコア 80% 以上） | `build.gradle.kts` |
| 設計ルール | ArchUnit | `src/test/java/org/kajip/app/ArchitectureTests.java` |

```sh
./gradlew spotlessApply   # 書式を自動修正
```

カバレッジとミューテーションスコアはプロジェクト全体で判定し、`*Application` クラスは対象外とする。`check` は `typos`、`tbls`、`db` への接続を使うため、コンテナ内で実行する。

### レポートの確認

`build/` はホストと共有されるため、`./gradlew check` の実行後に macOS 側のターミナルからプロジェクト直下で開ける。コンテナ内では `open` を使えない。

```sh
open build/reports/tests/test/index.html        # テスト結果
open build/reports/jacoco/test/html/index.html  # カバレッジ
open build/reports/pmd/main.html                # PMD
open build/reports/pitest/index.html            # PIT
```

コンテナ内から確認する場合は、プロジェクト直下（`/workspace`）で JDK 付属の `jwebserver` を起動する。`-d` には絶対パスが必要なため `$PWD` を使う。

```sh
jwebserver -d "$PWD/build/reports" -p 8000
```

VS Code がポートを転送すると、ブラウザで以下の URL を開ける。

- テスト結果: `http://localhost:8000/tests/test/`
- カバレッジ: `http://localhost:8000/jacoco/test/html/`
- PMD: `http://localhost:8000/pmd/main.html`
- PIT: `http://localhost:8000/pitest/`

## ドキュメントの自動生成

`doc/autogen/` は、コードと DB から生成して Git で管理する。**直接編集してはならない。** 直接編集すると `./gradlew check` が失敗する。図は Mermaid 形式の Markdown で書かれ、GitHub で描画される想定である。

| 生成物 | 内容 | 生成元 |
|---|---|---|
| `doc/autogen/class-diagram.md` | クラス図 | コンパイル済みの本番クラス（`ClassDiagramGenerator`） |
| `doc/autogen/openapi.json` | API 仕様（OpenAPI） | テスト実行時のアプリケーション（springdoc）。本番では公開しない |
| `doc/autogen/schema/` | ER 図とテーブル定義 | `app_test` のスキーマ（[tbls](https://github.com/k1LoW/tbls)、設定は `.tbls.yml`） |

コンテナ内で再生成する。図が古いと `./gradlew check` は失敗する。

```sh
./gradlew generateDocs
```

Claude Code の作業では、未コミットの変更か `origin/main` に無いコミットがあると、応答の終了時に Stop hook が `generateDocs` と `check` を自動で実行する。実行されない条件は [ガードの説明](doc/guards.md#claude-code-の設定) を参照する。手で編集したときは自動では更新されないため、コミットの前に上のコマンドを実行する。

## Docker イメージ

ホスト側のターミナルからプロジェクト直下で実行する。ビルドもコンテナ内で行うため、ホストに Java は不要。

```sh
docker build -t spring-boot-template .
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://<ホスト>:5432/<データベース> \
  -e SPRING_DATASOURCE_USERNAME=<ユーザー> \
  -e SPRING_DATASOURCE_PASSWORD=<パスワード> \
  spring-boot-template
```

`<ホスト>` には、起動するコンテナから届く PostgreSQL を指定する。`localhost` はコンテナ自身を指す。Dev Container の `db` はポートを公開していないので接続できない。起動の確認だけなら、下のスモークテストを使う。

死活監視には `GET /actuator/health` を使う。DB に接続できないと `UP` にならない。Actuator で公開するのは `health` のみとする。

ホストで `scripts/smoke-test.sh` を実行すると、イメージをビルドして PostgreSQL と起動し、応答を確認して後片付けまでを行う。CI でも実行する。

接続情報は必須で、指定しないと起動に失敗する（[環境変数の一覧](doc/parameters.md)）。イメージのビルドではテストと静的解析を実行しないため、事前に `./gradlew check` を通しておく。

## 依存関係の更新

Dependabot が Gradle、GitHub Actions、2 つの `Dockerfile` のベースイメージの更新を毎週確認し、Pull Request を作る設定である（実物の Pull Request はまだ確認していない）。`build.gradle.kts` などを変更するため、マージには人のレビューが必要となる。

次は対象外と見込まれるので、手で更新する。

- `.devcontainer/Dockerfile` の typos と tbls
- `build.gradle.kts` の PMD、PIT、JaCoCo などのツールの版
- `compose.yaml` と `scripts/smoke-test.sh` の `postgres:18`
- `.github/workflows/check.yml` の `@devcontainers/cli` の版

## テンプレートから新規プロジェクトを作るとき

[GitHub の設定手順](doc/github-setup.md) に従う。名前の置き換えの一覧も、手順の 3 節にある。

## Gradle キャッシュの共有

`~/.gradle` は volume `spring-boot-gradle-cache` に置かれ、このテンプレートから作った全プロジェクトで共有される。依存の再取得を省ける一方、複数のコンテナで同時にビルドすると、約 1 分のロック待ちの後に `Timeout waiting to lock` で失敗することがある。その場合は同時実行を避けるか、`compose.yaml` の volume の `name` をプロジェクト固有に変更する。
