# CLAUDE.md

Java 25 / Spring Boot 4 / Gradle（Kotlin DSL）/ PostgreSQL のテンプレート。利用者向けの手順は `README.md` を参照する。

## ルール

- ブランチ戦略は GitHub Flow に従う。`main` から作業ブランチを切り、Pull Request 経由で `main` へマージする。
- Pull Request を作ったら `gh pr merge --auto --squash` を指定する。無人か人のレビューかは CODEOWNERS と `main` の保護が決めるので、自分で判断や迂回をしない（詳細は `doc/agent-workflow.md`）。
- GitHub への push と Pull Request の操作は、人のアカウントではなく GitHub App の名義で、Mac 側から行う（手順は `doc/agent-workflow.md`）。`GH_TOKEN` を使うコマンドは 1 回の Bash 呼び出しにまとめる。トークンと秘密鍵を表示しない。
- コードは機能別パッケージの `web`、`domain`、`persistence` に置く（README の「パッケージ構成」）。ArchUnit が検査する。
- コミットメッセージは [Conventional Commits](https://www.conventionalcommits.org/ja/v1.0.0/) に従う。
- コミットの前に `generateDocs` と `check` を実行する。応答の終了時に Stop hook（`.claude/hooks/run-check.sh`）も、未コミットの変更か `origin/main` に無いコミットがあれば実行する。失敗や生成物の更新を返されたら対応する。
- `doc/autogen/` は生成物なので直接編集しない。編集ツールは `.claude/settings.json` で拒否され、シェルからの書き換えも禁止する。更新は `generateDocs` で行う。
- `doc/autogen/` 以外の文書と README は自動で更新されない。ディレクトリ構成、環境変数、ガード、GitHub の設定、手順を変えたら合わせて更新する。
- 検査を通すために閾値の引き下げやルールの無効化をしない。誤検知の抑止は対象を最小限にし、理由をコメントで残す。

## 実行環境

ホストに Java はない。Gradle はホストで直接実行せず、Dev Container 上で実行する。

```sh
npx -y @devcontainers/cli up --workspace-folder .
npx -y @devcontainers/cli exec --workspace-folder . ./gradlew <タスク>
```

- `.devcontainer/` を変更したら、`up` に `--remove-existing-container` を付けて作り直す。既存のコンテナでは変更が反映されないことがある。
- Dev Container は Compose で PostgreSQL（`db`）と同時に起動する。テストは Gradle の `test` タスク経由で `app_test` に繋ぎ、開発用の `app` と分ける。
- スキーマの変更は `src/main/resources/db/migration` に Flyway のマイグレーションとして追加する。適用済みのファイルは書き換えない。
- `docker compose down -v` は共有の Gradle キャッシュも削除対象になるため使わない。
- ホスト上で Dev Container を検証するときは、ワークスペースをホームディレクトリ配下に置く。`/private/tmp` では起動に失敗した実績がある。

## 主なタスク

| タスク | 用途 |
|---|---|
| `check` | テスト、全静的解析（javac lint、typos、PMD、Spotless、JaCoCo、PIT、ArchUnit）、生成物の鮮度確認 |
| `generateDocs` | クラス図、ER 図、OpenAPI の再生成 |
| `test` | テストのみ |
| `spotlessApply` | 書式の自動修正 |
| `bootRun` | アプリの起動（8080 番） |

`check` は `typos`、`tbls`、`db` への接続を使うため、コンテナ内でのみ成功する。`Dockerfile` や起動に関わる設定を変えたら、ホストで `scripts/smoke-test.sh` も実行する。

## このファイルの運用

毎回の作業に必要な情報だけを書き、手順の詳細は `README.md` に置く。特定のファイルにのみ関わるルールは `.claude/rules/` へ、ときどき行う手順はスキルへ分離する。
