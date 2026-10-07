package com.sistemaestudantes.api;

import com.sistemaestudantes.scheduling.analytics.StudyAnalyticsService;
import com.sistemaestudantes.scheduling.mcp.McpClientService;
import com.sistemaestudantes.scheduling.repository.InMemoryStudySessionRepository;
import com.sistemaestudantes.scheduling.repository.StudySessionRepository;
import com.sistemaestudantes.scheduling.repository.SubjectRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class StudyHttpServerTest {

    private static StudyHttpServer server;
    private static int port;
    private static HttpClient client;

    @BeforeAll
    static void setUp() throws IOException {
        StudySessionRepository sessionRepo = new InMemoryStudySessionRepository();
        SubjectRepository subjectRepo = new SubjectRepository();
        McpClientService mcpService = new McpClientService(sessionRepo);
        StudyAnalyticsService analyticsService = new StudyAnalyticsService(sessionRepo);

        // Usa porta de teste 8099
        server = new StudyHttpServer(8099, sessionRepo, subjectRepo, mcpService, analyticsService);
        server.start();
        port = server.getPort();
        client = HttpClient.newHttpClient();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void testHealthEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"UP\""));
    }

    @Test
    void testSubjectsEndpointContainsColorGuidelines() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/subjects"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        // Verifica as cores oficiais da SKILL: #3A7D8C, #7FB3D1, #7FA99B, etc.
        assertTrue(response.body().contains("#3A7D8C"));
        assertTrue(response.body().contains("#7FB3D1"));
    }

    @Test
    void testStaticIndexHtmlServing() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<!DOCTYPE html>"));
        assertTrue(response.body().contains("Sistema de Estudantes"));
    }

    @Test
    void testStaticStylesCssServing() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/styles.css"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        // Verifica se styles.css serve os tokens oficiais da SKILL
        assertTrue(response.body().contains("--color-primary: #3A7D8C"));
        assertTrue(response.body().contains("--color-lagoon: #9CC5DE"));
    }
}
