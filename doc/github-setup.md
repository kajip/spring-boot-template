# GitHub の設定手順

このテンプレートから新しいリポジトリを作るときの手順。リポジトリの設定、保護ルール、GitHub App は複製されないため、リポジトリごとに設定する。

各設定の役割は [ガードの説明](guards.md) を参照する。この手順は本リポジトリで行った設定に基づくが、複製から通しで実施した検証はまだ行っていない。

## 1. リポジトリを作る

1. テンプレートのページで **Use this template** から新しいリポジトリを作る。`Include all branches` は選ばない。
2. 作ったリポジトリを clone する。

非公開リポジトリでは、契約プランによりブランチ保護を使えない場合がある。先に確認する。

## 2. GitHub App を作る

Agent 専用の App をリポジトリごとに作る。作者は自分の Pull Request を承認できないため、人のアカウントとは分ける。

[New GitHub App](https://github.com/settings/apps/new) で以下を設定する。

| 項目 | 値 |
|---|---|
| Homepage URL | リポジトリの URL |
| Webhook の Active | 外す |
| Where can this GitHub App be installed? | Only on this account |
| Contents | Read and write |
| Pull requests | Read and write |
| Checks | Read-only |
| Commit statuses | Read-only |
| Metadata | Read-only |

**Administration と Workflows は与えない。**

作成後に以下を行う。

1. **Generate a private key** で秘密鍵を生成し、リポジトリの外に置く。秘密鍵をチャットやリポジトリに置かない。

   ```sh
   mkdir -p ~/.config/<App 名>
   mv ~/Downloads/<ダウンロードした鍵>.pem ~/.config/<App 名>/key.pem
   chmod 600 ~/.config/<App 名>/key.pem
   ```

2. **Install App** から **Only select repositories** を選び、このリポジトリのみを指定する。
3. 次の値を控える。

| 値 | 確認方法 |
|---|---|
| App ID | App の設定ページの **About** 欄 |
| Installation ID | インストール後の URL `settings/installations/<数字>` |
| Bot のユーザー ID | `gh api 'users/<App 名>[bot]' --jq .id` |

## 3. リポジトリの内容を書き換える

[README の一覧](../README.md#テンプレートから新規プロジェクトを作るとき) に加えて、以下を置き換える。

| ファイル | 置き換える内容 |
|---|---|
| `.github/CODEOWNERS` | `@kajip` をレビュー担当者に |
| `.github/agent-app.env` | App ID と Installation ID |
| `scripts/agent-github-token.sh` | 秘密鍵の既定のパスの App 名 |
| `doc/agent-workflow.md` | コミットの名義（App 名と Bot のユーザー ID） |
| `doc/parameters.md` | 秘密鍵の既定のパスの App 名 |
| `doc/directory-structure.md` | パッケージのパス |

クラス名などを変えると `doc/autogen/` が古くなるので、Dev Container 内で `./gradlew generateDocs check` を実行する。

コミットして `main` に push する。保護ルールを入れる前のため、直接 push できる。Claude には `main` への push を禁じているので、人が実行する。

## 4. CI の成功を確認する

push で動く `check` の成功を Actions の画面か次のコマンドで確認する。保護ルールは `check` の成功を必須にするため、失敗したままルールを入れるとマージできなくなる。

```sh
gh run list --branch main --limit 1
```

## 5. リポジトリの設定を変更する

プロジェクト直下で 1 行ずつ実行する。`{owner}` と `{repo}` は `gh` が補うので書き換えない。

```sh
gh api --silent -X PATCH repos/{owner}/{repo} -F allow_auto_merge=true
gh api --silent -X PATCH repos/{owner}/{repo} -F delete_branch_on_merge=true
gh api --silent -X PUT repos/{owner}/{repo}/vulnerability-alerts
```

## 6. `main` の保護を設定する

```sh
gh api --silent -X POST repos/{owner}/{repo}/rulesets --input .github/rulesets/main.json
```

## 7. 動作を確認する

App の名義で Pull Request を 2 件作る。手順は [Agent による開発フロー](agent-workflow.md) にある。

| 変更箇所 | 期待する結果 |
|---|---|
| `doc/` のみ | CI の成功後、無人で自動マージされる |
| `src/main/` や `.github/` などを含む | CI が通っても `BLOCKED` のまま、レビュー担当者の承認を待つ |

人の認証情報で試すと、管理者の迂回が働いて振り分けを確認できない。必ず App の名義で試す。

## 任意の設定

- **シークレットスキャン：** リポジトリの設定でシークレットスキャンと push 保護を有効にする。本リポジトリでは未設定で、効果を確認していない。
- **Template repository：** さらに複製元として使う場合は、リポジトリの設定で有効にする。
