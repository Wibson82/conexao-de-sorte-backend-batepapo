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
public record ChatEventDto(
    TipoEvento tipo,
    String sala,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp,
    
    MensagemDto mensagem,
    UsuarioOnlineDto usuarioOnline,
    SalaDto salaInfo,
    Object dados
) {
    
    /**
     * Tipos de eventos do chat
     */
    public enum TipoEvento {
        // Eventos de mensagem
        NOVA_MENSAGEM,
        MENSAGEM_EDITADA,
        MENSAGEM_EXCLUIDA,
        
        // Eventos de usuário
        USUARIO_ENTROU,
        USUARIO_SAIU,
        USUARIO_MUDOU_STATUS,
        
        // Eventos de sala
        SALA_CRIADA,
        SALA_ATUALIZADA,
        SALA_FECHADA,
        
        // Eventos de sistema
        HEARTBEAT,
        ERRO,
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