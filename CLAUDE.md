# CLAUDE.md

Java 25 / Spring Boot 4 / Gradle（Kotlin DSL）のテンプレート。利用者向けの手順は `README.md` を参照する。

## ルール

- ブランチ戦略は GitHub Flow に従う。`main` から作業ブランチを切り、Pull Request 経由で `main` へマージする。
- コミットメッセージは [Conventional Commits](https://www.conventionalcommits.org/ja/v1.0.0/) に従う。
- 応答の終了時に Stop hook（`.claude/hooks/run-check.sh`）が未検証の変更へ `check` を実行する。失敗を返されたら修正する。
- 検査を通すために閾値の引き下げやルールの無効化をしない。誤検知の抑止は対象を最小限にし、理由をコメントで残す。

## 実行環境

ホストに Java はない。Gradle はホストで直接実行せず、Dev Container 上で実行する。

```sh
npx -y @devcontainers/cli up --workspace-folder .
npx -y @devcontainers/cli exec --workspace-folder . ./gradlew <タスク>
```

- `.devcontainer/` を変更したら、`up` に `--remove-existing-container` を付けて作り直す。既存のコンテナでは変更が反映されないことがある。
- ホスト上で Dev Container を検証するときは、ワークスペースをホームディレクトリ配下に置く。`/private/tmp` では起動に失敗した実績がある。

## 主なタスク

| タスク | 用途 |
|---|---|
| `check` | テストと全静的解析（javac lint、typos、PMD、Spotless、JaCoCo） |
| `test` | テストのみ |
| `spotlessApply` | 書式の自動修正 |
| `bootRun` | アプリの起動（8080 番） |

`typos` はコンテナ内にのみ導入されるため、`check` もコンテナ内でのみ成功する。

## このファイルの運用

毎回の作業に必要な情報だけを書き、手順の詳細は `README.md` に置く。特定のファイルにのみ関わるルールは `.claude/rules/` へ、ときどき行う手順はスキルへ分離する。
