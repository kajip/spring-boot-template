# GitHub の設定手順

このテンプレートから新しいリポジトリを作るときの手順。リポジトリの設定、保護ルール、GitHub App は複製されないため、リポジトリごとに設定する。

各設定の役割は [ガードの説明](guards.md) を参照する。この手順は本リポジトリで行った設定に基づくが、複製から通しで実施した検証はまだ行っていない。

## 前提

- **実行場所：** 特に断りがなければ、Mac（ホスト）のプロジェクト直下で実行する。
- **ツール：** [README の「必要なもの」](../README.md#必要なもの) を揃え、`gh auth status` で `gh` のログインを確かめる。
- **アカウント：** 個人アカウントが所有するリポジトリを前提とする。Organization の場合は組織の設定から App を作る必要があり、その手順は検証していない。
- **プラン：** 非公開リポジトリでは、契約プランによりブランチ保護や Code Owner の必須化を使えない場合がある。[料金ページ](https://github.com/pricing) で先に確認する。
- **Agent を使わない場合：** この構成は検証していない。

## 1. リポジトリを作る

1. テンプレートのページで **Use this template** から新しいリポジトリを作る。`Include all branches` は選ばない。
2. 作ったリポジトリをホームディレクトリ配下に clone する。

## 2. GitHub App を作る

Agent 専用の App をリポジトリごとに作る。作者は自分の Pull Request を承認できないため、人のアカウントとは分ける。

[New GitHub App](https://github.com/settings/apps/new) で以下を設定する。

| 区分 | 項目 | 値 |
|---|---|---|
| 基本 | GitHub App name | GitHub 全体で一意の名前。小文字とハイフンを推奨 |
| 基本 | Homepage URL | リポジトリの URL |
| 基本 | Webhook の Active | 外す |
| 基本 | Where can this GitHub App be installed? | Only on this account |
| Repository permissions | Contents | Read and write |
| Repository permissions | Pull requests | Read and write |
| Repository permissions | Checks | Read-only |
| Repository permissions | Commit statuses | Read-only |
| Repository permissions | Metadata | Read-only |

**Administration と Workflows は与えない。**

以降の `<App 名>` は、App のページの URL `github.com/apps/<ここ>` の値を指す。

作成後に以下を行う。

1. App の設定ページの **Generate a private key** で秘密鍵を生成し、リポジトリの外に置く。秘密鍵をチャットやリポジトリに置かない。

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

新しいプロジェクトの名前に置き換える。

| ファイル | 置き換える内容 |
|---|---|
| `settings.gradle.kts` | `rootProject.name` |
| `build.gradle.kts` | `group`、`mainClass`、`pitest` のパッケージ名 |
| `src/main/resources/application.properties` | `spring.application.name` |
| `src/main/java`、`src/test/java` | パッケージ名、`SpringBootTemplateApplication` と `SpringBootTemplateApplicationTests` のクラス名 |
| `src/test/.../ArchitectureTests.java` | パッケージ名の文字列 |
| `.devcontainer/devcontainer.json` | `name`、開発用イメージ名 `spring-boot-template-dev` |
| `.devcontainer/compose.yaml` | 開発用イメージ名 `spring-boot-template-dev` |
| `.github/CODEOWNERS` | `@kajip` をレビュー担当者に。`ArchitectureTests.java` のパス。担当者にはリポジトリへの書き込み権限が要る |
| `.github/agent-app.env` | App ID と Installation ID |
| `scripts/agent-github-token.sh` | 秘密鍵の既定のパスの App 名 |
| `doc/agent-workflow.md` | コミットの名義（App 名と Bot のユーザー ID）、秘密鍵のパス |
| `doc/parameters.md` | 秘密鍵の既定のパスの App 名 |
| `doc/directory-structure.md` | パッケージのパス |
| `README.md` | 見出し、Docker イメージ名、パッケージ名とパス、テストのクラス名 |

Gradle キャッシュの volume 名 `spring-boot-gradle-cache` は、複数のプロジェクトで共有するためにそのまま残す。

置き換え漏れを検索で確かめる。`doc/guards.md` の実地の記録と、`doc/github-setup.md` の説明は残ってよい。

```sh
git grep -nE 'kajip|claude-code-coding-agent|spring-boot-template|SpringBootTemplate|353756006|5656593|179584177'
```

クラス名などを変えると `doc/autogen/` が古くなるので、再生成して全検査を通す。

```sh
npx -y @devcontainers/cli up --workspace-folder .
npx -y @devcontainers/cli exec --workspace-folder . ./gradlew generateDocs check
```

コミットして `main` に push する。保護ルールを入れる前のため、直接 push できる。人の名義で実行する。

## 4. CI の成功を確認する

保護ルールは `check` の成功を必須にするため、失敗したままルールを入れるとマージできなくなる。

```sh
gh run list --branch main --limit 1
gh run watch <上で表示された ID> --exit-status
```

失敗したら `gh run view <ID> --log-failed` で原因を確かめる。

CODEOWNERS に誤りが無いことも確かめる。`errors` が空であればよい。

```sh
gh api repos/{owner}/{repo}/codeowners/errors
```

## 5. リポジトリの設定を変更する

1 行ずつ実行する。`{owner}` と `{repo}` は `gh` が補うので書き換えない。

```sh
gh api --silent -X PATCH repos/{owner}/{repo} -F allow_auto_merge=true
gh api --silent -X PATCH repos/{owner}/{repo} -F delete_branch_on_merge=true
gh api --silent -X PUT repos/{owner}/{repo}/vulnerability-alerts
```

## 6. `main` の保護を設定する

```sh
gh api --silent -X POST repos/{owner}/{repo}/rulesets --input .github/rulesets/main.json
```

以降、`main` への変更は Pull Request 経由のみとなる。自分の Pull Request は自分では承認できないので、人が作った変更は CI の成功後に `gh pr merge <番号> --squash --admin` でマージする。

### 保護ルールを変更するとき

`.github/rulesets/main.json` を変えても GitHub には反映されない。ファイルを変更してマージしたうえで、人が適用する。再度 POST すると別のルールが増える恐れがあるため、既存のものを更新する。この更新の手順は実施して確かめていない。

```sh
gh api repos/{owner}/{repo}/rulesets --jq '.[] | "\(.id) \(.name)"'
gh api --silent -X PUT repos/{owner}/{repo}/rulesets/<ID> --input .github/rulesets/main.json
```

## 7. 動作を確認する

App の名義で Pull Request を 2 件作る。手順は [Agent による開発フロー](agent-workflow.md) にある。人の認証情報で試すと、管理者の迂回が働いて振り分けを確認できない。

| 変更箇所 | 期待する結果 |
|---|---|
| `doc/` のみ | CI の成功後、無人で自動マージされる |
| `src/main/` を含む | CI が通っても `BLOCKED` のまま、レビュー担当者の承認を待つ |

状態と名義は次のコマンドで確かめる。`author` が `app/` で始まり、`mergeStateStatus` が `BLOCKED` であればよい。

```sh
gh pr view <番号> --json author,mergeStateStatus,reviewRequests
```

確認用の Pull Request は、終わったら閉じる。

```sh
gh pr close <番号> --delete-branch
```

## 任意の設定

- **シークレットスキャン：** リポジトリの設定でシークレットスキャンと push 保護を有効にする。本リポジトリでは未設定で、効果を確認していない。
- **Template repository：** さらに複製元として使う場合は、リポジトリの設定で有効にする。
