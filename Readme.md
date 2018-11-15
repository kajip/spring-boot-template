# Spring Boot アプリケーション の Template

## 概要

Spring Boot でコマンドラインツールを作るためのテンプレートプロジェクト

## 予めインストールが必要なアプリケーション

* Java 1.8 以上
* Docker
* Dockerイメージを保存する先毎のDocker Credential Helpers
  * Google Container Registry: [docker-credential-gcr](https://cloud.google.com/container-registry/docs/advanced-authentication#docker_credential_helper)
  * AWS Elastic Container Registry: [docker-credential-ecr-login](https://github.com/awslabs/amazon-ecr-credential-helper)
  * Docker Hub Registry: [docker-credential-helpers](https://github.com/docker/docker-credential-helpers)

## 新しいプロジェクトを作るときに必要な作業

1. プロジェクト名の設定

下記のファイルのプロジェクト名を修正する

* settings.gradle ファイルの rootProject.name プロパティ
* src/main/resources/config/application.yml の spring.application.name プロパティ

1. 依存ライブラリの修正

2. サンプルプロジェクトの削除

## ビルド方法

### アプリケーションの起動

```bash
./gradlew bootRun
```

### アプリケーションのテスト

```bash
./gradlew test
```

### アプリケーションのビルド

```bash
./gradlew clean build
```

### Dockerイメージの作成

環境変数でビルド日時、コミットIDを設定してからjibDockerBuildタスクを実行する

```bash
# コミットIDの埋め込み
export ORG_GRADLE_PROJECT_vcsRef=$(git show -s --format=%H)
# ビルド時間の埋め込み(RFC3339フォーマットが望ましい)
export ORG_GRADLE_PROJECT_buildDate=$(date)
./gradle jibDockerBuild
```

### Dockerイメージのpush

予め、push先のレポジトリのURLをgradle.propertiesファイルに設定しておく。

イメージ作成時と同じ環境変数を設定後、jibタスクを実行する

```bash
# コミットIDの埋め込み
export ORG_GRADLE_PROJECT_vcsRef=$(git show -s --format=%H)
# ビルド時間の埋め込み(RFC3339フォーマットが望ましい)
export ORG_GRADLE_PROJECT_buildDate=$(date)
./gradle jib
```

### Dockerイメージからアプリケーションを起動

```bash
docker -d --rm プロジェクト名
```
