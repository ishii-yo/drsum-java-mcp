# DrSum MCPサーバーの実装

## やりたい事

- Dr.Sumサーバーに接続して、LLMでデータ分析を行う

## 実装ステータス

- 📋 計画完了
- 🔧 実装準備完了（DrSumEA.jar API確認済み）
- ⏳ 実装開始待ち

## データ分析のために要求される機能

- メタ情報の取得
- メタ情報には、サンプルデータが付与される
- サンプルデータはデフォルト3行。定数でコード上変更可能
- 分析のためのクエリーの実行
- 接続情報の保持
- 以下の接続情報は秘匿される
  - host名：文字列
  - ポート番号：文字列
  - ユーザー名：文字列
  - パスワード：文字列。空文字も可能
  - データベース名：文字列

## 実装上の注意

- 本プロジェクトはサンプル目的なので、できるだけ最小限の実装を心掛ける。
- コードはDrSumMcpServer.java内で完結するようシンプルにする
- DrSumへの接続についてはDrSumEA.jarを使用する
- Unit testを書くこと
- SOLID原則に従い、変更容易性、テスト容易性に考慮して実装する

## 実装計画及びTODO

### 【現状把握】

#### ✅ 完了しているもの
- MCPサーバーの基本構造（DrSumMcpServer.java）
- Mavenプロジェクトのセットアップ
- MCP Java SDK（v0.14.1）の統合
- サンプル要約ツール（summarize）の実装
- 基本的なUnit testの雛形
- DrSumEA.jarのローカルMavenリポジトリへのインストール手順確認

#### ❌ 未実装のもの
- Dr.Sum接続機能の実装
- メタ情報取得ツールの実装
- サンプルデータの取得機能
- クエリー実行ツールの実装
- 接続情報の保持・管理機能
- 接続情報の秘匿化
- SOLID原則に基づいたコードのリファクタリング
- DrSumEA.jarを使用した実装
- 実際の機能に対応したUnit test

---

### 【要件分析】

#### 機能要件
1. **Dr.Sum接続管理**
   - 接続情報：host、port、username、password、database
   - 接続の確立と維持
   - 接続情報の秘匿化

2. **メタ情報取得ツール（get_metadata）**
   - テーブル/ビューのメタ情報取得
   - サンプルデータの付与（デフォルト3行、定数で変更可能）
   - 出力形式：テーブル構造 + サンプルデータ

3. **クエリー実行ツール（execute_query）**
   - SQLクエリの実行
   - 結果セットの取得と整形
   - エラーハンドリング

#### 非機能要件
- コードはDrSumMcpServer.java内で完結（最小限実装）
- SOLID原則に従う（変更容易性、テスト容易性）
- Unit testを実装
- DrSumEA.jarを使用

---

### 【技術設計】

#### クラス構造（SOLID原則準拠）

```
DrSumMcpServer.java
├── DrSumMcpServer (メインクラス)
├── DrSumConnection (接続管理 - Single Responsibility)
│   ├── ConnectionConfig (接続情報保持)
│   └── connect() / disconnect() / isConnected()
├── DrSumMetadataService (メタ情報取得 - Single Responsibility)
│   ├── getTableMetadata()
│   └── getSampleData()
└── DrSumQueryService (クエリー実行 - Single Responsibility)
    └── executeQuery()
```

#### MCPツール定義

1. **configure_connection**
   - パラメータ：host, port, username, password, database
   - 機能：接続情報を設定・接続確立
   - 戻り値：接続成功/失敗メッセージ

2. **get_metadata**
   - パラメータ：table_name, sample_rows (optional, default=3)
   - 機能：テーブルメタ情報+サンプルデータ取得
   - 戻り値：メタ情報とサンプルデータ（JSON形式）

3. **execute_query**
   - パラメータ：sql_query
   - 機能：SQLクエリ実行
   - 戻り値：実行結果（JSON形式）

4. **disconnect**
   - パラメータ：なし
   - 機能：Dr.Sum接続の切断
   - 戻り値：切断成功メッセージ

---

### 【実装TODO】

#### Phase 1: 基盤整備（リファクタリング）
- [ ] 1.1 現在のDrSumMcpServerクラスをSOLID原則に基づきリファクタリング
  - [ ] 既存のsummarizeツールを残したまま、新規クラス構造を追加
  - [ ] テスト容易性を考慮した設計

#### Phase 2: Dr.Sum接続機能実装
- [ ] 2.1 ConnectionConfigクラスの実装
  - [ ] フィールド：host, port, username, password, database
  - [ ] バリデーション機能
  - [ ] パスワードの秘匿化（ログ出力時）
  
- [ ] 2.2 DrSumConnectionクラスの実装
  - [ ] DrSumEA.jarを使用した接続機能
  - [ ] connect()メソッド
  - [ ] disconnect()メソッド
  - [ ] isConnected()メソッド
  - [ ] 接続プーリングは不要（シンプルに）
  
- [ ] 2.3 configure_connectionツールの実装
  - [ ] ツール定義追加
  - [ ] リクエストハンドラー実装
  - [ ] エラーハンドリング

