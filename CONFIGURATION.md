# DrSum Java MCP Server Configuration Guide

## 接続方式について

DrSum MCP Serverは**都度接続方式**を採用しています。

### 都度接続方式とは

- ツール呼び出し時に環境変数から接続情報を読み取り
- Dr.Sumに接続してデータ取得
- 処理完了後に自動切断

### メリット

1. **シンプル**: 接続状態の管理が不要
2. **安全**: リソースリークのリスクがない
3. **自動**: ユーザーが接続を意識する必要なし
4. **セキュア**: 認証情報は環境変数で安全に管理

### デメリット

- 各ツール呼び出しで接続オーバーヘッドが発生（通常1〜2秒程度）
- 頻繁なクエリ実行時はパフォーマンス低下の可能性

### 使用イメージ

```
ユーザー: 「受注ビューのデータを分析して」
    ↓
AI: get_metadataツールを呼び出し
    ↓
1. 環境変数から接続情報読み取り
2. Dr.Sum接続
3. データ取得
4. 切断
    ↓
AI: 分析結果を返答
```

## 必須環境変数

Dr.Sum接続には以下の環境変数が必要です：

| 環境変数 | 説明 | 必須 | 例 |
|---------|------|------|-----|
| `DRSUM_HOST` | Dr.Sumサーバーのホスト名またはIPアドレス | ✅ | `localhost`, `192.168.1.100` |
| `DRSUM_PORT` | Dr.Sumサーバーのポート番号 | ✅ | `6001` |
| `DRSUM_USERNAME` | 認証用ユーザー名 | ✅ | `Administrator` |
| `DRSUM_PASSWORD` | 認証用パスワード | ⚠️ | 空文字列可 |
| `DRSUM_DATABASE` | 接続するデータベース名 | ✅ | `SALES` |

## MCP Client Configuration

### Claude Desktop設定例（推奨）

**設定ファイル:** `%APPDATA%\Claude\claude_desktop_config.json` (Windows)

```json
{
  "mcpServers": {
    "drsum": {
      "command": "java",
      "args": [
        "-jar", 
        "C:\\mb_dev\\github\\drsum-java-mcp\\target\\drsum-java-mcp-1.0.0-SNAPSHOT-fat.jar"
      ],
      "env": {
        "DRSUM_HOST": "localhost",
        "DRSUM_PORT": "6001",
        "DRSUM_USERNAME": "Administrator",
        "DRSUM_PASSWORD": "",
        "DRSUM_DATABASE": "SALES"
      }
    }
  }
}
```

### 開発環境設定例

```json
{
  "mcpServers": {
    "drsum-dev": {
      "command": "java",
      "args": ["-jar", "path/to/drsum-java-mcp-fat.jar"],
      "env": {
        "DRSUM_HOST": "localhost",
        "DRSUM_PORT": "6001",
        "DRSUM_USERNAME": "dev_user",
        "DRSUM_PASSWORD": "dev_password",
        "DRSUM_DATABASE": "DEV_DB"
      }
    }
  }
}
```

### 本番環境設定例

```json
{
  "mcpServers": {
    "drsum-prod": {
      "command": "java",
      "args": ["-jar", "path/to/drsum-java-mcp-fat.jar"],
      "env": {
        "DRSUM_HOST": "prod-server.company.com",
        "DRSUM_PORT": "6001",
        "DRSUM_USERNAME": "prod_user",
        "DRSUM_PASSWORD": "secure_password",
        "DRSUM_DATABASE": "PROD_DB"
      }
    }
  }
}
```

### 複数環境の同時設定

```json
{
  "mcpServers": {
    "drsum-dev": {
      "command": "java",
      "args": ["-jar", "path/to/drsum-java-mcp-fat.jar"],
      "env": {
        "DRSUM_HOST": "localhost",
        "DRSUM_PORT": "6001",
        "DRSUM_USERNAME": "Administrator",
        "DRSUM_PASSWORD": "",
        "DRSUM_DATABASE": "DEV_SALES"
      }
    },
    "drsum-prod": {
      "command": "java",
      "args": ["-jar", "path/to/drsum-java-mcp-fat.jar"],
      "env": {
        "DRSUM_HOST": "prod-server",
        "DRSUM_PORT": "6001",
        "DRSUM_USERNAME": "prod_user",
        "DRSUM_PASSWORD": "password",
        "DRSUM_DATABASE": "PROD_SALES"
      }
    }
  }
}
```

