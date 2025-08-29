-- Schema simplificado para o Chat Service - MVP
-- Tabela de mensagens de chat

CREATE TABLE IF NOT EXISTS mensagens_chat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conteudo TEXT NOT NULL,
    usuario_id BIGINT NOT NULL,
    usuario_nome VARCHAR(100) NOT NULL,
    sala VARCHAR(50) NOT NULL,
    tipo ENUM('TEXTO', 'SISTEMA', 'ENTRADA', 'SAIDA', 'MODERACAO', 'RESULTADO', 'DICA', 'COMANDO') DEFAULT 'TEXTO',
    status ENUM('ENVIADA', 'ENTREGUE', 'LIDA', 'ERRO', 'MODERADA', 'EXCLUIDA', 'PENDENTE', 'REMOVIDA_MODERACAO', 'QUARENTENA') DEFAULT 'ENVIADA',
    data_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sala_data (sala, data_envio),
    INDEX idx_usuario (usuario_id),
    INDEX idx_data_envio (data_envio)
);