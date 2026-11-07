# ガードの説明

Agent に開発を任せても品質と安全を保つための仕組みをまとめる。手元のガードは迂回できるため、最終的な守りは GitHub 側に置く。

| 層 | 効く相手 | 迂回の可否 |
|---|---|---|
| Claude Code の設定 | Claude Code | 他の書き方や別の環境で迂回できる |
| `./gradlew check` | 実行した人と Agent | 実行しなければ効かない |
| GitHub | 全員 | 管理者のみ Pull Request 経由で迂回できる |

## Claude Code の設定

| ガード | 内容 | 設定場所 |
|---|---|---|
| 作業ルール | ブランチ戦略、App の名義、生成物の扱いなど | `CLAUDE.md` |
| 編集の拒否 | `doc/autogen/` の編集、`.env` の読み取り | `.claude/settings.json` |
| コマンドの拒否 | `main` への push、強制 push | `.claude/settings.json` |
| 自動検査 | 応答の終了時に、未検証の変更へ `generateDocs` と `check` を実行し、失敗を Agent に返す | `.claude/hooks/run-check.sh` |

`permissions.deny` のコマンド拒否は前方一致なので、`git -C <パス> push` のような書き方には一致しない。誤操作の防止と位置づける。

## `./gradlew check`

Dev Container 内で実行する。CI でも同じ内容を実行する。

| 防ぐもの | 手段 | 基準 |
|---|---|---|
| コンパイラ警告 | `javac -Xlint:all -Werror` | 警告 0 件 |
| タイポ | typos | 検出 0 件 |
| コード規約違反 | PMD | 違反 0 件 |
| 書式の崩れ | Spotless | 差分なし |
| 動作の不具合と Bean の定義漏れ | JUnit | 全件成功 |
| テストの実行漏れ | JaCoCo | 行カバレッジ 80% 以上 |
| テストの検証漏れ | PIT | ミューテーションスコア 80% 以上 |
| 設計の崩れ | ArchUnit | 配置、依存方向、循環など全ルール |
| 生成物の古さと直接編集 | テストと `verifySchemaDoc` | `doc/autogen/` が再生成の結果と一致 |

カバレッジとスコアはプロジェクト全体の割合で判定するため、コードが増えると小さな劣化は閾値の範囲内に埋もれる。

## GitHub

| ガード | 内容 | 設定場所 |
|---|---|---|
| CI | Pull Request と `main` への push で `check` とスモークテストを実行する | `.github/workflows/check.yml` |
| `main` の保護 | Pull Request と CI の成功を必須とし、強制 push と削除を禁止する | `.github/rulesets/main.json` |
| マージの振り分け | 全パスを人のレビュー対象とし、`doc/`、`README.md`、`src/test/` のみ外す | `.github/CODEOWNERS` |
| Agent の名義 | 管理者権限のない GitHub App で、トークンは 1 時間で失効する | `.github/agent-app.env` |
| 依存関係の監視 | 更新の Pull Request と脆弱性の通知 | `.github/dependabot.yml`、リポジトリ設定 |

CODEOWNERS は既定で全パスを対象とするため、新しいパスは人のレビュー対象になる。CI の定義と保護ルールの定義も対象に含まれる。

唯一の Code Owner は自分の Pull Request を承認できないため、管理者に Pull Request 経由の迂回を許している。直接の push は管理者でも禁止する。

## 実地で確認した結果

| 確認した内容 | 結果 |
|---|---|
| App が `doc/` のみを変更する Pull Request | CI の成功後、無人で自動マージされた（#3） |
| App が所有パスを変更する Pull Request | CI が通っても承認まで止まり、承認後にマージされた（#2） |
| App による `main` への直接 push | `GH013` で拒否された |
| App によるリポジトリ設定の変更 | `403` で拒否された |
| App による CI 定義の変更 | push が拒否された |
| 管理者の Pull Request 経由の迂回 | 承認なしでマージできた（#8） |
| `check` の各検査 | 違反を仕込むと該当の検査が失敗した |
| PIT | テストの検証を弱めると、JaCoCo は通り PIT が失敗した |
| スモークテスト | 接続を壊すと失敗し、後片付けも行われた |

Dependabot の更新の Pull Request は、まだ実物を確認していない。

## 残る弱点

- **人の認証情報：** 同じ環境に人の認証情報があると、Agent は人の名義で操作でき、レビューの関門も迂回できる。対策の設計は [Agent 実行コンテナの設計](agent-container-design.md) にある。
- **無人マージの範囲：** パスでは変更の意味を判定できない。`src/test/` の変更で検証を弱めても、閾値の範囲内なら無人でマージされる。
- **検査の網羅性：** OpenAPI は文書の鮮度のみを検査し、破壊的な変更かどうかは判定しない。
- **秘密情報の検出：** GitHub のシークレットスキャンは現在無効である。
- **手書きの文書：** この文書を含む手書きの文書は `check` で検査されない。
