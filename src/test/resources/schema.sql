-- ============================================================================
-- 🗄️ SCHEMA SQL PARA TESTES H2 - MICROSERVIÇO BATE-PAPO
-- ============================================================================
--
-- Schema compatível com as entidades R2DBC:
-- - MensagemR2dbc / MensagemSimplesR2dbc
-- - UsuarioOnlineR2dbc
-- - SalaR2dbc
--
-- @author Sistema de Migração R2DBC
-- @version 1.0
-- @since 2024
-- ============================================================================

-- ========================================
-- 📋 TABELA: SALAS_CHAT
-- ========================================
CREATE TABLE IF NOT EXISTS salas_chat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(200),
    tipo VARCHAR(20) NOT NULL DEFAULT 'PUBLICA',
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    max_usuarios INTEGER DEFAULT 100,
    moderada BOOLEAN DEFAULT FALSE,
    usuarios_online INTEGER DEFAULT 0,
    total_mensagens BIGINT DEFAULT 0,
    criada_por BIGINT,
    ultima_atividade TIMESTAMP,
    configuracoes TEXT,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(50),
    atualizado_por VARCHAR(50)
);

-- ========================================
-- 💬 TABELA: MENSAGENS_CHAT
-- ========================================
CREATE TABLE IF NOT EXISTS mensagens_chat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conteudo TEXT NOT NULL,
    usuario_id BIGINT NOT NULL,
    usuario_nome VARCHAR(100) NOT NULL,
    sala VARCHAR(50) NOT NULL,
    tipo VARCHAR(20) DEFAULT 'TEXTO',
    status VARCHAR(20) DEFAULT 'ENVIADA',
    data_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_edicao TIMESTAMP,
    resposta_para_id BIGINT,
    anexos TEXT,
    metadata TEXT,
    ip_origem VARCHAR(45),
    user_agent TEXT,
    editada BOOLEAN DEFAULT FALSE,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(50),
    atualizado_por VARCHAR(50),
    FOREIGN KEY (resposta_para_id) REFERENCES mensagens_chat(id)
);

-- ========================================
-- 👥 TABELA: USUARIOS_ONLINE_CHAT
-- ========================================
CREATE TABLE IF NOT EXISTS usuarios_online_chat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    usuario_nome VARCHAR(100) NOT NULL,
    sala VARCHAR(50) NOT NULL,
    status_presenca VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    session_id VARCHAR(100) NOT NULL UNIQUE,
    connection_id VARCHAR(100),
    data_entrada TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultimo_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_saida TIMESTAMP,
    ip_origem VARCHAR(45),
    user_agent TEXT,
    dispositivo VARCHAR(50),
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(50),
    atualizado_por VARCHAR(50)
);

-- ========================================
-- 📊 ÍNDICES PARA PERFORMANCE
-- ========================================

-- Índices para mensagens_chat
CREATE INDEX IF NOT EXISTS idx_mensagens_sala ON mensagens_chat(sala);
CREATE INDEX IF NOT EXISTS idx_mensagens_usuario ON mensagens_chat(usuario_id);
CREATE INDEX IF NOT EXISTS idx_mensagens_data_envio ON mensagens_chat(data_envio);
CREATE INDEX IF NOT EXISTS idx_mensagens_sala_data ON mensagens_chat(sala, data_envio);

-- Índices para usuarios_online_chat
CREATE INDEX IF NOT EXISTS idx_usuarios_online_sala ON usuarios_online_chat(sala);
CREATE INDEX IF NOT EXISTS idx_usuarios_online_usuario ON usuarios_online_chat(usuario_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_online_session ON usuarios_online_chat(session_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_online_status ON usuarios_online_chat(status_presenca);

-- Índices para salas_chat
CREATE INDEX IF NOT EXISTS idx_salas_nome ON salas_chat(nome);
CREATE INDEX IF NOT EXISTS idx_salas_tipo ON salas_chat(tipo);
CREATE INDEX IF NOT EXISTS idx_salas_status ON salas_chat(status);

-- ========================================
-- 🧪 DADOS DE TESTE
-- ========================================

-- Salas de teste
INSERT INTO salas_chat (nome, descricao, tipo, status, criada_por) VALUES 
('geral', 'Sala geral para conversas', 'PUBLICA', 'ATIVA', 1),
('tecnologia', 'Discussões sobre tecnologia', 'PUBLICA', 'ATIVA', 1),
('jogos', 'Conversa sobre jogos', 'PUBLICA', 'ATIVA', 1),
('privada-teste', 'Sala privada para testes', 'PRIVADA', 'ATIVA', 1);

-- Mensagens de teste
INSERT INTO mensagens_chat (conteudo, usuario_id, usuario_nome, sala) VALUES 
('Olá pessoal!', 123, 'user123', 'geral'),
('Como vocês estão?', 456, 'user456', 'geral'),
('Alguém viu as novidades do Spring?', 123, 'user123', 'tecnologia'),
('Sim, o WebFlux está incrível!', 789, 'moderator123', 'tecnologia'),
('Quem joga Valorant?', 456, 'user456', 'jogos');

-- Usuários online de teste
INSERT INTO usuarios_online_chat (usuario_id, usuario_nome, sala, session_id, status_presenca) VALUES 
(123, 'user123', 'geral', 'session-123-geral', 'ONLINE'),
(456, 'user456', 'geral', 'session-456-geral', 'ONLINE'),
(789, 'moderator123', 'tecnologia', 'session-789-tech', 'ONLINE'),
(123, 'user123', 'tecnologia', 'session-123-tech', 'AUSENTE');