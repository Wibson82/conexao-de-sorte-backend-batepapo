package br.tec.facilitaservicos.batepapo.apresentacao.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import br.tec.facilitaservicos.batepapo.dominio.enums.StatusSala;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoSala;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO reativo para Sala de Chat.
 * 
 * Representa uma sala de bate-papo com todas as informações
 * necessárias para gerenciamento, controle de acesso e
 * monitoramento de atividades em tempo real.
 * 
 * Principais casos de uso:
 * - Criação e configuração de salas de chat
 * - Controle de acesso e limites de usuários
 * - Monitoramento de atividade e estatísticas
 * - Moderação e administração de salas
 * 
 * Funcionalidades suportadas:
 * - Diferentes tipos de sala (pública, privada, moderada)
 * - Controle de capacidade máxima de usuários
 * - Sistema de moderação com permissões
 * - Rastreamento de atividade em tempo real
 * - Estatísticas de uso e engagement
 * 
 * Restrições de negócio:
 * - Nome da sala limitado a 50 caracteres
 * - Descrição limitada a 200 caracteres
 * - Capacidade máxima varia por tipo de sala
 * - Apenas criador e moderadores podem alterar configurações
 * - Salas inativas são automaticamente arquivadas
 * 
 * Relacionamentos:
 * - Conecta com microserviço de usuários para validação
 * - Integra com sistema de permissões para moderação
 * - Vincula com mensagens para estatísticas
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Sala de bate-papo com configurações e estatísticas de atividade")
public record SalaDto(
    @Schema(description = "Identificador único da sala", 
            example = "42")
    Long id,
    
    @Schema(description = "Nome da sala de chat", 
            example = "Sala Geral",
            required = true,
            maxLength = 50)
    @NotBlank(message = "Nome da sala é obrigatório")
    @Size(max = 50, message = "Nome da sala não pode exceder 50 caracteres")
    String nome,
    
    @Schema(description = "Descrição detalhada sobre o propósito da sala", 
            example = "Espaço para conversas gerais e apresentações",
            maxLength = 200)
    @Size(max = 200, message = "Descrição não pode exceder 200 caracteres")
    String descricao,
    
    @Schema(description = "Tipo da sala que define suas características", 
            example = "PUBLICA",
            allowableValues = {"PUBLICA", "PRIVADA", "MODERADA", "VIP", "TEMPORARIA"},
            required = true)
    @NotNull(message = "Tipo da sala é obrigatório")
    TipoSala tipo,
    
    @Schema(description = "Status atual da sala", 
            example = "ATIVA",
            allowableValues = {"ATIVA", "INATIVA", "ARQUIVADA", "MANUTENCAO"})
    StatusSala status,
    
    @Schema(description = "Número máximo de usuários permitidos simultaneamente", 
            example = "100",
            minimum = "1")
    Integer maxUsuarios,
    
    @Schema(description = "Indica se a sala possui moderação ativa", 
            example = "false")
    Boolean moderada,
    
    @Schema(description = "Número atual de usuários conectados à sala", 
            example = "23",
            minimum = "0")
    Integer usuariosOnline,
    
    @Schema(description = "Total de mensagens enviadas nesta sala", 
            example = "1847",
            minimum = "0")
    Long totalMensagens,
    
    @Schema(description = "Identificador do usuário que criou a sala", 
            example = "12345")
    Long criadaPor,
    
    @Schema(description = "Data e hora da última atividade na sala", 
            example = "2025-09-01T16:45:30")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime ultimaAtividade,
    
    @Schema(description = "Data e hora de criação da sala", 
            example = "2025-08-15T10:20:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataCriacao
) {
    
    /**
     * Cria DTO com dados básicos para criação
     */
    public static SalaDto paraCriacao(String nome, String descricao, TipoSala tipo, Long criadaPor) {
        return new SalaDto(
            null, nome, descricao, tipo, StatusSala.ATIVA, 
            tipo.getMaxUsuariosPadrao(), false, 0, 0L, criadaPor,
            LocalDateTime.now(), LocalDateTime.now()
        );
    }
    
    /**
     * Cria DTO completo
     */
    public static SalaDto completa(Long id, String nome, String descricao, TipoSala tipo, 
                                  StatusSala status, Integer maxUsuarios, Boolean moderada,
                                  Integer usuariosOnline, Long totalMensagens, Long criadaPor,
                                  LocalDateTime ultimaAtividade, LocalDateTime dataCriacao) {
        return new SalaDto(id, nome, descricao, tipo, status, maxUsuarios, moderada,
                          usuariosOnline, totalMensagens, criadaPor, ultimaAtividade, dataCriacao);
    }
    
    /**
     * Verifica se a sala está ativa
     */
    public boolean isAtiva() {
        return status == StatusSala.ATIVA;
    }
    
    /**
     * Verifica se a sala é pública
     */
    public boolean isPublica() {
        return tipo != null && tipo.isAcessoLivre();
    }
    
    /**
     * Verifica se a sala é moderada
     */
    public boolean isModerada() {
        return Boolean.TRUE.equals(moderada);
    }
    
    /**
     * Verifica se a sala está cheia
     */
    public boolean isCheia() {
        return usuariosOnline != null && maxUsuarios != null && 
               usuariosOnline >= maxUsuarios;
    }
    
    /**
     * Verifica se a sala teve atividade recente
     */
    public boolean temAtividadeRecente() {
        return ultimaAtividade != null && 
               ultimaAtividade.isAfter(LocalDateTime.now().minusMinutes(30));
    }
    
    /**
     * Calcula percentual de ocupação
     */
    public double getPercentualOcupacao() {
        if (maxUsuarios == null || maxUsuarios == 0) {
            return 0.0;
        }
        return ((double) (usuariosOnline != null ? usuariosOnline : 0) / maxUsuarios) * 100.0;
    }
    
    /**
     * Obtém ícone do tipo de sala
     */
    public String getIconeTipo() {
        return tipo != null ? tipo.getIcone() : "💬";
    }
    
    /**
     * Obtém ícone do status da sala
     */
    public String getIconeStatus() {
        return status != null ? status.getIcone() : "🟢";
    }
    
    /**
     * Obtém cor/tema da sala
     */
    public String getCor() {
        return tipo != null ? tipo.getCor() : "primary";
    }
    
    /**
     * Verifica se pode aceitar novos usuários
     */
    public boolean podeAceitarNovoUsuario() {
        return isAtiva() && !isCheia();
    }
}