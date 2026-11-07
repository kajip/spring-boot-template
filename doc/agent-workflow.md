# Agent による開発フロー

ここでの Agent は Claude Code を指す。Agent はすべての変更を Pull Request にし、自動マージを指定する。無人でマージするか人がレビューするかは、Agent の申告ではなく変更されたパスで決まる。

## マージの振り分け

`.github/CODEOWNERS` で全パスを人のレビュー対象とし、以下だけを無人の対象として外す。新しいパスは既定で人のレビュー対象になる。

| 変更箇所 | マージ条件 |
|---|---|
| `doc/`、`README.md`、`src/test/` のみ | CI の成功で自動マージ |
| それ以外を 1 つでも含む | CI の成功に加えて人の承認が必要 |
| `src/test/.../ArchitectureTests.java`、`doc/agent-workflow.md` | 無人の範囲内だが、例外として人の承認が必要 |
| `.github/workflows/` | App では push できない。人が push する |

`src/main/` は当初は人のレビュー対象とする。PIT と ArchUnit の運用実績を確かめてから、無人の範囲を広げる。

パスでは変更の意味を判定できない。無人の範囲に残る弱点は [ガードの説明](guards.md#残る弱点) を参照する。問題が出たら revert の Pull Request で戻す。

## Agent の手順

**Mac（ホスト）のプロジェクト直下で、次の一連のコマンドを同じシェルで実行する。** Dev Container には `gh` も秘密鍵も無い。Claude Code の Bash ツールは呼び出しをまたいで環境変数を保持しないため、`GH_TOKEN` を使うコマンドは同じ呼び出しで実行する。

```sh
# 最新の main から作業ブランチを切る。<type> は Conventional Commits の種別
git fetch origin
git switch -c <type>/<topic> origin/main

# 実装の後、生成物を更新して全検査を通す（Dev Container 内で実行される）
npx -y @devcontainers/cli up --workspace-folder .
npx -y @devcontainers/cli exec --workspace-folder . ./gradlew generateDocs check

# doc/autogen/ の更新も含めてコミットする
git add -A
git -c user.name='claude-code-coding-agent[bot]' \
  -c user.email='353756006+claude-code-coding-agent[bot]@users.noreply.github.com' \
  commit

# GitHub への操作は App の名義で行う。トークンは 1 時間で失効する
export GH_TOKEN=$(scripts/agent-github-token.sh)
git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push -u origin HEAD
gh pr create --fill
gh pr merge --auto --squash
```

- **名義の確認：** 作者は `gh pr view --json author` で確かめる。`app/` で始まらなければ、人の名義で作成されている。
- **`credential.helper` の指定は省かない。** `git push` は `GH_TOKEN` を直接は読まず、指定しないと Mac に保存された人の認証情報で push し得る。
- **秘密情報：** トークンと秘密鍵は表示しない。秘密鍵はリポジトリの外（`~/.config/claude-code-coding-agent/key.pem`）に置く。

同じ環境に人の認証情報があると、Agent は人の名義でも操作できる。無人で動かすときは、人の認証情報が無い環境で実行する。

## 関連文書

- GitHub 側の設定は [GitHub の設定手順](github-setup.md) を参照する。
- ガードの全体像は [ガードの説明](guards.md) を参照する。
- 人の認証情報が無い環境は [Agent 実行コンテナの設計](agent-container-design.md) にある。未実装である。
<!-- probe -->
