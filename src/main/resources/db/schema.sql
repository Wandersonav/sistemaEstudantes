-- ============================================================================
-- SISTEMA DE ESTUDANTES — ESQUEMA RELACIONAL POSTGRESQL
-- Banco de dados: Estudantes / postgres
-- Host: localhost | Porta: 5432 | Usuário: postgres
-- ============================================================================

-- 1. Tabela de Disciplinas / Matérias (subjects)
CREATE TABLE IF NOT EXISTS subjects (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    hex_color VARCHAR(20) NOT NULL DEFAULT '#3A7D8C',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela de Sessões / Blocos de Estudo (study_sessions)
CREATE TABLE IF NOT EXISTS study_sessions (
    id VARCHAR(50) PRIMARY KEY,
    subject_id VARCHAR(50) NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 25,
    activity_type VARCHAR(50) NOT NULL DEFAULT 'TEORIA',
    status VARCHAR(50) NOT NULL DEFAULT 'PLANEJADA',
    sync_status VARCHAR(50) NOT NULL DEFAULT 'NAO_SINCRONIZADO',
    external_event_id VARCHAR(255),
    sync_error_message TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_study_sessions_subject FOREIGN KEY (subject_id) 
        REFERENCES subjects(id) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- 3. Índices Relacionais para Otimização de Consultas e Relatórios
CREATE INDEX IF NOT EXISTS idx_study_sessions_subject_id ON study_sessions(subject_id);
CREATE INDEX IF NOT EXISTS idx_study_sessions_start_time ON study_sessions(start_time);
CREATE INDEX IF NOT EXISTS idx_study_sessions_status ON study_sessions(status);
CREATE INDEX IF NOT EXISTS idx_study_sessions_range ON study_sessions(start_time, end_time);

-- 4. Tabela de Histórico de Ciclos Pomodoro (pomodoro_cycles)
CREATE TABLE IF NOT EXISTS pomodoro_cycles (
    id VARCHAR(50) PRIMARY KEY,
    session_id VARCHAR(50),
    mode VARCHAR(50) NOT NULL DEFAULT 'FOCUS',
    duration_minutes INTEGER NOT NULL DEFAULT 25,
    completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    CONSTRAINT fk_pomodoro_study_session FOREIGN KEY (session_id) 
        REFERENCES study_sessions(id) ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_pomodoro_session_id ON pomodoro_cycles(session_id);
CREATE INDEX IF NOT EXISTS idx_pomodoro_completed_at ON pomodoro_cycles(completed_at);

-- 5. Carga Inicial de Disciplinas (Seed)
INSERT INTO subjects (id, name, code, hex_color) VALUES
('subj-1', 'Algoritmos e Estruturas de Dados', 'AED', '#3A7D8C'),
('subj-2', 'Cálculo Diferencial e Integral', 'CALC', '#7FB3D1'),
('subj-3', 'Arquitetura de Software', 'ARQ', '#7FA99B'),
('subj-4', 'Banco de Dados e SQL', 'BD', '#8A9BAA'),
('subj-5', 'Redes de Computadores', 'REDES', '#5F7F99'),
('subj-6', 'Inteligência Artificial e ML', 'IA', '#C9B99A')
ON CONFLICT (id) DO UPDATE SET
name = EXCLUDED.name,
code = EXCLUDED.code,
hex_color = EXCLUDED.hex_color;

-- 6. Carga Inicial de Sessões de Estudo de Demonstração (Seed)
INSERT INTO study_sessions (id, subject_id, subject_name, topic, start_time, end_time, duration_minutes, activity_type, status, sync_status, notes) VALUES
('seed-sess-1', 'subj-1', 'Algoritmos e Estruturas de Dados', 'Árvores Binárias e AVL', CURRENT_DATE - INTERVAL '5 days' + TIME '14:00', CURRENT_DATE - INTERVAL '5 days' + TIME '15:30', 90, 'TEORIA', 'CONCLUIDA', 'SINCRONIZADO', 'Revisão de rotações simples e duplas'),
('seed-sess-2', 'subj-2', 'Cálculo Diferencial e Integral', 'Derivadas Parciais', CURRENT_DATE - INTERVAL '4 days' + TIME '14:00', CURRENT_DATE - INTERVAL '4 days' + TIME '15:00', 60, 'EXERCICIOS', 'CONCLUIDA', 'SINCRONIZADO', 'Lista 3 exercícios 1 a 10'),
('seed-sess-3', 'subj-4', 'Banco de Dados e SQL', 'Modelagem ER e Normalização', CURRENT_DATE - INTERVAL '3 days' + TIME '14:00', CURRENT_DATE - INTERVAL '3 days' + TIME '16:00', 120, 'TEORIA', 'CONCLUIDA', 'SINCRONIZADO', '1FN, 2FN, 3FN e BCNF'),
('seed-sess-4', 'subj-3', 'Arquitetura de Software', 'Microsserviços e Event-Driven', CURRENT_DATE - INTERVAL '2 days' + TIME '14:00', CURRENT_DATE - INTERVAL '2 days' + TIME '15:20', 80, 'REVISAO', 'CONCLUIDA', 'SINCRONIZADO', 'Padrão Saga e Outbox Pattern'),
('seed-sess-5', 'subj-1', 'Algoritmos e Estruturas de Dados', 'Grafos (Dijkstra e BFS)', CURRENT_DATE - INTERVAL '1 days' + TIME '14:00', CURRENT_DATE - INTERVAL '1 days' + TIME '15:40', 100, 'EXERCICIOS', 'CONCLUIDA', 'SINCRONIZADO', 'Implementação em Java e complexidade'),
('seed-sess-6', 'subj-6', 'Inteligência Artificial e ML', 'Regressão Linear e Otimização', CURRENT_DATE + TIME '14:00', CURRENT_DATE + TIME '15:30', 90, 'TEORIA', 'PLANEJADA', 'SINCRONIZADO', 'Gradiente Descendente e MSE')
ON CONFLICT (id) DO NOTHING;
