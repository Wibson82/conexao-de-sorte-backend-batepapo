package br.tec.facilitaservicos.batepapo.apresentacao.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import br.tec.facilitaservicos.batepapo.dominio.enums.StatusPresenca;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO reativo para Usuário Online no Chat
 * 
 * @param id Identificador único
 * @param usuarioId ID do usuário
 * @param usuarioNome Nome do usuário
 * @param sala Nome da sala
 * @param statusPresenca Status de presença
 * @param sessionId ID da sessão
 * @param dataEntrada Data/hora de entrada na sala
 * @param ultimoHeartbeat Data/hora do último heartbeat
 * @param tempoOnlineMinutos Tempo online em minutos
 * @param dispositivo Tipo de dispositivo
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Representa um usuário online em uma sala de chat, com informações de presença.")
public record UsuarioOnlineDto(
    @Schema(description = "Identificador único do registro de usuário online", example = "1")
    Long id,
    
    @Schema(description = "ID do usuário", example = "12345")
    @NotNull(message = "ID do usuário é obrigatório")
    Long usuarioId,
    
    @Schema(description = "Nome de exibição do usuário", example = "João Silva")
    @NotBlank(message = "Nome do usuário é obrigatório")
    String usuarioNome,
    
    @Schema(description = "Nome ou ID da sala em que o usuário está online", example = "sala-geral")
    @NotBlank(message = "Sala é obrigatória")
    String sala,
    
    @Schema(description = "Status de presença do usuário", example = "ONLINE")
    @NotNull(message = "Status de presença é obrigatório")
    StatusPresenca statusPresenca,
    
    @Schema(description = "Identificador único da sessão do usuário", example = "sessao-xyz-789")
    String sessionId,
    
    @Schema(description = "Data e hora de entrada do usuário na sala", example = "2025-09-01T09:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataEntrada,
    
    @Schema(description = "Data e hora do último heartbeat do usuário", example = "2025-09-01T10:05:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime ultimoHeartbeat,
    
    @Schema(description = "Tempo que o usuário está online na sala em minutos", example = "65")
    Long tempoOnlineMinutos,
    @Schema(description = "Tipo de dispositivo usado para acessar o chat", example = "WEB")
    String dispositivo
) {
    
    /**
     * Cria DTO com dados básicos para entrada
     */
    public static UsuarioOnlineDto paraEntrada(Long usuarioId, String usuarioNome, String sala, String sessionId) {
        return new UsuarioOnlineDto(
            null, usuarioId, usuarioNome, sala, StatusPresenca.ONLINE,
            sessionId, LocalDateTime.now(), LocalDateTime.now(), 0L, null
        );
    }
    
    /**
     * Cria DTO completo
     */
    public static UsuarioOnlineDto completo(Long id, Long usuarioId, String usuarioNome, String sala,
                                           StatusPresenca statusPresenca, String sessionId,
                                           LocalDateTime dataEntrada, LocalDateTime ultimoHeartbeat,
                                           Long tempoOnlineMinutos, String dispositivo) {
        return new UsuarioOnlineDto(id, usuarioId, usuarioNome, sala, statusPresenca,
                                   sessionId, dataEntrada, ultimoHeartbeat, tempoOnlineMinutos, dispositivo);
    }
    
    /**
     * Verifica se o usuário está online
     */
    public boolean isOnline() {
        return statusPresenca == StatusPresenca.ONLINE;
    }
    
    /**
     * Verifica se o usuário está ausente
     */
    public boolean isAusente() {
        return statusPresenca == StatusPresenca.AUSENTE;
    }
    
    /**
     * Verifica se a conexão está ativa
     */
    public boolean isConexaoAtiva() {
        return ultimoHeartbeat != null && 
               ultimoHeartbeat.isAfter(LocalDateTime.now().minusMinutes(5));
    }
    
    /**
     * Verifica se é uma nova sessão
     */
    public boolean isNovaSessao() {
        return dataEntrada != null && 
               dataEntrada.isAfter(LocalDateTime.now().minusMinutes(2));
    }
    
    /**
     * Obtém ícone do status de presença
     */
    public String getIconeStatus() {
        return statusPresenca != null ? statusPresenca.getIcone() : "🟢";
    }
    
    /**
     * Obtém cor do status de presença
     */
    public String getCorStatus() {
        return statusPresenca != null ? statusPresenca.getCor() : "success";
    }
    
    /**
     * Obtém descrição do status
     */
    public String getDescricaoStatus() {
        return statusPresenca != null ? statusPresenca.getDescricao() : "Online";
    }
}