## その他の環境変数（オプション）

| 環境変数 | 説明 | デフォルト |
|---------|------|-----------|
| `LOG_LEVEL` | ログレベル (DEBUG, INFO, WARN, ERROR) | INFO |
| `JAVA_OPTS` | 追加のJVMオプション | - |

## トラブルシューティング

### 環境変数未設定エラー

**エラーメッセージ:**
```
Error: Environment variable DRSUM_HOST is not set. Please configure Dr.Sum connection information in MCP settings.
```

**解決方法:**
1. MCP設定ファイルに`env`セクションがあることを確認
2. 必須環境変数(`DRSUM_HOST`, `DRSUM_PORT`, `DRSUM_USERNAME`, `DRSUM_DATABASE`)が全て設定されていることを確認
3. MCPクライアント（Claude Desktopなど）を再起動

### 接続エラー

**エラーメッセージ:**
```
Error: Failed to connect to Dr.Sum: Connection refused
```

**解決方法:**
1. Dr.Sumサーバーが起動していることを確認
2. ホスト名・ポート番号が正しいことを確認
3. ファイアウォール設定を確認
4. ネットワーク接続を確認

### 認証エラー

**エラーメッセージ:**
```
Error: Failed to connect to Dr.Sum: Authentication failed
```

**解決方法:**
1. ユーザー名・パスワードが正しいことを確認
2. Dr.Sum側でユーザーアカウントが有効であることを確認

### データベース接続エラー

**エラーメッセージ:**
```
Error: Failed to connect to Dr.Sum: Database 'SALES' not found
```

**解決方法:**
1. データベース名が正しいことを確認（大文字小文字の区別に注意）
2. Dr.Sum側でデータベースが存在することを確認
3. ユーザーが該当データベースへのアクセス権限を持っていることを確認

### ポート番号の形式エラー

**エラーメッセージ:**
```
Error: Environment variable DRSUM_PORT must be a valid integer. Got: abc
```

**解決方法:**
- `DRSUM_PORT`の値を数値（例: `"6001"`）に修正

### パフォーマンスの問題

**症状:** ツール呼び出しが遅い

**原因:** 都度接続方式による接続オーバーヘッド

**対策:**
1. ネットワーク遅延を確認（Dr.Sumサーバーが遠隔地にある場合）
2. 一度のツール呼び出しで必要な情報を全て取得するようにプロンプトを工夫
3. サンプルデータの行数を減らす（`sample_rows`パラメータ調整）

## セキュリティベストプラクティス

### 認証情報の管理

1. **パスワードの保護**
   - 環境変数は設定ファイルに平文で記載される
   - 設定ファイルのアクセス権限を適切に設定
   - 本番環境では強力なパスワードを使用

2. **アクセス制御**
   - Dr.Sumユーザーに最小限の権限のみ付与
   - 読み取り専用アクセスを推奨

3. **ログ管理**
   - パスワードはログに出力されない（自動マスキング）
   - ログファイルのアクセス権限を適切に設定

### ネットワークセキュリティ

1. Dr.Sumサーバーへのアクセスを制限
2. 必要に応じてVPNやSSHトンネルを使用
3. 本番環境では暗号化通信を推奨

## ツール使用例

### メタデータ取得

AIに以下のように依頼すると、自動的に`get_metadata`ツールが呼び出されます：

```
ユーザー: 「受注ビューのテーブル構造を教えて」

AI: get_metadataツールを呼び出し
    → 環境変数から接続
    → メタデータ取得
    → 切断
    → 結果をユーザーに説明
```

### SQLクエリ実行

```
ユーザー: 「2023年の売上合計を教えて」

AI: execute_queryツールを呼び出し
    → 環境変数から接続
    → SELECT SUM(売上) FROM 受注ビュー WHERE 年=2023 を実行
    → 切断
    → 結果をユーザーに説明
```

### 複数ツールの連続使用

```
ユーザー: 「受注ビューを分析して、トップ3の顧客を教えて」

AI: 
1. get_metadataで構造確認
   → 接続 → データ取得 → 切断
2. execute_queryで集計
   → 接続 → クエリ実行 → 切断
3. 結果をユーザーに説明
```

各ツール呼び出しで独立した接続・切断が行われます。
