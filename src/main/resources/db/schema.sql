-- ============================================================================
-- SISTEMA DE ESTUDANTES — ESQUEMA RELACIONAL POSTGRESQL (EM PORTUGUÊS)
-- Banco de dados: Estudantes / postgres
-- Host: localhost | Porta: 5432 | Usuário: postgres
-- Tabelas: disciplinas, sessoes_estudo, ciclos_pomodoro
-- Colunas em Português: id, nome, codigo, cor_hex, criado_em, disciplina_id, etc.
-- ============================================================================

-- 0. Dropar views legadas para permitir migração limpa sem conflito de dependências
DO $$
BEGIN
    EXECUTE 'DROP VIEW IF EXISTS study_sessions CASCADE';
    EXECUTE 'DROP VIEW IF EXISTS subjects CASCADE';
    EXECUTE 'DROP VIEW IF EXISTS pomodoro_cycles CASCADE';
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- 0.1 Dropar tabelas legadas em inglês (caso existam de versões anteriores)
DO $$
BEGIN
    EXECUTE 'DROP TABLE IF EXISTS pomodoro_cycles CASCADE';
    EXECUTE 'DROP TABLE IF EXISTS study_sessions CASCADE';
    EXECUTE 'DROP TABLE IF EXISTS subjects CASCADE';
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- 0.2 Migração de colunas em inglês para português caso as tabelas já existam
DO $$
BEGIN
    -- Migração da tabela disciplinas
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'disciplinas' AND column_name = 'name') THEN
        ALTER TABLE disciplinas RENAME COLUMN name TO nome;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'disciplinas' AND column_name = 'code') THEN
        ALTER TABLE disciplinas RENAME COLUMN code TO codigo;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'disciplinas' AND column_name = 'hex_color') THEN
        ALTER TABLE disciplinas RENAME COLUMN hex_color TO cor_hex;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'disciplinas' AND column_name = 'created_at') THEN
        ALTER TABLE disciplinas RENAME COLUMN created_at TO criado_em;
    END IF;

    -- Migração da tabela sessoes_estudo
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sessoes_estudo' AND column_name = 'subject_id') THEN
        ALTER TABLE sessoes_estudo RENAME COLUMN subject_id TO disciplina_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sessoes_estudo' AND column_name = 'subject_name') THEN
        ALTER TABLE sessoes_estudo RENAME COLUMN subject_name TO disciplina_nome;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sessoes_estudo' AND column_name = 'topic') THEN
        ALTER TABLE sessoes_estudo RENAME COLUMN topic TO topico;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sessoes_estudo' AND column_name = 'start_time') THEN
        ALTER TABLE sessoes_estudo RENAME COLUMN start_time TO data_inicio;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sessoes_estudo' AND column_name = 'end_time') THEN
        ALTER TABLE sessoes_estudo RENAME COLUMN end_time TO data_fim;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sessoes_estudo' AND column_name = 'duration_minutes') THEN
        ALTER TABLE sessoes_estudo RENAME COLUMN duration_minutes TO duracao_minutos;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sessoes_estudo' AND column_name = 'activity_type') THEN
        ALTER TABLE sessoes_estudo RENAME COLUMN activity_type TO tipo_atividade;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sessoes_estudo' AND column_name = 'sync_status') THEN
        ALTER TABLE sessoes_estudo RENAME COLUMN sync_status TO status_sincronizacao;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sessoes_estudo' AND column_name = 'external_event_id') THEN
        ALTER TABLE sessoes_estudo RENAME COLUMN external_event_id TO evento_externo_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sessoes_estudo' AND column_name = 'sync_error_message') THEN
        ALTER TABLE sessoes_estudo RENAME COLUMN sync_error_message TO mensagem_erro_sincronizacao;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sessoes_estudo' AND column_name = 'notes') THEN
        ALTER TABLE sessoes_estudo RENAME COLUMN notes TO observacoes;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sessoes_estudo' AND column_name = 'created_at') THEN
        ALTER TABLE sessoes_estudo RENAME COLUMN created_at TO criado_em;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sessoes_estudo' AND column_name = 'updated_at') THEN
        ALTER TABLE sessoes_estudo RENAME COLUMN updated_at TO atualizado_em;
    END IF;

    -- Migração da tabela ciclos_pomodoro
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'ciclos_pomodoro' AND column_name = 'session_id') THEN
        ALTER TABLE ciclos_pomodoro RENAME COLUMN session_id TO sessao_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'ciclos_pomodoro' AND column_name = 'mode') THEN
        ALTER TABLE ciclos_pomodoro RENAME COLUMN mode TO modo;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'ciclos_pomodoro' AND column_name = 'duration_minutes') THEN
        ALTER TABLE ciclos_pomodoro RENAME COLUMN duration_minutes TO duracao_minutos;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'ciclos_pomodoro' AND column_name = 'completed_at') THEN
        ALTER TABLE ciclos_pomodoro RENAME COLUMN completed_at TO concluido_em;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'ciclos_pomodoro' AND column_name = 'notes') THEN
        ALTER TABLE ciclos_pomodoro RENAME COLUMN notes TO observacoes;
    END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- 1. Tabela de Disciplinas / Matérias (disciplinas)
