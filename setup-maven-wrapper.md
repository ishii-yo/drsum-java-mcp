# Maven Wrapperのセットアップガイド

## Maven Wrapperとは

Maven Wrapper（mvnw）は、プロジェクトに特定のMavenバージョンを同梱し、Mavenのインストールなしでビルドを実行できるツールです。

## 特徴

### メリット
- ✅ Mavenの個別インストールが不要
- ✅ プロジェクト全体で同じMavenバージョンを保証
- ✅ 新しい開発者がすぐに始められる
- ✅ CI/CDパイプラインの構築が簡単

### デメリット
- ⚠️ 初回実行時にMavenをダウンロード（数十MB）
- ⚠️ プロジェクトのリポジトリサイズが若干増加（約60KB）

## セットアップ方法

### 方法1: 既存のMavenを使用（推奨）

もし一時的にでもMavenが使える環境がある場合：

```bash
# プロジェクトディレクトリで実行
mvn wrapper:wrapper -Dmaven=3.9.6
```

これで以下のファイルが生成されます：
- `mvnw` (Unix/Linux/macOS用)
- `mvnw.cmd` (Windows用)
- `.mvn/wrapper/maven-wrapper.jar`
- `.mvn/wrapper/maven-wrapper.properties`

### 方法2: 手動セットアップ（Mavenがない場合）

Maven Wrapperのファイルを直接ダウンロードして配置します。

#### ステップ1: 必要なディレクトリを作成

```powershell
# PowerShellで実行
New-Item -ItemType Directory -Force -Path ".mvn\wrapper"
```

#### ステップ2: ファイルをダウンロード

以下のファイルをダウンロードする必要があります：

1. **mvnw.cmd** (Windowsスクリプト)
2. **mvnw** (Unix/Linux/macOSスクリプト)
3. **maven-wrapper.jar** (Wrapperの実行ファイル)
4. **maven-wrapper.properties** (設定ファイル)

これらは以下から入手できます：
- Apache Maven公式リポジトリ
- 既存のMaven Wrapperプロジェクトから

#### ステップ3: 検証

```powershell
# Windows
.\mvnw.cmd --version

# Unix/Linux/macOS
./mvnw --version
```

### 方法3: 参考プロジェクトからコピー（最も簡単）

MCP Java SDKのリポジトリにはすでにMaven Wrapperが含まれているため、そこからコピーすることもできます。

```powershell
# MCP Java SDKをクローン（既にある場合はスキップ）
git clone https://github.com/modelcontextprotocol/java-sdk.git temp-mcp-sdk

# Maven Wrapperファイルをコピー
Copy-Item -Path "temp-mcp-sdk\mvnw.cmd" -Destination "." -Force
Copy-Item -Path "temp-mcp-sdk\mvnw" -Destination "." -Force
Copy-Item -Path "temp-mcp-sdk\.mvn" -Destination "." -Recurse -Force

# 一時ディレクトリを削除
Remove-Item -Path "temp-mcp-sdk" -Recurse -Force
```

## 使用方法

Maven Wrapperがセットアップされたら、通常の`mvn`コマンドの代わりに`mvnw`を使用します：

### Windows (PowerShell/CMD)

```powershell
# ビルド
.\mvnw.cmd clean compile

# テスト
.\mvnw.cmd test

# パッケージング
.\mvnw.cmd clean package

# インストール
.\mvnw.cmd clean install
```

### Unix/Linux/macOS

```bash
# ビルド
./mvnw clean compile

# テスト
./mvnw test

# パッケージング
./mvnw clean package

# インストール
./mvnw clean install
```

## 初回実行時の動作

初めて`mvnw`を実行すると：

1. 指定されたMavenバージョンをダウンロード
2. ユーザーのホームディレクトリにキャッシュ
   - Windows: `%USERPROFILE%\.m2\wrapper\dists\`
   - Unix/Linux/macOS: `~/.m2/wrapper/dists/`
3. ダウンロード完了後、通常のMavenとして動作

2回目以降はキャッシュを使用するため、ダウンロードは不要です。

## トラブルシューティング

### エラー: 'mvnw.cmd' は認識されません

**原因**: Maven Wrapperがまだセットアップされていない

**解決策**: 上記のいずれかの方法でセットアップを実行

### エラー: ダウンロードに失敗

**原因**: ネットワーク接続の問題、またはプロキシ設定

**解決策**: 
```powershell
# プロキシ設定（必要な場合）
$env:MAVEN_OPTS="-Dhttp.proxyHost=proxy.example.com -Dhttp.proxyPort=8080"
.\mvnw.cmd clean compile
```

### エラー: 実行権限がない（Unix/Linux/macOS）

**原因**: `mvnw`に実行権限がない

**解決策**:
```bash
chmod +x mvnw
./mvnw --version
```

## .gitignoreの設定

Maven Wrapperのファイルは通常Gitにコミットしますが、以下は除外します：

```gitignore
# Maven Wrapper - コミットする
# mvnw
# mvnw.cmd
# .mvn/wrapper/maven-wrapper.jar
# .mvn/wrapper/maven-wrapper.properties

# Maven Wrapper - 除外する
.mvn/wrapper/maven-wrapper.jar  # 一部のプロジェクトでは除外
```

**推奨**: すべてのMaven Wrapperファイルをコミットすることで、他の開発者がすぐに始められます。

## Maven Wrapperの設定ファイル

`.mvn/wrapper/maven-wrapper.properties`の内容例：

```properties
# Maven Wrapper設定
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
```

## まとめ

Maven Wrapperは、プロジェクトの依存性管理とビルドプロセスを簡素化する優れたツールです。特にチーム開発やCI/CDパイプラインでは必須と言えます。

MCP Java SDKプロジェクトもMaven Wrapperを使用しているため、同じアプローチを採用することで統一された開発体験を提供できます。
