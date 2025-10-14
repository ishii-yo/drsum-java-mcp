# DrSum Java MCP Server

このプロジェクトは、[Model Context Protocol (MCP)](https://modelcontextprotocol.org/) Java SDKを使用して構築された、**Dr.Sumデータベース分析機能**を提供するMCPサーバーです。

## 概要

DrSum MCP Serverは、AIアシスタント（Claude Desktopなど）やアプリケーションに、Dr.Sumデータベースへの接続・分析機能を提供します。LLMがDr.Sumのデータを直接分析できるようになり、自然言語でのデータクエリや分析が可能になります。

## 主な機能

### Dr.Sum分析機能
- **データベース接続管理**: Dr.Sumサーバーへの接続・切断
- **メタ情報取得**: テーブル構造とサンプルデータの取得（デフォルト3行）
- **SQLクエリー実行**: 任意のSQLクエリを実行し、結果をJSON形式で取得

### その他の機能
- **テキスト要約**: 長いテキストを指定された文数に要約（サンプル機能）
- **MCP標準準拠**: Model Context Protocolの仕様に準拠
- **STDIO通信**: 標準入出力を使用した通信
- **セキュリティ**: パスワードの秘匿化

## 必要条件

- Java 17以上
- Maven 3.6以上

## 依存関係

このプロジェクトは以下の依存関係を使用しています：

### MCP Java SDK
- `io.modelcontextprotocol.sdk:mcp` (0.14.1) - MCP Java SDK Core
- `io.modelcontextprotocol.sdk:mcp-bom` - 依存関係バージョン管理
- `io.modelcontextprotocol.sdk:mcp-test` - テスト用ユーティリティ

### Dr.Sum EA ライブラリ
- `jp.co.dw_sapporo:DrSumEA` (5.7.0) - Dr.Sum Enterprise Analytics API
  - DBI（Database Interface）機能を使用
  - ローカルMavenリポジトリにインストールが必要

### その他
- `org.slf4j:slf4j-api` (2.0.16) - ロギングAPI
- `ch.qos.logback:logback-classic` (1.4.14) - ロギング実装
- `org.junit.jupiter:junit-jupiter` (5.10.1) - テストフレームワーク

## DrSumEA.jarのインストール

Dr.Sum接続機能を使用するには、DrSumEA.jarをローカルMavenリポジトリにインストールする必要があります：

```powershell
# Windows (PowerShell)
.\mvnw.cmd install:install-file "-Dfile=C:\DrSum57\DevKit\java\api\DrSumEA.jar" "-DgroupId=jp.co.dw_sapporo" "-DartifactId=DrSumEA" "-Dversion=5.7.0" "-Dpackaging=jar"
```

```bash
# Unix/Linux/macOS
./mvnw install:install-file -Dfile=/path/to/DrSumEA.jar -DgroupId=jp.co.dw_sapporo -DartifactId=DrSumEA -Dversion=5.7.0 -Dpackaging=jar
```

## セットアップ

このプロジェクトはMaven Wrapperを使用しているため、Mavenをインストールする必要はありません。

### Windows

1. **プロジェクトのクローンまたはダウンロード**

2. **依存関係のインストール**
   ```cmd
   .\mvnw.cmd clean install
   ```

3. **プロジェクトのビルド**
   ```cmd
   .\mvnw.cmd clean compile
   ```

4. **テストの実行**
   ```cmd
   .\mvnw.cmd test
   ```

### Unix/Linux/macOS

1. **プロジェクトのクローンまたはダウンロード**

2. **依存関係のインストール**
   ```bash
   ./mvnw clean install
   ```

3. **プロジェクトのビルド**
   ```bash
   ./mvnw clean compile
   ```

4. **テストの実行**
   ```bash
   ./mvnw test
   ```

## 使用方法

### 方法1: コマンドラインから直接起動（開発・テスト用）

#### Windows (PowerShell)
```powershell
# Maven Wrapperを使用して起動
.\mvnw.cmd exec:java "-Dexec.mainClass=com.example.drsum.DrSumMcpServer"

# または起動スクリプトを使用
.\scripts\start-server.bat
```

#### Unix/Linux/macOS
```bash
# Maven Wrapperを使用して起動
./mvnw exec:java -Dexec.mainClass="com.example.drsum.DrSumMcpServer"

# または起動スクリプトを使用
./scripts/start-server.sh
```

**注意**: この方法ではサーバーがSTDIO（標準入出力）モードで起動し、JSON-RPCメッセージの入力待ちになります。通常はMCPクライアントから自動起動されるため、手動起動は必要ありません。

### 方法2: 実行可能JARを作成して起動（本番・配布用）

#### ステップ1: パッケージのビルド
```powershell
# Windows
.\mvnw.cmd clean package

# Unix/Linux/macOS
./mvnw clean package
```

これにより以下が作成されます：

- `target/drsum-java-mcp-1.0.0-SNAPSHOT.jar` - メインJAR（依存関係なし）
- `target/drsum-java-mcp-1.0.0-SNAPSHOT-fat.jar` - Fat JAR（依存関係含む）
- `target/lib/` - 依存ライブラリ（53個のJARファイル）

**JARの種類について:**

- **Fat JAR**: 全ての依存関係が1つのJARファイルに含まれています。配布が簡単で、単一ファイルで動作します。
- **通常JAR**: 依存関係は分離されており、`lib/`フォルダ内のJARファイルが必要です。ファイルサイズは小さくなります。

#### ステップ2A: Fat JAR で起動（推奨 - 単一ファイル）

**必要な条件:**
- Java 17以上がインストール済み
- Fat JARファイルのみ

```powershell
# Windows
java -jar target\drsum-java-mcp-1.0.0-SNAPSHOT-fat.jar

# Unix/Linux/macOS
java -jar target/drsum-java-mcp-1.0.0-SNAPSHOT-fat.jar
```

**配布時に必要なファイル:**
```
drsum-java-mcp-1.0.0-SNAPSHOT-fat.jar  # このファイルのみ
```

#### ステップ2B: 通常JARで起動（依存関係分離）

**必要な条件:**
- Java 17以上がインストール済み
- メインJARファイル
- `lib/`フォルダ内の全依存関係（53個のJARファイル）
- JARファイルと`lib/`フォルダが同じディレクトリに配置されていること

```powershell
# Windows
java -jar target\drsum-java-mcp-1.0.0-SNAPSHOT.jar

# Unix/Linux/macOS
java -jar target/drsum-java-mcp-1.0.0-SNAPSHOT.jar
```

**配布時に必要なファイル構造:**
```
your-distribution/
├── drsum-java-mcp-1.0.0-SNAPSHOT.jar  # メインJAR
└── lib/                                # 依存ライブラリフォルダ
    ├── mcp-0.14.1.jar
    ├── jackson-databind-2.17.0.jar
    ├── slf4j-api-2.0.16.jar
    └── ... (他50個のJARファイル)
```

**注意:** `lib/`フォルダが見つからない場合、以下のエラーが発生します：
```
Exception in thread "main" java.lang.NoClassDefFoundError: io/modelcontextprotocol/...
```

### 方法3: MCPクライアント（Claude Desktopなど）から自動起動（推奨）

MCPクライアントが自動的にサーバーを起動・管理します。サーバーを手動で起動する必要はありません。

#### Claude Desktop での設定

Claude Desktopの設定ファイルに以下を追加します：

**設定ファイルの場所:**
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`
- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Linux: `~/.config/Claude/claude_desktop_config.json`

**設定例1: Maven Wrapperで起動（開発時）**

Windows:
```json
{
  "mcpServers": {
    "drsum": {
      "command": "cmd.exe",
      "args": [
        "/c",
        "C:\\mb_dev\\github\\drsum-java-mcp\\mvnw.cmd",
        "exec:java",
        "-Dexec.mainClass=com.example.drsum.DrSumMcpServer",
        "-q"
      ],
      "env": {
        "JAVA_HOME": "C:\\java\\jdk-17"
      }
    }
  }
}
```

Unix/Linux/macOS:
```json
{
  "mcpServers": {
    "drsum": {
      "command": "/path/to/drsum-java-mcp/mvnw",
      "args": [
        "exec:java",
        "-Dexec.mainClass=com.example.drsum.DrSumMcpServer",
        "-q"
      ],
      "env": {
        "JAVA_HOME": "/path/to/jdk-17"
      }
    }
  }
}
```

**設定例2A: Fat JARで起動（推奨・単一ファイル）**

```json
{
  "mcpServers": {
    "drsum": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\mb_dev\\github\\drsum-java-mcp\\target\\drsum-java-mcp-1.0.0-SNAPSHOT-fat.jar"
      ]
    }
  }
}
```

**設定例2B: 通常JARで起動（依存関係分離）**

```json
{
  "mcpServers": {
    "drsum": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\mb_dev\\github\\drsum-java-mcp\\target\\drsum-java-mcp-1.0.0-SNAPSHOT.jar"
      ]
    }
  }
}
```

**注意:** 設定例2Bを使用する場合、以下の条件が必要です：
- `target/lib/`フォルダが存在すること
- JARファイルと`lib/`フォルダが同じディレクトリにあること

**設定例3: スクリプトで起動**

Windows:
```json
{
  "mcpServers": {
    "drsum": {
      "command": "C:\\mb_dev\\github\\drsum-java-mcp\\scripts\\start-server.bat"
    }
  }
}
```

Unix/Linux/macOS:
```json
{
  "mcpServers": {
    "drsum": {
      "command": "/path/to/drsum-java-mcp/scripts/start-server.sh"
    }
  }
}
```

#### 設定後の確認

1. Claude Desktopを再起動
2. 新しいチャットを開始
3. MCPツールとして "summarize" が利用可能になります

#### 他のMCPクライアントでの使用

他のMCPクライアント（VS Code拡張機能、カスタムクライアントなど）でも同様の設定が可能です。基本的には：
- `command`: 実行するコマンド
- `args`: コマンドの引数
- `env`: 環境変数（オプション）

を指定します。

### 利用可能なツール

#### `configure_connection`
Dr.Sumサーバーへの接続を確立します。

**パラメータ:**
- `host` (必須): Dr.Sumサーバーのホスト名またはIPアドレス
- `port` (必須): Dr.Sumサーバーのポート番号
- `username` (必須): 認証用ユーザー名
- `password` (オプション): 認証用パスワード（空文字列可）
- `database` (必須): 接続するデータベース名

**例:**
```json
{
  "tool": "configure_connection",
  "arguments": {
    "host": "localhost",
    "port": 6001,
    "username": "Administrator",
    "password": "",
    "database": "SALES"
  }
}
```

#### `get_metadata`
テーブルのメタ情報とサンプルデータを取得します。

**パラメータ:**
- `table_name` (必須): テーブル名
- `sample_rows` (オプション): サンプルデータの行数（デフォルト: 3）

**レスポンス例:**
```json
{
  "table": "受注ビュー",
  "columns": [
    {
      "name": "年",
      "display_name": "年",
      "type": 2,
      "type_name": "INTEGER",
      "nullable": false,
      "precision": 10,
      "scale": 0
    },
    {
      "name": "価格",
      "display_name": "価格",
      "type": 3,
      "type_name": "DECIMAL",
      "nullable": true,
      "precision": 18,
      "scale": 2
    }
  ],
  "sample_data": [
    ["2006", "150000.00"],
    ["2007", "280000.00"],
    ["2008", "320000.00"]
  ]
}
```

#### `execute_query`
SQLクエリを実行し、結果を取得します。

**パラメータ:**
- `sql_query` (必須): 実行するSQLクエリ

**レスポンス例:**
```json
{
  "columns": [
    {"name": "年", "display_name": "年", "type": 2},
    {"name": "SUM(価格)", "display_name": "SUM(価格)", "type": 3}
  ],
  "rows": [
    ["2006", "150000.00"],
    ["2007", "280000.00"],
    ["2008", "320000.00"]
  ],
  "row_count": 3
}
```

#### `disconnect`
Dr.Sumサーバーとの接続を切断します。

**パラメータ:** なし

#### `summarize`（サンプル機能）
テキスト内容を要約します。

**パラメータ:**
- `text` (必須): 要約するテキスト内容
- `max_sentences` (オプション): 要約の最大文数（デフォルト: 3）

## プロジェクト構造

```
src/
├── main/
│   ├── java/
│   │   └── com/example/drsum/
│   │       └── DrSumMcpServer.java          # メインサーバークラス
│   └── resources/
│       └── logback.xml                       # ログ設定
└── test/
    └── java/
        └── com/example/drsum/
            └── DrSumMcpServerTest.java       # テストクラス
```

## 開発

### ログ

アプリケーションは以下の場所にログを出力します：
- コンソール出力（開発時）
- `logs/drsum-mcp-server.log`（本番環境）

### 拡張

新しい機能を追加するには：

1. 新しいツールの定義を`createXXXTool()`メソッドで作成
2. ツール処理ロジックを`handleXXXRequest()`メソッドで実装
3. メインクラスでツール仕様を登録

## MCP Java SDKについて

このプロジェクトで使用されているMCP Java SDKの詳細については、以下を参照してください：

- [MCP Java SDK GitHub](https://github.com/modelcontextprotocol/java-sdk)
- [Model Context Protocol 仕様](https://modelcontextprotocol.org/docs/concepts/architecture)
- [MCP Java SDK ドキュメント](https://modelcontextprotocol.io/sdk/java/mcp-overview)

## ライセンス

このプロジェクトはMITライセンスの下で公開されています。

## 貢献

プロジェクトへの貢献を歓迎します。プルリクエストやIssueの作成をお願いします。