CREATE TABLE IF NOT EXISTS disciplinas (
    id VARCHAR(50) PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    cor_hex VARCHAR(20) NOT NULL DEFAULT '#3A7D8C',
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela de Sessões e Blocos de Estudo (sessoes_estudo)
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
    CONSTRAINT fk_sessoes_estudo_disciplina FOREIGN KEY (disciplina_id) 
        REFERENCES disciplinas(id) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- 3. Índices Relacionais para Otimização de Consultas e Relatórios
CREATE INDEX IF NOT EXISTS idx_sessoes_estudo_disciplina_id ON sessoes_estudo(disciplina_id);
CREATE INDEX IF NOT EXISTS idx_sessoes_estudo_data_inicio ON sessoes_estudo(data_inicio);
CREATE INDEX IF NOT EXISTS idx_sessoes_estudo_status ON sessoes_estudo(status);
CREATE INDEX IF NOT EXISTS idx_sessoes_estudo_range ON sessoes_estudo(data_inicio, data_fim);

-- 4. Tabela de Histórico de Ciclos Pomodoro (ciclos_pomodoro)
CREATE TABLE IF NOT EXISTS ciclos_pomodoro (
    id VARCHAR(50) PRIMARY KEY,
    sessao_id VARCHAR(50),
    modo VARCHAR(50) NOT NULL DEFAULT 'FOCUS',
    duracao_minutos INTEGER NOT NULL DEFAULT 25,
    concluido_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacoes TEXT,
    CONSTRAINT fk_ciclos_pomodoro_sessao FOREIGN KEY (sessao_id) 
        REFERENCES sessoes_estudo(id) ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_ciclos_pomodoro_sessao_id ON ciclos_pomodoro(sessao_id);
CREATE INDEX IF NOT EXISTS idx_ciclos_pomodoro_concluido_em ON ciclos_pomodoro(concluido_em);

-- 5. Views de Compatibilidade e Aliases (permite consultas legadas transparentes)
CREATE OR REPLACE VIEW subjects AS 
    SELECT 
        id, 
        nome AS name, 
        codigo AS code, 
        cor_hex AS hex_color, 
        criado_em AS created_at 
    FROM disciplinas;

CREATE OR REPLACE VIEW study_sessions AS 
    SELECT 
        id, 
        disciplina_id AS subject_id, 
        disciplina_nome AS subject_name, 
        topico AS topic, 
        data_inicio AS start_time, 
        data_fim AS end_time, 
        duracao_minutos AS duration_minutes, 
        tipo_atividade AS activity_type, 
        status, 
        status_sincronizacao AS sync_status, 
        evento_externo_id AS external_event_id, 
        mensagem_erro_sincronizacao AS sync_error_message, 
        observacoes AS notes, 
        criado_em AS created_at, 
        atualizado_em AS updated_at
    FROM sessoes_estudo;

CREATE OR REPLACE VIEW pomodoro_cycles AS 
    SELECT 
        id, 
        sessao_id AS session_id, 
        modo AS mode, 
        duracao_minutos AS duration_minutes, 
        concluido_em AS completed_at, 
        observacoes AS notes 
    FROM ciclos_pomodoro;

-- 6. Carga Inicial de Disciplinas (Seed)
INSERT INTO disciplinas (id, nome, codigo, cor_hex) VALUES
('subj-1', 'Algoritmos e Estruturas de Dados', 'AED', '#3A7D8C'),
('subj-2', 'Cálculo Diferencial e Integral', 'CALC', '#7FB3D1'),
('subj-3', 'Arquitetura de Software', 'ARQ', '#7FA99B'),
('subj-4', 'Banco de Dados e SQL', 'BD', '#8A9BAA'),
('subj-5', 'Redes de Computadores', 'REDES', '#5F7F99'),
('subj-6', 'Inteligência Artificial e ML', 'IA', '#C9B99A')
ON CONFLICT (id) DO UPDATE SET
nome = EXCLUDED.nome,
codigo = EXCLUDED.codigo,
cor_hex = EXCLUDED.cor_hex;

-- 7. Carga Inicial de Sessões de Estudo de Demonstração (Seed)
INSERT INTO sessoes_estudo (id, disciplina_id, disciplina_nome, topico, data_inicio, data_fim, duracao_minutos, tipo_atividade, status, status_sincronizacao, observacoes) VALUES
('seed-sess-1', 'subj-1', 'Algoritmos e Estruturas de Dados', 'Árvores Binárias e AVL', CURRENT_DATE - INTERVAL '5 days' + TIME '14:00', CURRENT_DATE - INTERVAL '5 days' + TIME '15:30', 90, 'TEORIA', 'CONCLUIDA', 'SINCRONIZADO', 'Revisão de rotações simples e duplas'),
('seed-sess-2', 'subj-2', 'Cálculo Diferencial e Integral', 'Derivadas Parciais', CURRENT_DATE - INTERVAL '4 days' + TIME '14:00', CURRENT_DATE - INTERVAL '4 days' + TIME '15:00', 60, 'EXERCICIOS', 'CONCLUIDA', 'SINCRONIZADO', 'Lista 3 exercícios 1 a 10'),
('seed-sess-3', 'subj-4', 'Banco de Dados e SQL', 'Modelagem ER e Normalização', CURRENT_DATE - INTERVAL '3 days' + TIME '14:00', CURRENT_DATE - INTERVAL '3 days' + TIME '16:00', 120, 'TEORIA', 'CONCLUIDA', 'SINCRONIZADO', '1FN, 2FN, 3FN e BCNF'),
('seed-sess-4', 'subj-3', 'Arquitetura de Software', 'Microsserviços e Event-Driven', CURRENT_DATE - INTERVAL '2 days' + TIME '14:00', CURRENT_DATE - INTERVAL '2 days' + TIME '15:20', 80, 'REVISAO', 'CONCLUIDA', 'SINCRONIZADO', 'Padrão Saga e Outbox Pattern'),
('seed-sess-5', 'subj-1', 'Algoritmos e Estruturas de Dados', 'Grafos (Dijkstra e BFS)', CURRENT_DATE - INTERVAL '1 days' + TIME '14:00', CURRENT_DATE - INTERVAL '1 days' + TIME '15:40', 100, 'EXERCICIOS', 'CONCLUIDA', 'SINCRONIZADO', 'Implementação em Java e complexidade'),
('seed-sess-6', 'subj-6', 'Inteligência Artificial e ML', 'Regressão Linear e Otimização', CURRENT_DATE + TIME '14:00', CURRENT_DATE + TIME '15:30', 90, 'TEORIA', 'PLANEJADA', 'SINCRONIZADO', 'Gradiente Descendente e MSE')
ON CONFLICT (id) DO NOTHING;