- [ ] 2.4 disconnectツールの実装
  - [ ] ツール定義追加
  - [ ] リクエストハンドラー実装

#### Phase 3: メタ情報取得機能実装
- [ ] 3.1 DrSumMetadataServiceクラスの実装
  - [ ] getTableMetadata()メソッド
  - [ ] getSampleData()メソッド（デフォルト3行）
  - [ ] SAMPLE_ROWS定数定義
  
- [ ] 3.2 get_metadataツールの実装
  - [ ] ツール定義追加（table_name, sample_rows）
  - [ ] リクエストハンドラー実装
  - [ ] メタ情報とサンプルデータの整形（JSON）

#### Phase 4: クエリー実行機能実装
- [ ] 4.1 DrSumQueryServiceクラスの実装
  - [ ] executeQuery()メソッド
  - [ ] 結果セットの整形
  - [ ] SQLインジェクション対策検討
  
- [ ] 4.2 execute_queryツールの実装
  - [ ] ツール定義追加（sql_query）
  - [ ] リクエストハンドラー実装
  - [ ] エラーハンドリング

#### Phase 5: テスト実装
- [ ] 5.1 DrSumConnectionのUnit test
  - [ ] 接続成功/失敗のテスト
  - [ ] 接続状態管理のテスト
  - [ ] モックを使用したテスト
  
- [ ] 5.2 DrSumMetadataServiceのUnit test
  - [ ] メタ情報取得のテスト
  - [ ] サンプルデータ取得のテスト
  - [ ] エッジケースのテスト
  
- [ ] 5.3 DrSumQueryServiceのUnit test
  - [ ] クエリ実行成功のテスト
  - [ ] エラーハンドリングのテスト
  
- [ ] 5.4 統合テスト
  - [ ] MCPツール全体の動作確認
  - [ ] 実際のDr.Sum環境での動作確認（可能であれば）

#### Phase 6: ドキュメント更新
- [ ] 6.1 README.mdの更新
  - [ ] 新機能の使い方追加
  - [ ] 接続設定の例
  - [ ] 利用可能なツール一覧更新
  
- [ ] 6.2 CONFIGURATION.mdの作成
  - [ ] Dr.Sum接続設定の詳細
  - [ ] トラブルシューティング

---

### 【実装の優先順位】

1. **最優先**: Phase 2（Dr.Sum接続機能）
   - 接続できなければ他の機能も動作しない
   
2. **次優先**: Phase 3（メタ情報取得）
   - データ分析の基盤となる機能
   
3. **通常**: Phase 4（クエリー実行）
   - 実際の分析機能
   
4. **重要**: Phase 5（テスト）
   - 品質保証のため並行実施

5. **最後**: Phase 6（ドキュメント）
   - 完成後にまとめて更新

---

### 【技術的な考慮事項】

#### DrSumEA.jarの使用方法（サンプルコードより）

##### 基本的な接続パターン（DBI - Database Interface）
```java
// 1. 接続の確立
DWDbiConnection connection = new DWDbiConnection(host, port, username, password);

// 2. データベースを開く
connection.openDatabase(databaseName);

// 3. カーソルを取得してクエリー実行
DWDbiCursor cursor = connection.cursor();
cursor.execute("SELECT * FROM テーブル名");

// 4. 結果取得
Vector<Vector<String>> records = cursor.fetchmany(10);  // 10件取得

// 5. スキーマ情報取得
DWColumnInfo[] schema = cursor.m_oDescription;  // execute後に利用可能

// 6. クリーンアップ
cursor.close();
connection.close();
```

##### メタ情報取得のAPI
```java
// データベース一覧
DWDatabaseInfo[] dbList = connection.getDatabaseList();

// テーブル一覧
DWTableInfo[] tableList = connection.getTableList(databaseName);

// スキーマ情報（カラム情報）
DWColumnInfo[] schema = connection.getSchema(databaseName, tableName);
// schema[i].m_sName      : カラム名
// schema[i].m_iType      : データ型
// schema[i].m_sDisplay   : 表示名
// schema[i].m_iPrecision : 精度

// ビュー情報
DWViewInfo viewInfo = connection.getViewInfo(databaseName, tableName);
```

##### 重要なクラス
- **DWDbiConnection**: DBI接続クラス（extends DWConnection）
- **DWDbiCursor**: SQLカーソルクラス
- **DWColumnInfo**: カラムメタ情報
- **DWDatabaseInfo**: データベース情報
- **DWTableInfo**: テーブル情報
- **DWViewInfo**: ビュー情報
- **DWException**: Dr.Sum例外クラス

##### 例外処理
```java
try {
    // Dr.Sum操作
} catch (DWException e) {
    // Dr.Sum固有のエラー処理
} catch (Exception e) {
    // 一般的なエラー処理
} finally {
    // リソース解放
}
```

#### サンプルデータのデフォルト行数
```java
private static final int DEFAULT_SAMPLE_ROWS = 3;
```

#### 接続情報の秘匿化
- パスワードはログに出力しない
- toString()メソッドで"****"に置換

