package br.tec.facilitaservicos.batepapo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * ============================================================================
 * 💬 APLICAÇÃO PRINCIPAL - MICROSERVIÇO BATE-PAPO
 * ============================================================================
 * 
 * Microserviço de bate-papo em tempo real 100% reativo usando:
 * - Spring Boot 3.5+
 * - WebFlux (reativo)
 * - R2DBC (reativo)  
 * - Spring Security reativo com JWT
 * - WebSocket + SSE para tempo real
 * - Redis Streams para messaging distribuído
 * - Cache inteligente para mensagens frequentes
 * - Rate limiting por usuário
 * 
 * Endpoints:
 * - GET /api/chat/stream/{sala} - SSE stream de mensagens
 * - POST /api/chat/mensagem - Enviar mensagem
 * - GET /api/chat/salas - Listar salas disponíveis
 * - GET /api/chat/mensagens/{sala} - Histórico paginado
 * - GET /api/chat/online/{sala} - Usuários online
 * - WebSocket /ws/chat - Conexão WebSocket
 * 
 * ============================================================================
 */
@SpringBootApplication
@EnableR2dbcRepositories
@EnableR2dbcAuditing
public class BatepapoApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(BatepapoApplication.class, args);
    }
}