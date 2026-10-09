package com.sistemaestudantes;

import com.sistemaestudantes.api.StudyHttpServer;
import com.sistemaestudantes.database.DatabaseConfig;
import com.sistemaestudantes.scheduling.analytics.StudyAnalyticsService;
import com.sistemaestudantes.scheduling.mcp.McpClientService;
import com.sistemaestudantes.scheduling.repository.InMemoryStudySessionRepository;
import com.sistemaestudantes.scheduling.repository.PostgresStudySessionRepository;
import com.sistemaestudantes.scheduling.repository.StudySessionRepository;
import com.sistemaestudantes.scheduling.repository.SubjectRepository;

import java.util.concurrent.CountDownLatch;

/**
 * Ponto de entrada da aplicação Sistema de Estudantes.
 * Inicializa a API RESTful e o servidor HTTP para a interface Web (index.html).
 */
public class Main {

    private static final int DEFAULT_PORT = 8080;
    private static final CountDownLatch keepAliveLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("🎓 Sistema de Estudantes — Servidor Web & API");
        System.out.println("==========================================================");

        int configuredPort = Integer.getInteger("port", DEFAULT_PORT);

        // Inicializa o banco de dados PostgreSQL relacional (host: localhost, port: 5432, user: postgres, pass: 1234)
        DatabaseConfig dbConfig = new DatabaseConfig();
        boolean dbConnected = dbConfig.init();
        if (dbConnected) {
            System.out.println("🐘 [Banco de Dados] PostgreSQL conectado e pronto para uso.");
        } else {
            System.out.println("⚠️ [Banco de Dados] PostgreSQL indisponível. Operando em modo de resiliência com repositório local.");
        }

        StudySessionRepository sessionRepository = new PostgresStudySessionRepository(dbConfig);
        SubjectRepository subjectRepository = new SubjectRepository(dbConfig);
        McpClientService mcpClientService = new McpClientService(sessionRepository);
        StudyAnalyticsService analyticsService = new StudyAnalyticsService(sessionRepository);

        StudyHttpServer httpServer = null;

        try {
            httpServer = new StudyHttpServer(
                    configuredPort,
                    sessionRepository,
                    subjectRepository,
                    mcpClientService,
                    analyticsService,
                    dbConfig
            );
            httpServer.start();

            int port = httpServer.getPort();
            System.out.println("💻 Interface Web disponível em : http://localhost:" + port);
            System.out.println("🔗 Endpoints da API REST:");
            System.out.println("   • GET  http://localhost:" + port + "/api/health");
            System.out.println("   • GET  http://localhost:" + port + "/api/subjects");
            System.out.println("   • GET  http://localhost:" + port + "/api/sessions");
            System.out.println("   • GET  http://localhost:" + port + "/api/metrics");
            System.out.println("   • POST http://localhost:" + port + "/api/sessions");
            System.out.println("==========================================================");
            System.out.println("ℹ️  Pressione Ctrl+C para encerrar o servidor.");
        } catch (Exception e) {
            System.err.println("Erro ao iniciar servidor HTTP: " + e.getMessage());
        }

        final StudyHttpServer finalServer = httpServer;
        final DatabaseConfig finalDbConfig = dbConfig;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nEncerrando servidor backend...");
            if (finalServer != null) {
                finalServer.stop();
            }
            if (finalDbConfig != null) {
                finalDbConfig.close();
            }
            keepAliveLatch.countDown();
        }));

        try {
            keepAliveLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
