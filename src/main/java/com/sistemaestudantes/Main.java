package com.sistemaestudantes;

import com.formdev.flatlaf.FlatDarkLaf;
import com.sistemaestudantes.api.StudyHttpServer;
import com.sistemaestudantes.scheduling.analytics.StudyAnalyticsService;
import com.sistemaestudantes.scheduling.mcp.McpClientService;
import com.sistemaestudantes.scheduling.repository.InMemoryStudySessionRepository;
import com.sistemaestudantes.scheduling.repository.StudySessionRepository;
import com.sistemaestudantes.scheduling.repository.SubjectRepository;
import com.sistemaestudantes.view.MainApplicationFrame;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;

/**
 * Ponto de entrada da aplicação Sistema de Estudantes.
 * Inicializa a API RESTful na porta 8080 para o frontend React e mantém o processo em execução.
 */
public class Main {

    private static final int API_PORT = 8080;
    private static final CountDownLatch keepAliveLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("🎓 Sistema de Estudantes — Backend & Servidor de Dados API");
        System.out.println("==========================================================");

        StudySessionRepository sessionRepository = new InMemoryStudySessionRepository();
        SubjectRepository subjectRepository = new SubjectRepository();
        McpClientService mcpClientService = new McpClientService(sessionRepository);
        StudyAnalyticsService analyticsService = new StudyAnalyticsService(sessionRepository);

        StudyHttpServer httpServer = null;

        // 1. Inicializa o Servidor HTTP RESTful para o frontend React
        try {
            httpServer = new StudyHttpServer(
                    API_PORT,
                    sessionRepository,
                    subjectRepository,
                    mcpClientService,
                    analyticsService
            );
            httpServer.start();
            System.out.println("🔗 Endpoints disponíveis:");
            System.out.println("   • GET  http://localhost:8080/api/health");
            System.out.println("   • GET  http://localhost:8080/api/subjects");
            System.out.println("   • GET  http://localhost:8080/api/sessions");
            System.out.println("   • GET  http://localhost:8080/api/metrics");
            System.out.println("   • POST http://localhost:8080/api/sessions");
            System.out.println("💻 Frontend React disponível em: http://localhost:5173");
            System.out.println("==========================================================");
            System.out.println("ℹ️  Pressione Ctrl+C para encerrar o servidor.");
        } catch (Exception e) {
            System.err.println("Erro ao iniciar servidor HTTP REST: " + e.getMessage());
        }

        final StudyHttpServer finalServer = httpServer;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nEncerrando servidor backend...");
            if (finalServer != null) {
                finalServer.stop();
            }
            keepAliveLatch.countDown();
        }));

        // 2. Se houver suporte a ambiente gráfico, abre a janela auxiliar opcional
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                FlatDarkLaf.setup();
            } catch (Exception e) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }
            }

            SwingUtilities.invokeLater(() -> {
                try {
                    MainApplicationFrame frame = new MainApplicationFrame();
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    frame.setVisible(true);
                } catch (Exception e) {
                    System.out.println("Interface gráfica Desktop opcional não exibida: " + e.getMessage());
                }
            });
        }

        // 3. Mantém a thread principal ativa para o servidor HTTP não morrer
        try {
            keepAliveLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
