# ガードの説明

Agent（Claude Code）に開発を任せても品質と安全を保つための仕組みをまとめる。手元のガードは迂回できるため、最終的な守りは GitHub 側に置く。

| 層 | 効く相手 | 迂回の可否 |
|---|---|---|
| Claude Code の設定 | Claude Code | 他の書き方や別の環境で迂回できる |
| `./gradlew check` | 実行した人と Agent | 実行しなければ効かない |
| GitHub | 全員 | 管理者のみ Pull Request 経由で迂回できる |

## 用語

| 用語 | 意味 |
|---|---|
| Dev Container | 開発環境を丸ごと収めたコンテナ。ホストに Java を入れずに開発できる |
| Stop hook | Claude Code の応答が終わる時に自動で動く処理 |
| CODEOWNERS | パスごとにレビュー担当者を定める GitHub の仕組み |
| GitHub App | 人とは別の名義で GitHub を操作するための仕組み |
| PIT | コードをわざと壊してテストが検出するかを測るツール。検出できた割合がミューテーションスコア |
| ArchUnit | パッケージの配置や依存方向などの設計ルールをテストとして書くツール |
| スモークテスト | 完成品を実際に動かし、大きな破綻が無いかを確かめるテスト |
| `BLOCKED` | Pull Request がマージ条件を満たしていない状態 |
| 管理者の迂回 | リポジトリの管理者が、承認なしで Pull Request をマージできること |

## Claude Code の設定

| ガード | 内容 | 設定場所 |
|---|---|---|
| 作業ルール | ブランチ戦略、App の名義、生成物の扱いなど | `CLAUDE.md` |
| 編集の拒否 | `doc/autogen/` の編集、`.env` の読み取り | `.claude/settings.json` |
| コマンドの拒否 | `git push` で始まる `main` への push と強制 push | `.claude/settings.json` |
| 自動検査 | 応答の終了時に `generateDocs` と `check` を実行し、失敗を Agent に返す | `.claude/hooks/run-check.sh` |

コマンドの拒否は前方一致なので、次の書き方には一致しない。誤操作の防止と位置づけ、実際に push を止めるのは GitHub の保護とする。

- `git -C <パス> push`、`git -c ... push`（標準の手順もこの形）
- `main` 上での引数なしの `git push`、`git push origin HEAD`

自動検査は次の場合に実行されない。最終的な検査は CI が担う。

| 場合 | 動作 |
|---|---|
| 未コミットの変更が無い（応答内でコミットまで済ませた） | 何もしない |
| 前回成功した内容と同じ | 省略する |
| 前回失敗した内容と同じ | 省略して通知する |
| 修正の再試行が 3 回に達した | 中断して通知する |
| Dev Container を起動できない | 省略して通知する |
| 他のコンテナが Gradle キャッシュを使用中 | 完了できず通知し、次回に再実行する |

`generateDocs` が書き換えた `doc/autogen/` は未コミットのまま残る。そのままでは CI の鮮度確認で失敗する。

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

| ガード | 内容 | 定義の場所 |
|---|---|---|
| CI | Pull Request と `main` への push で `check` とスモークテストを実行する | `.github/workflows/check.yml` |
| `main` の保護 | Pull Request と CI の成功を必須とし、強制 push と削除を禁止する | GitHub の設定。元の定義は `.github/rulesets/main.json` |
| マージの振り分け | 全パスを人のレビュー対象とし、`doc/`、`README.md`、`src/test/` のみ外す | `.github/CODEOWNERS` |
| Agent の名義 | 管理者権限のない GitHub App で、トークンは 1 時間で失効する | GitHub App の設定画面。ID は `.github/agent-app.env` |
| 依存関係の監視 | 更新の Pull Request と脆弱性の通知 | `.github/dependabot.yml`、リポジトリ設定 |
| 秘密情報の検出 | シークレットスキャンと push 保護で、既知の形式の秘密情報を検出し、push を止める | リポジトリ設定 |

`.github/rulesets/main.json` は設定時に一度送る入力で、ファイルを変えても GitHub には反映されない。変更手順は [GitHub の設定手順](github-setup.md#保護ルールを変更するとき) にある。

CODEOWNERS は既定で全パスを対象とするため、新しいパスは人のレビュー対象になる。CI の定義と保護ルールの定義も対象に含まれる。無人の範囲内でも、`ArchitectureTests.java` と `doc/agent-workflow.md` はガードや Agent の指示そのものなので、人のレビュー対象に戻している。

唯一の Code Owner は自分の Pull Request を承認できないため、管理者に Pull Request 経由の迂回を許している（`gh pr merge --admin`）。直接の push は管理者でも禁止する。

## 実地で確認した結果

テンプレート本体のリポジトリで 2026 年 11 月に確認した記録である。Pull Request の番号もそのリポジトリのもので、複製先には当てはまらない。複製先では [手順 7](github-setup.md#7-動作を確認する) で確かめ直す。

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

未確認の点は次のとおり。

- Dependabot の更新の Pull Request の実物
- シークレットスキャンと push 保護の実際の検出（公開リポジトリに秘密情報の模造品を送る試験はしていない）
- 管理者の迂回で、CI の成功も省略できるか
- Code Owner の名前を誤ったときの動作。無効な行は所有者なしの扱いとなり、全パスが無人の対象になる恐れがある

## 残る弱点

- **人の認証情報：** 同じ環境に人の認証情報があると、Agent は人の名義で操作でき、レビューの関門も迂回できる。対策の設計は [Agent 実行コンテナの設計](agent-container-design.md) にある。
- **無人マージの範囲：** パスでは変更の意味を判定できない。`src/test/` のテストの削除や無効化、`doc/autogen/` の鮮度確認を行うテストの変更は、閾値の範囲内なら無人でマージされ得る。手順書の `doc/github-setup.md` と、この文書も無人で変更できる。
- **検査の網羅性：** OpenAPI は文書の鮮度のみを検査し、破壊的な変更かどうかは判定しない。
- **秘密情報の検出：** シークレットスキャンが検出できるのは既知の形式のみである。独自形式のパスワードなどは検出できず、push 保護もコマンドの指定で解除され得る。
- **手書きの文書：** この文書を含む手書きの文書は `check` で検査されない。
