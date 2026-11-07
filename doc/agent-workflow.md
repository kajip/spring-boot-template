# Agent による開発フロー

Agent はすべての変更を Pull Request にし、自動マージを指定する。無人でマージするか人がレビューするかは、Agent の申告ではなく変更されたパスで決まる。

## マージの振り分け

`.github/CODEOWNERS` で全パスを人のレビュー対象とし、以下だけを無人の対象として外す。新しいパスは既定で人のレビュー対象になる。

| 変更箇所 | マージ条件 |
|---|---|
| `doc/`、`README.md`、`src/test/` のみ | CI の成功で自動マージ |
| それ以外を 1 つでも含む | CI の成功に加えて人の承認が必要 |

`src/main/` は当初は人のレビュー対象とする。ArchUnit も導入して実績を確かめてから、無人の範囲を広げる。

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

## 初回に人が行う設定

1. GitHub にリポジトリを作成し、`main` を push する。`main` への直接の push は `.claude/settings.json` で Claude に禁じているため、人が実行する。
2. Agent 専用の GitHub App を作り、対象のリポジトリにのみインストールする。作者は自分の Pull Request を承認できないため、人のアカウントと分ける。権限は Contents と Pull requests の Read and write、Checks と Commit statuses の Read-only のみとし、Administration と Workflows は与えない。
3. リポジトリの設定で `Allow auto-merge` を有効にする。
4. `main` に次の保護を設定する。
   - Pull Request を必須にする（承認数は 0、Code Owner のレビューを必須）
   - ステータスチェック `check` の成功を必須にする
   - 強制 push と削除を禁止する
   - 迂回は管理者の Pull Request 経由のみ許可する（直接の push は管理者でも禁止）

唯一の Code Owner は自分の Pull Request を承認できないため、人の変更をマージできるよう管理者の迂回を残す。Agent に管理者権限があると同じ迂回ができてしまう。Agent 専用のアカウントに切り替えるまで、人のレビューの関門は Agent に効かない。

非公開リポジトリでは、契約プランによりブランチ保護を使えない場合がある。

## テンプレートから作るとき

`.github/CODEOWNERS` の所有者を、そのプロジェクトのレビュー担当者に置き換える。
