# ディレクトリ構造

Git で管理する主なファイルの構成。`build/` と `.gradle/` はビルド時に作られ、Git では管理しない。

```text
.
├── .claude/                     Claude Code の設定
│   ├── hooks/run-check.sh       応答終了時に generateDocs と check を実行する Stop hook
│   └── settings.json            hook の登録
├── .devcontainer/               開発環境
│   ├── Dockerfile               開発用イメージ（JDK、typos、tbls）
│   ├── compose.yaml             開発コンテナ（app）と PostgreSQL（db）の定義
│   ├── devcontainer.json        Dev Container の設定
│   └── init-test-db.sql         テスト用データベース app_test の作成
├── .github/                     GitHub の設定
│   ├── CODEOWNERS               人のレビューが必要なパス
│   ├── agent-app.env            Agent 用 GitHub App の ID（秘密ではない）
│   └── workflows/check.yml      CI（Dev Container で check を実行）
├── config/pmd/ruleset.xml       PMD のルール
├── doc/                         ドキュメント
│   ├── agent-workflow.md        Agent による開発フロー
│   ├── autogen/                 生成物（直接編集しない）
│   │   ├── class-diagram.md     クラス図
│   │   └── schema/              ER 図とテーブル定義
│   ├── directory-structure.md   この文書
│   └── parameters.md            環境変数とパラメータの一覧
├── gradle/wrapper/              Gradle Wrapper の本体と設定
├── scripts/agent-github-token.sh  Agent 用 GitHub App のトークン発行
├── src/
│   ├── main/
│   │   ├── java/org/kajip/app/  アプリケーションのコード
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/    Flyway のマイグレーション
│   └── test/java/org/kajip/app/ テストとクラス図の生成器
├── .dockerignore                Docker イメージのビルドから除外するファイル
├── .gitattributes               改行コードなどの Git 属性
├── .gitignore                   Git の管理から除外するファイル
├── .tbls.yml                    tbls の設定
├── CLAUDE.md                    Claude Code への指示
├── Dockerfile                   アプリケーションの Docker イメージ
├── README.md                    利用者向けの手順
├── build.gradle.kts             ビルド設定
├── gradlew / gradlew.bat        Gradle Wrapper の起動スクリプト
└── settings.gradle.kts          Gradle のプロジェクト名
```
