# spring-boot-template

Spring Boot アプリ開発用テンプレート。ビルドとテストは Dev Container 上で実行するため、ローカルに Java は不要。

- Java 25 / Spring Boot 4.1.2 / Gradle Wrapper（Kotlin DSL）
- `GET /` が `Hello, World!` を返すサンプル付き

## 必要なもの

- Docker（colima 等）
- VS Code と Dev Containers 拡張、または [Dev Container CLI](https://github.com/devcontainers/cli)

## 使い方

### VS Code

コマンドパレットから `Dev Containers: Reopen in Container` を実行し、コンテナ内のターミナルで以下を実行する。

```sh
./gradlew test      # テスト
./gradlew build     # ビルド
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

```sh
./gradlew spotlessApply   # 書式を自動修正
```

カバレッジはプロジェクト全体で判定し、`*Application` クラスは対象外とする。`typos` はコンテナ内にのみ導入されるため、`check` はコンテナ内で実行する。

### レポートの確認

`build/` はホストと共有されるため、`./gradlew check` の実行後に macOS 側のターミナルからプロジェクト直下で開ける。コンテナ内では `open` を使えない。

```sh
open build/reports/tests/test/index.html        # テスト結果
open build/reports/jacoco/test/html/index.html  # カバレッジ
open build/reports/pmd/main.html                # PMD
```

コンテナ内から確認する場合は、プロジェクト直下（`/workspaces/<プロジェクト名>`）で JDK 付属の `jwebserver` を起動する。`-d` には絶対パスが必要なため `$PWD` を使っており、プロジェクト名の読み替えは不要。

```sh
jwebserver -d "$PWD/build/reports" -p 8000
```

VS Code がポートを転送すると、ブラウザで以下の URL を開ける。

- テスト結果: `http://localhost:8000/tests/test/`
- カバレッジ: `http://localhost:8000/jacoco/test/html/`
- PMD: `http://localhost:8000/pmd/main.html`

## Docker イメージ

ホスト側のターミナルからプロジェクト直下で実行する。ビルドもコンテナ内で行うため、ホストに Java は不要。

```sh
docker build -t spring-boot-template .
docker run --rm -p 8080:8080 spring-boot-template
```

イメージのビルドではテストと静的解析を実行しないため、事前に `./gradlew check` を通しておく。

## テンプレートから新規プロジェクトを作るとき

以下を新しいプロジェクト名に置き換える。

- `settings.gradle.kts` の `rootProject.name`
- `build.gradle.kts` の `group`
- `src/main/resources/application.properties` の `spring.application.name`
- `src/main/java` と `src/test/java` のパッケージ名および `SpringBootTemplateApplication` のクラス名
- `.devcontainer/devcontainer.json` の `name`

## Gradle キャッシュの共有

`~/.gradle` は volume `spring-boot-gradle-cache` に置かれ、このテンプレートから作った全プロジェクトで共有される。依存の再取得を省ける一方、複数のコンテナで同時にビルドするとキャッシュのロック待ちで失敗する可能性がある。その場合は同時実行を避けるか、`devcontainer.json` の volume 名をプロジェクト固有に変更する。
