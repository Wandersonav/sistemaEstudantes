package com.sistemaestudantes.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Gerenciador central de configuração e conexão com o banco de dados relacional PostgreSQL.
 * Utiliza o pool de conexões de alta performance HikariCP com credenciais configuráveis.
 */
public class DatabaseConfig {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5432;
    private static final String DEFAULT_DB = "Estudantes";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASS = "1234";

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    private HikariDataSource dataSource;
    private boolean initialized = false;

    public DatabaseConfig() {
        this.host = getEnvOrProperty("DB_HOST", "db.host", DEFAULT_HOST);
        this.port = Integer.parseInt(getEnvOrProperty("DB_PORT", "db.port", String.valueOf(DEFAULT_PORT)));
        this.database = getEnvOrProperty("DB_NAME", "db.name", DEFAULT_DB);
        this.username = getEnvOrProperty("DB_USER", "db.user", DEFAULT_USER);
        this.password = getEnvOrProperty("DB_PASSWORD", "db.password", DEFAULT_PASS);
    }

    public DatabaseConfig(String host, int port, String database, String username, String password) {
        this.host = host != null ? host : DEFAULT_HOST;
        this.port = port > 0 ? port : DEFAULT_PORT;
        this.database = database != null ? database : DEFAULT_DB;
        this.username = username != null ? username : DEFAULT_USER;
        this.password = password != null ? password : DEFAULT_PASS;
    }

    /**
     * Inicializa o pool HikariCP e valida a conexão com o PostgreSQL.
     */
    public synchronized boolean init() {
        if (initialized && dataSource != null && !dataSource.isClosed()) {
            return true;
        }

        try {
            // Tenta conectar no banco alvo (ex: Estudantes); se falhar, tenta fallback no banco 'postgres'
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?ApplicationName=SistemaEstudantes", host, port, database);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(3000); // 3 segundos
            config.setValidationTimeout(2000);
            config.setIdleTimeout(60000);
            config.setMaxLifetime(600000);
            config.setPoolName("HikariPool-SistemaEstudantes");

            // Configurações recomendadas para PostgreSQL
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            this.dataSource = new HikariDataSource(config);

            // Testa a conexão
            try (Connection conn = dataSource.getConnection()) {
                System.out.println("🐘 [PostgreSQL] Conexão estabelecida com sucesso: " + jdbcUrl + " (Usuário: " + username + ")");
            }

            // Executa script DDL inicial
            runSchemaInit();
            this.initialized = true;
            return true;
        } catch (Exception e) {
            System.err.println("⚠️ [PostgreSQL] Aviso ao conectar em " + database + ": " + e.getMessage());

            // Tentativa de fallback para o banco padrão 'postgres' caso 'Estudantes' não exista
            if (!"postgres".equalsIgnoreCase(database)) {
                try {
                    String fallbackUrl = String.format("jdbc:postgresql://%s:%d/postgres?ApplicationName=SistemaEstudantes", host, port);
                    System.out.println("🔄 [PostgreSQL] Tentando fallback para o banco de dados 'postgres'...");

                    HikariConfig fallbackConfig = new HikariConfig();
                    fallbackConfig.setJdbcUrl(fallbackUrl);
                    fallbackConfig.setUsername(username);
                    fallbackConfig.setPassword(password);
                    fallbackConfig.setMaximumPoolSize(5);
                    fallbackConfig.setConnectionTimeout(3000);

                    this.dataSource = new HikariDataSource(fallbackConfig);
                    try (Connection conn = dataSource.getConnection()) {
                        System.out.println("🐘 [PostgreSQL] Conectado ao banco 'postgres'!");
                    }
                    runSchemaInit();
                    this.initialized = true;
                    return true;
                } catch (Exception ex) {
                    System.err.println("❌ [PostgreSQL] Fallback também falhou: " + ex.getMessage());
                }
            }
            return false;
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Pool de conexões não inicializado ou fechado.");
        }
        return dataSource.getConnection();
    }

    public boolean isConnected() {
        if (dataSource == null || dataSource.isClosed()) return false;
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("🔌 [PostgreSQL] Pool de conexões encerrado.");
        }
    }

    /**
     * Executa a criação das tabelas e índices relacionais se não existirem.
     */
    private void runSchemaInit() {
        try (InputStream is = getClass().getResourceAsStream("/db/schema.sql")) {
            if (is == null) {
                System.out.println("ℹ️ Script /db/schema.sql não encontrado no classpath, criando schema via código.");
                executeDefaultDDL();
                return;
            }

            String sql = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                System.out.println("✅ [PostgreSQL] Esquema relacional e sementes aplicados com sucesso.");
            }
        } catch (Exception e) {
            System.err.println("⚠️ [PostgreSQL] Erro ao aplicar script schema.sql: " + e.getMessage());
            executeDefaultDDL();
        }
    }

    private void executeDefaultDDL() {
        String ddl = """
            CREATE TABLE IF NOT EXISTS disciplinas (
                id VARCHAR(50) PRIMARY KEY,
                nome VARCHAR(255) NOT NULL,
                codigo VARCHAR(50) NOT NULL,
                cor_hex VARCHAR(20) NOT NULL DEFAULT '#3A7D8C',
                criado_em TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS sessoes_estudo (
                id VARCHAR(50) PRIMARY KEY,
                disciplina_id VARCHAR(50) NOT NULL,
                disciplina_nome VARCHAR(255) NOT NULL,
                topico VARCHAR(255) NOT NULL,
                data_inicio TIMESTAMP NOT NULL,
                data_fim TIMESTAMP NOT NULL,
                duracao_minutos INTEGER NOT NULL DEFAULT 25,
                tipo_atividade VARCHAR(50) NOT NULL DEFAULT 'TEORIA',
                status VARCHAR(50) NOT NULL DEFAULT 'PLANEJADA',
                status_sincronizacao VARCHAR(50) NOT NULL DEFAULT 'NAO_SINCRONIZADO',
                evento_externo_id VARCHAR(255),
                mensagem_erro_sincronizacao TEXT,
                observacoes TEXT,
                criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_sessoes_estudo_disciplina FOREIGN KEY (disciplina_id) REFERENCES disciplinas(id) ON UPDATE CASCADE ON DELETE RESTRICT
            );

            CREATE INDEX IF NOT EXISTS idx_sessoes_estudo_disciplina_id ON sessoes_estudo(disciplina_id);
            CREATE INDEX IF NOT EXISTS idx_sessoes_estudo_data_inicio ON sessoes_estudo(data_inicio);
            CREATE INDEX IF NOT EXISTS idx_sessoes_estudo_status ON sessoes_estudo(status);
        """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
            System.out.println("✅ [PostgreSQL] Tabelas relacionais disciplinas e sessoes_estudo verificadas.");
        } catch (Exception e) {
            System.err.println("❌ [PostgreSQL] Erro ao executar DDL padrão: " + e.getMessage());
        }
    }

    private String getEnvOrProperty(String envVar, String sysProp, String defaultValue) {
        String val = System.getenv(envVar);
        if (val != null && !val.isBlank()) return val;
        val = System.getProperty(sysProp);
        if (val != null && !val.isBlank()) return val;
        return defaultValue;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabase() { return database; }
    public String getUsername() { return username; }
}
