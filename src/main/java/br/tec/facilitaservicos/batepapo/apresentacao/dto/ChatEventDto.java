package br.tec.facilitaservicos.batepapo.apresentacao.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO reativo para Eventos de Chat em tempo real (SSE/WebSocket)
 * 
 * @param tipo Tipo do evento
 * @param sala Nome da sala onde ocorreu o evento
 * @param timestamp Data/hora do evento
 * @param mensagem Dados da mensagem (para eventos de mensagem)
 * @param usuarioOnline Dados do usuário (para eventos de presença)
 * @param sala Dados da sala (para eventos de sala)
 * @param dados Dados adicionais do evento
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Representa um evento de chat em tempo real para comunicação via SSE/WebSocket")
public record ChatEventDto(
    @Schema(description = "Tipo do evento de chat", example = "NOVA_MENSAGEM")
    TipoEvento tipo,
    @Schema(description = "Nome ou ID da sala onde ocorreu o evento", example = "sala-geral")
    String sala,
    
    @Schema(description = "Data e hora exata em que o evento ocorreu", example = "2025-09-01T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp,
    
    @Schema(description = "Dados da mensagem (presente para eventos de mensagem)")
    MensagemDto mensagem,
    @Schema(description = "Dados do usuário online (presente para eventos de presença)")
    UsuarioOnlineDto usuarioOnline,
    @Schema(description = "Dados da sala (presente para eventos de sala)")
    SalaDto salaInfo,
    @Schema(description = "Dados adicionais do evento (estrutura varia conforme o tipo de evento)")
    Object dados
) {
    
    /**
     * Tipos de eventos do chat
     */
    @Schema(description = "Tipos de eventos do chat")
    public enum TipoEvento {
        // Eventos de mensagem
        @Schema(description = "Nova mensagem enviada")
        NOVA_MENSAGEM,
        @Schema(description = "Mensagem existente editada")
        MENSAGEM_EDITADA,
        @Schema(description = "Mensagem excluída")
        MENSAGEM_EXCLUIDA,
        
        // Eventos de usuário
        @Schema(description = "Usuário entrou na sala")
        USUARIO_ENTROU,
        @Schema(description = "Usuário saiu da sala")
        USUARIO_SAIU,
        @Schema(description = "Status de presença do usuário mudou")
        USUARIO_MUDOU_STATUS,
        
        // Eventos de sala
        @Schema(description = "Nova sala de chat criada")
        SALA_CRIADA,
        @Schema(description = "Informações da sala atualizadas")
        SALA_ATUALIZADA,
        @Schema(description = "Sala de chat fechada")
        SALA_FECHADA,
        
        // Eventos de sistema
        @Schema(description = "Evento de heartbeat para manter a conexão ativa")
        HEARTBEAT,
        @Schema(description = "Ocorreu um erro no sistema")
        ERRO,
        @Schema(description = "Sistema em manutenção")
        MANUTENCAO
    }
    
    /**
     * Cria evento de nova mensagem
     */
    public static ChatEventDto novaMensagem(String sala, MensagemDto mensagem) {
        return new ChatEventDto(TipoEvento.NOVA_MENSAGEM, sala, LocalDateTime.now(),
                               mensagem, null, null, null);
    }
    
    /**
     * Cria evento de usuário entrando
     */
    public static ChatEventDto usuarioEntrou(String sala, UsuarioOnlineDto usuario) {
        return new ChatEventDto(TipoEvento.USUARIO_ENTROU, sala, LocalDateTime.now(),
                               null, usuario, null, null);
    }
    
    /**
     * Cria evento de usuário saindo
     */
    public static ChatEventDto usuarioSaiu(String sala, UsuarioOnlineDto usuario) {
        return new ChatEventDto(TipoEvento.USUARIO_SAIU, sala, LocalDateTime.now(),
                               null, usuario, null, null);
    }
    
    /**
     * Cria evento de mudança de status
     */
    public static ChatEventDto mudancaStatus(String sala, UsuarioOnlineDto usuario) {
        return new ChatEventDto(TipoEvento.USUARIO_MUDOU_STATUS, sala, LocalDateTime.now(),
                               null, usuario, null, null);
    }
    
    /**
     * Cria evento de sala atualizada
     */
    public static ChatEventDto salaAtualizada(SalaDto sala) {
        return new ChatEventDto(TipoEvento.SALA_ATUALIZADA, sala.nome(), LocalDateTime.now(),
                               null, null, sala, null);
    }
    
    /**
     * Cria evento de heartbeat
     */
    public static ChatEventDto heartbeat(String sala) {
        return new ChatEventDto(TipoEvento.HEARTBEAT, sala, LocalDateTime.now(),
                               null, null, null, null);
    }
    
    /**
     * Cria evento de erro
     */
    public static ChatEventDto erro(String sala, String mensagemErro) {
        return new ChatEventDto(TipoEvento.ERRO, sala, LocalDateTime.now(),
                               null, null, null, mensagemErro);
    }
    
    /**
     * Verifica se é evento de mensagem
     */
    public boolean isEventoMensagem() {
        return tipo == TipoEvento.NOVA_MENSAGEM || 
               tipo == TipoEvento.MENSAGEM_EDITADA || 
               tipo == TipoEvento.MENSAGEM_EXCLUIDA;
    }
    
    /**
     * Verifica se é evento de usuário
     */
    public boolean isEventoUsuario() {
        return tipo == TipoEvento.USUARIO_ENTROU || 
               tipo == TipoEvento.USUARIO_SAIU || 
               tipo == TipoEvento.USUARIO_MUDOU_STATUS;
    }
    
    /**
     * Verifica se é evento de sala
     */
    public boolean isEventoSala() {
        return tipo == TipoEvento.SALA_CRIADA || 
               tipo == TipoEvento.SALA_ATUALIZADA || 
               tipo == TipoEvento.SALA_FECHADA;
    }
    
    /**
     * Verifica se é evento do sistema
     */
    public boolean isEventoSistema() {
        return tipo == TipoEvento.HEARTBEAT || 
               tipo == TipoEvento.ERRO || 
               tipo == TipoEvento.MANUTENCAO;
    }
}