#### エラーハンドリング
- Dr.Sum接続エラー
- SQLエラー
- ネットワークエラー
- タイムアウト

#### テスト戦略
- Mockitoを使用してDr.Sum接続をモック化
- テストデータの準備
- エッジケースの網羅

---

### 【実装イメージ】

#### ConnectionConfigクラス（内部クラス）
```java
/**
 * Dr.Sum接続設定を保持するクラス
 */
static class ConnectionConfig {
    private final String host;
    private final int port;
    private final String username;
    private final String password;  // 秘匿化対象
    private final String database;
    
    public ConnectionConfig(String host, int port, String username, 
                          String password, String database) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
    }
    
    @Override
    public String toString() {
        // パスワードは表示しない
        return String.format("ConnectionConfig{host=%s, port=%d, username=%s, password=****, database=%s}",
                           host, port, username, database);
    }
}
```

#### DrSumConnectionクラス（内部クラス）
```java
/**
 * Dr.Sum接続を管理するクラス
 */
static class DrSumConnection {
    private DWDbiConnection connection;
    private ConnectionConfig config;
    
    public void connect(ConnectionConfig config) throws DWException {
        this.config = config;
        this.connection = new DWDbiConnection(
            config.host, config.port, config.username, config.password);
        this.connection.openDatabase(config.database);
    }
    
    public boolean isConnected() {
        return connection != null && connection.m_hDatabase != 0;
    }
    
    public void disconnect() throws DWException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
    
    public DWDbiConnection getConnection() {
        return connection;
    }
}
```

#### DrSumMetadataServiceクラス（内部クラス）
```java
/**
 * Dr.Sumメタ情報取得サービス
 */
static class DrSumMetadataService {
    private static final int DEFAULT_SAMPLE_ROWS = 3;
    private final DrSumConnection dsConnection;
    
    public DrSumMetadataService(DrSumConnection connection) {
        this.dsConnection = connection;
    }
    
    public String getTableMetadata(String tableName, int sampleRows) throws DWException {
        DWDbiConnection conn = dsConnection.getConnection();
        String dbName = conn.m_sDatabase;
        
        // メタ情報取得
        DWColumnInfo[] schema = conn.getSchema(dbName, tableName);
        
        // サンプルデータ取得
        DWDbiCursor cursor = conn.cursor();
        try {
            String sql = String.format("SELECT * FROM %s LIMIT %d", tableName, sampleRows);
            cursor.execute(sql);
            Vector<Vector<String>> samples = cursor.fetchmany(sampleRows);
            
            // JSON形式で整形して返却
            return formatMetadataAsJson(schema, samples);
        } finally {
            cursor.close();
        }
    }
    
    private String formatMetadataAsJson(DWColumnInfo[] schema, Vector<Vector<String>> samples) {
        // JSON整形ロジック
        // ...
    }
}
```

#### MCPツール実装例
```java
// configure_connectionツール
private static McpSchema.Tool createConfigureConnectionTool() {
    return McpSchema.Tool.builder()
        .name("configure_connection")
        .description("Configure and establish connection to Dr.Sum server")
        .inputSchema(McpSchema.ToolInputSchema.builder()
            .properties(Map.of(
                "host", Map.of("type", "string", "description", "Dr.Sum server host"),
                "port", Map.of("type", "integer", "description", "Dr.Sum server port"),
                "username", Map.of("type", "string", "description", "Username"),
                "password", Map.of("type", "string", "description", "Password (can be empty)"),
                "database", Map.of("type", "string", "description", "Database name")
            ))
            .required(List.of("host", "port", "username", "database"))
            .build())
        .build();
}

// get_metadataツール
private static McpSchema.Tool createGetMetadataTool() {
    return McpSchema.Tool.builder()
        .name("get_metadata")
        .description("Get table metadata with sample data from Dr.Sum")
        .inputSchema(McpSchema.ToolInputSchema.builder()
            .properties(Map.of(
                "table_name", Map.of("type", "string", "description", "Table name"),
                "sample_rows", Map.of("type", "integer", "description", "Number of sample rows (default: 3)")
            ))
            .required(List.of("table_name"))
            .build())
        .build();
}
```

---

### 【次のステップ】

1. ✅ DrSumEA.jarのAPIドキュメントを確認 **← 完了！**
2. Phase 1のリファクタリングから開始
   - 現在のDrSumMcpServerクラスに内部クラスを追加
   - SOLID原則に従った設計
3. Phase 2のDr.Sum接続機能実装
   - ConnectionConfig実装
   - DrSumConnection実装
   - configure_connectionツール実装
4. 小さい単位で実装→テスト→コミットを繰り返す

**推奨実装順序:**
1. ConnectionConfig → DrSumConnection（Phase 2.1, 2.2）
2. configure_connectionツール + テスト（Phase 2.3 + Phase 5.1）
3. DrSumMetadataService（Phase 3.1）
4. get_metadataツール + テスト（Phase 3.2 + Phase 5.2）
5. DrSumQueryService（Phase 4.1）
6. execute_queryツール + テスト（Phase 4.2 + Phase 5.3）
7. ドキュメント更新（Phase 6）
  