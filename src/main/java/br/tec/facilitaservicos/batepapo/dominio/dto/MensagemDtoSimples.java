package br.tec.facilitaservicos.batepapo.dominio.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Simple DTO for chat messages
 * Follows AGENTS.md guidelines - Java 24, Spring Boot 3.5.5
 */
public record MensagemDtoSimples(
    Long id,
    Long usuarioId,
    String salaId,
    String conteudo,
    String tipo,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp
) {}