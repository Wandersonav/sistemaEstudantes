package com.sistemaestudantes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sistemaestudantes.scheduling.analytics.StudyAnalyticsService;
import com.sistemaestudantes.scheduling.analytics.StudyMetrics;
import com.sistemaestudantes.scheduling.domain.ActivityType;
import com.sistemaestudantes.scheduling.domain.SessionStatus;
import com.sistemaestudantes.scheduling.domain.StudySession;
import com.sistemaestudantes.scheduling.dto.StudySessionDTO;
import com.sistemaestudantes.scheduling.mcp.McpClientService;
import com.sistemaestudantes.scheduling.repository.StudySessionRepository;
import com.sistemaestudantes.scheduling.repository.SubjectRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP RESTful desacoplado embutido em Java para servir a API do Sistema de Estudantes.
 */
public class StudyHttpServer {

    private final int port;
    private final StudySessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;
    private final McpClientService mcpClientService;
    private final StudyAnalyticsService analyticsService;
    private final ObjectMapper objectMapper;
    private HttpServer server;

    public StudyHttpServer(int port,
                           StudySessionRepository sessionRepository,
                           SubjectRepository subjectRepository,
                           McpClientService mcpClientService,
                           StudyAnalyticsService analyticsService) {
        this.port = port;
        this.sessionRepository = sessionRepository;
        this.subjectRepository = subjectRepository;
        this.mcpClientService = mcpClientService;
        this.analyticsService = analyticsService;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));

        // Registra endpoints RESTful
        server.createContext("/api/subjects", this::handleSubjects);
        server.createContext("/api/sessions", this::handleSessions);
        server.createContext("/api/metrics", this::handleMetrics);
        server.createContext("/api/health", this::handleHealth);

        // Serve a interface web limpa (HTML, CSS, JS) na raiz
        server.createContext("/", this::handleStaticFiles);

        server.start();
        System.out.println("🚀 [Servidor Fullstack] Aplicação e API disponíveis em http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "UP");
        resp.put("timestamp", LocalDateTime.now().toString());
        resp.put("version", "1.0.0");
        sendJsonResponse(exchange, 200, resp);
    }

    private void handleSubjects(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 200, subjectRepository.findAll());
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleSessions(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath(); // ex: /api/sessions ou /api/sessions/{id} ou /api/sessions/{id}/sync-mcp

        String[] parts = path.split("/");

        if (parts.length == 3 && "GET".equals(method)) {
            // GET /api/sessions
            List<StudySession> sessions = sessionRepository.findAll();
            sendJsonResponse(exchange, 200, sessions);
            return;
        }

        if (parts.length == 3 && "POST".equals(method)) {
            // POST /api/sessions -> Criar nova sessão
            InputStream is = exchange.getRequestBody();
            StudySessionDTO dto = objectMapper.readValue(is, StudySessionDTO.class);

            StudySession session = new StudySession(
                    dto.getSubjectId(),
                    dto.getSubjectName(),
                    dto.getTopic(),
                    dto.getStartTime() != null ? dto.getStartTime() : LocalDateTime.now(),
                    dto.getEndTime() != null ? dto.getEndTime() : LocalDateTime.now().plusMinutes(25),
                    dto.getActivityType() != null ? dto.getActivityType() : ActivityType.TEORIA,
                    dto.getStatus() != null ? dto.getStatus() : SessionStatus.PLANEJADA,
                    dto.getNotes()
            );

            sessionRepository.save(session);

            if (dto.isSyncWithGoogleCalendar()) {
                mcpClientService.syncSessionAsync(session);
            }

            sendJsonResponse(exchange, 201, session);
            return;
        }

        if (parts.length == 4) {
            String sessionId = parts[3];
            Optional<StudySession> opt = sessionRepository.findById(sessionId);

            if (opt.isEmpty()) {
                sendJsonResponse(exchange, 404, Map.of("error", "Sessão não encontrada: " + sessionId));
                return;
            }

            StudySession session = opt.get();

            if ("PUT".equals(method)) {
                // PUT /api/sessions/{id}
                InputStream is = exchange.getRequestBody();
                StudySessionDTO dto = objectMapper.readValue(is, StudySessionDTO.class);

                if (dto.getStatus() != null) session.setStatus(dto.getStatus());
                if (dto.getTopic() != null) session.setTopic(dto.getTopic());
                if (dto.getActivityType() != null) session.setActivityType(dto.getActivityType());
                if (dto.getNotes() != null) session.setNotes(dto.getNotes());

                sessionRepository.save(session);
                sendJsonResponse(exchange, 200, session);
                return;
            }

            if ("DELETE".equals(method)) {
                // DELETE /api/sessions/{id}
                sessionRepository.deleteById(sessionId);
                sendJsonResponse(exchange, 200, Map.of("message", "Sessão removida com sucesso."));
                return;
            }
        }

        if (parts.length == 5 && "sync-mcp".equals(parts[4]) && "POST".equals(method)) {
            // POST /api/sessions/{id}/sync-mcp -> Sincronização manual MCP
            String sessionId = parts[3];
            Optional<StudySession> opt = sessionRepository.findById(sessionId);

            if (opt.isEmpty()) {
                sendJsonResponse(exchange, 404, Map.of("error", "Sessão não encontrada: " + sessionId));
                return;
            }

            StudySession session = opt.get();
            mcpClientService.syncSessionAsync(session);
            sendJsonResponse(exchange, 202, Map.of("message", "Sincronização MCP despachada com sucesso.", "session", session));
            return;
        }

        exchange.sendResponseHeaders(405, -1);
    }

    private void handleMetrics(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            StudyMetrics metrics = analyticsService.calculateMetrics();
            sendJsonResponse(exchange, 200, metrics);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Se for requisição para rota de API não encontrada
        if (path.startsWith("/api/")) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        if ("/".equals(path) || path.isBlank()) {
            path = "/index.html";
        }

        java.io.File file = new java.io.File("web" + path);
        if (!file.exists() || file.isDirectory()) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        String mime = "application/octet-stream";
        if (path.endsWith(".html")) mime = "text/html; charset=UTF-8";
        else if (path.endsWith(".css")) mime = "text/css; charset=UTF-8";
        else if (path.endsWith(".js")) mime = "application/javascript; charset=UTF-8";
        else if (path.endsWith(".svg")) mime = "image/svg+xml";
        else if (path.endsWith(".json")) mime = "application/json; charset=UTF-8";

        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        byte[] bytes = objectMapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
