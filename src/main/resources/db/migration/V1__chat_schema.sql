-- Schema isolado para Chat Service no MySQL compartilhado
-- Usando prefixo 'chat_' para evitar conflitos com outras tabelas

-- Tabela de salas de chat
CREATE TABLE IF NOT EXISTS chat_salas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    tipo ENUM('geral', 'resultados', 'dicas', 'suporte') DEFAULT 'geral',
    max_usuarios INT DEFAULT 100,
    ativo BOOLEAN DEFAULT TRUE,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_chat_salas_nome (nome),
    INDEX idx_chat_salas_tipo (tipo),
    INDEX idx_chat_salas_ativo (ativo)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de mensagens do chat
CREATE TABLE IF NOT EXISTS chat_mensagens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sala_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,  -- FK para usuarios do Auth Service
    conteudo TEXT NOT NULL,
    tipo ENUM('TEXT', 'IMAGE', 'FILE', 'SYSTEM') DEFAULT 'TEXT',
    metadata JSON,
    data_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    editado BOOLEAN DEFAULT FALSE,
    data_edicao TIMESTAMP NULL,
    
    FOREIGN KEY (sala_id) REFERENCES chat_salas(id) ON DELETE CASCADE,
    INDEX idx_chat_mensagens_sala_data (sala_id, data_envio DESC),
    INDEX idx_chat_mensagens_usuario (usuario_id),
    INDEX idx_chat_mensagens_tipo (tipo)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de usuários online por sala
CREATE TABLE IF NOT EXISTS chat_usuarios_online (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    sala_id BIGINT NOT NULL,
    status ENUM('ONLINE', 'AWAY', 'TYPING') DEFAULT 'ONLINE',
    ultima_atividade TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    data_entrada TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_chat_usuario_sala (usuario_id, sala_id),
    FOREIGN KEY (sala_id) REFERENCES chat_salas(id) ON DELETE CASCADE,
    INDEX idx_chat_usuarios_online_sala (sala_id),
    INDEX idx_chat_usuarios_online_status (status),
    INDEX idx_chat_usuarios_online_atividade (ultima_atividade)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Inserir salas padrão
INSERT IGNORE INTO chat_salas (nome, descricao, tipo) VALUES
('Geral', 'Conversa geral entre usuários', 'geral'),
('Resultados', 'Discussão sobre resultados dos jogos', 'resultados'),
('Dicas', 'Compartilhamento de dicas e estratégias', 'dicas'),
('Suporte', 'Canal de suporte ao usuário', 'suporte');

-- Tabela para histórico de eventos (para auditoria)
CREATE TABLE IF NOT EXISTS chat_eventos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo VARCHAR(50) NOT NULL,
    usuario_id BIGINT,
    sala_id BIGINT,
    mensagem_id BIGINT,
    dados JSON,
    data_evento TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_chat_eventos_tipo (tipo),
    INDEX idx_chat_eventos_usuario (usuario_id),
    INDEX idx_chat_eventos_data (data_evento)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;