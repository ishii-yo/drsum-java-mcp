# DrSum Java MCP Server

このプロジェクトは、[Model Context Protocol (MCP)](https://modelcontextprotocol.org/) Java SDKを使用して構築された、**Dr.Sumデータベース分析機能**を提供するMCPサーバーです。

## 概要

DrSum MCP Serverは、AIアシスタント（Claude Desktopなど）やアプリケーションに、Dr.Sumデータベースへの接続・分析機能を提供します。LLMがDr.Sumのデータを直接分析できるようになり、自然言語でのデータクエリや分析が可能になります。

## 主な機能

### Dr.Sum分析機能
- **環境変数ベースの接続管理**: MCPクライアント設定で環境変数を設定
- **都度接続方式**: ツール呼び出しごとに接続・切断を行い、リソースリークを防止
- **メタ情報取得**: テーブル構造とサンプルデータの取得（デフォルト3行、設定可能）
- **SQLクエリー実行**: 任意のSQLクエリを実行し、結果をJSON形式で取得
- **自動接続管理**: ユーザーが接続を意識する必要なし

### その他の機能
- **MCP標準準拠**: Model Context Protocolの仕様に準拠
- **STDIO通信**: 標準入出力を使用した通信
- **セキュリティ**: パスワードの秘匿化、環境変数での安全な認証情報管理
- **シンプルな設計**: 接続状態を保持しない、状態管理不要

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
.\mvnw.cmd install:install-file "-Dfile=path\to\DrSumEA.jar" "-DgroupId=jp.co.dw_sapporo" "-DartifactId=DrSumEA" "-Dversion=5.7.0" "-Dpackaging=jar"
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
        "JAVA_HOME": "C:\\java\\jdk-17",
        "DRSUM_HOST": "localhost",
        "DRSUM_PORT": "6001",
        "DRSUM_USERNAME": "your-id",
        "DRSUM_PASSWORD": "your-password",
        "DRSUM_DATABASE": "BUG_DB",
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
        "JAVA_HOME": "/path/to/jdk-17",
        "DRSUM_HOST": "localhost",
        "DRSUM_PORT": "6001",
        "DRSUM_USERNAME": "your-id",
        "DRSUM_PASSWORD": "your-password",
        "DRSUM_DATABASE": "BUG_DB",
      }
    }
  }
}
```

**設定例2A: Fat JARで起動（推奨・単一ファイル） + 環境変数設定**

```json
{
  "mcpServers": {
    "drsum": {
      "command": "java",
      "args": [
        "-Dfile.encoding=UTF-8",
        "-jar",
        "C:\\mb_dev\\github\\drsum-java-mcp\\target\\drsum-java-mcp-1.0.0-SNAPSHOT-fat.jar"
      ],
      "env": {
        "DRSUM_HOST": "localhost",
        "DRSUM_PORT": "6001",
        "DRSUM_USERNAME": "your-id",
        "DRSUM_PASSWORD": "your-password",
        "DRSUM_DATABASE": "BUG_DB",
      }
    }
  }
}
```

**重要**: 
- 環境変数(`env`)の設定は必須です。Dr.Sum接続情報はここで設定します。
- **日本語などのマルチバイト文字を使用する場合は、`"-Dfile.encoding=UTF-8"`を`args`の先頭に追加してください**（Windows環境では特に重要）。

**重要**: 環境変数(`env`)の設定は必須です。Dr.Sum接続情報はここで設定します。

**設定例2B: 通常JARで起動（依存関係分離）**

```json
{
  "mcpServers": {
    "drsum": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\mb_dev\\github\\drsum-java-mcp\\target\\drsum-java-mcp-1.0.0-SNAPSHOT.jar"
      ],
      "env": {
        "DRSUM_HOST": "localhost",
        "DRSUM_PORT": "6001",
        "DRSUM_USERNAME": "your-id",
        "DRSUM_PASSWORD": "your-password",
        "DRSUM_DATABASE": "BUG_DB",
      }
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
      "command": "C:\\mb_dev\\github\\drsum-java-mcp\\scripts\\start-server.bat",
      "env": {
        "DRSUM_HOST": "localhost",
        "DRSUM_PORT": "6001",
        "DRSUM_USERNAME": "your-id",
        "DRSUM_PASSWORD": "your-password",
        "DRSUM_DATABASE": "BUG_DB",
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
      "command": "/path/to/drsum-java-mcp/scripts/start-server.sh",
      "env": {
        "DRSUM_HOST": "localhost",
        "DRSUM_PORT": "6001",
        "DRSUM_USERNAME": "your-id",
        "DRSUM_PASSWORD": "your-password",
        "DRSUM_DATABASE": "BUG_DB",
      }
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
- `env`: 環境変数

を指定します。

### 接続情報の設定

Dr.Sumへの接続情報は、MCPクライアント設定の環境変数(`env`)で設定します。

**必須環境変数:**
- `DRSUM_HOST`: Dr.Sumサーバーのホスト名またはIPアドレス
- `DRSUM_PORT`: Dr.Sumサーバーのポート番号
- `DRSUM_USERNAME`: 認証用ユーザー名
- `DRSUM_PASSWORD`: 認証用パスワード
- `DRSUM_DATABASE`: 接続するデータベース名

**オプション環境変数:**
- `DRSUM_SCOPES`: テーブルスコープ定義（JSON形式、オプション）

**テーブルスコープについて:**

特定の分析用途に応じて、対象とするテーブル・ビューを限定できます。スコープを定義することで、LLMが適切なテーブルのみを対象に分析を行えます。

スコープ定義の形式（JSON）:
```json
{
  "scope_name": ["table1", "table2", "view1"],
  "another_scope": ["table3", "table4"]
}
```

**設定例:**

```json
{
  "mcpServers": {
    "drsum": {
      "command": "java",
      "args": ["-Dfile.encoding=UTF-8", "-jar", "path/to/drsum-java-mcp-fat.jar"],
      "env": {
        "DRSUM_HOST": "localhost",
        "DRSUM_PORT": "6001",
        "DRSUM_USERNAME": "your-id",
        "DRSUM_PASSWORD": "your-password",
        "DRSUM_DATABASE": "BUG_DB",
        "DRSUM_SCOPES": "{\"bug_analysis\": [\"bug_reports\", \"error_logs\", \"v_bug_trends\"], \"sales_analysis\": [\"orders\", \"customers\", \"v_sales_summary\"]}"
      }
    }
  }
}
```

### 利用可能なツール

**接続方式について:**
- 接続情報は環境変数から読み取られます
- 各ツール呼び出し時に自動的に接続・切断が行われます（都度接続方式）
- ユーザーが接続を意識する必要はありません

#### `list_tables`
データベース内の全てのテーブルとビューの一覧を取得します。

**パラメータ:**
- `scope` (オプション): スコープ名。指定した場合、そのスコープに定義されたテーブル・ビューのみを返します

**レスポンス例:**
```json
{
  "database": "SALES",
  "tables": [
    "受注",
    "顧客",
    "商品"
  ],
  "views": [
    "受注ビュー",
    "売上サマリー"
  ],
  "total_count": 5
}
```

**使用例:**
```
ユーザー: 「このデータベースにどんなテーブルがありますか？」
AI: list_tablesツールを呼び出してテーブル一覧を取得し、説明します

ユーザー: 「bug_analysisスコープで不具合の傾向を分析して」
AI: list_tables(scope="bug_analysis")を呼び出し、スコープに定義されたテーブルのみを対象に分析します
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

### 都度接続方式のメリット

1. **シンプル**: 接続状態を管理する必要がない
2. **安全**: リソースリークのリスクなし
3. **自動**: ユーザーが接続/切断を意識不要
4. **セキュア**: 認証情報は環境変数で管理

### 都度接続方式の動作

```
ユーザー: 「受注ビューのデータを分析して」
    ↓
AI: get_metadata ツールを呼び出し
    ↓
1. 環境変数から接続情報を読み取り
2. Dr.Sumに接続
3. メタデータとサンプルデータを取得
4. Dr.Sumから切断
    ↓
AI: データ分析結果を返答
```

## トラブルシューティング

### マルチバイト文字（日本語）の文字化け

**症状:**
- SQLクエリやテーブル名に日本語を使用すると文字化けする
- エラーメッセージに文字化けした文字列が表示される（例: "受注ビュー" が "蜿玲ｳｨ繝薙Η繝ｼ" のように表示される）

**原因:**
- Windows環境でJavaのデフォルト文字エンコーディングがMS932（Shift_JIS系）になっており、Dr.SumがUTF-8を期待しているため、文字エンコーディングの不一致が発生します。

**解決方法（推奨）:**

MCP設定ファイルのJava起動引数に `-Dfile.encoding=UTF-8` を追加します：

```json
{
  "mcpServers": {
    "drsum": {
      "command": "java",
      "args": [
        "-Dfile.encoding=UTF-8",
        "-jar",
        "path/to/drsum-java-mcp-fat.jar"
      ],
      "env": {
        "DRSUM_HOST": "localhost",
        "DRSUM_PORT": "6001",
        "DRSUM_USERNAME": "your-id",
        "DRSUM_PASSWORD": "your-password",
        "DRSUM_DATABASE": "BUG_DB",
      }
    }
  }
}
```

**クロスプラットフォーム対応:**
- この設定は **Windows、Linux、macOS 全てのプラットフォームで推奨** されます
- Linux/macOSでは通常デフォルトでUTF-8ですが、明示的に指定することで環境依存を排除できます

**検証方法:**

1. MCPクライアント（Claude Desktopなど）を再起動
2. 日本語を含むテーブル名やクエリをテスト
3. 正しく表示されることを確認

例:
```
ユーザー: 「受注ビューのデータを取得してください」
→ 正常に動作し、日本語が正しく表示されるはずです
```

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

