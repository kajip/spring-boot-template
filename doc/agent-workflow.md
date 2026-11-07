# Agent による開発フロー

Agent はすべての変更を Pull Request にし、自動マージを指定する。無人でマージするか人がレビューするかは、Agent の申告ではなく変更されたパスで決まる。

## マージの振り分け

`.github/CODEOWNERS` で全パスを人のレビュー対象とし、以下だけを無人の対象として外す。新しいパスは既定で人のレビュー対象になる。

| 変更箇所 | マージ条件 |
|---|---|
| `doc/`、`README.md`、`src/test/` のみ | CI の成功で自動マージ |
| それ以外を 1 つでも含む | CI の成功に加えて人の承認が必要 |

`src/main/` は当初は人のレビュー対象とする。PIT と ArchUnit の運用実績を確かめてから、無人の範囲を広げる。

パスでは変更の意味を判定できない。`src/test/` の変更で検証を弱めると PIT のスコアが下がるが、スコアはプロジェクト全体の割合で判定するため、閾値の範囲内の劣化は通る。無人マージには見逃しの余地が残るので、問題が出たら revert の Pull Request で戻す。

## Agent の手順

```sh
git switch -c <type>/<topic> main

# 実装してコミットする
git -c user.name='claude-code-coding-agent[bot]' \
  -c user.email='353756006+claude-code-coding-agent[bot]@users.noreply.github.com' \
  commit

# GitHub への操作は App の名義で行う。トークンは 1 時間で失効する
export GH_TOKEN=$(scripts/agent-github-token.sh)
git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push -u origin HEAD
gh pr create --fill
gh pr merge --auto --squash
```

トークンと秘密鍵は表示しない。秘密鍵はリポジトリの外（`~/.config/claude-code-coding-agent/key.pem`）に置く。

同じ環境に人の認証情報があると、Agent は人の名義でも操作できる。無人で動かすときは、人の認証情報が無い環境で実行する。

## 関連文書

- GitHub 側の設定は [GitHub の設定手順](github-setup.md) を参照する。
- ガードの全体像は [ガードの説明](guards.md) を参照する。
- 人の認証情報が無い環境は [Agent 実行コンテナの設計](agent-container-design.md) にある。未実装である。
