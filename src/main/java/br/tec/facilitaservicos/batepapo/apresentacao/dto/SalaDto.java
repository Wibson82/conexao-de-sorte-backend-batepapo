package br.tec.facilitaservicos.batepapo.apresentacao.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import br.tec.facilitaservicos.batepapo.dominio.enums.StatusSala;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoSala;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO reativo para Sala de Chat
 * 
 * @param id Identificador único da sala
 * @param nome Nome da sala
 * @param descricao Descrição da sala
 * @param tipo Tipo da sala
 * @param status Status da sala
 * @param maxUsuarios Máximo de usuários permitidos
 * @param moderada Se a sala é moderada
 * @param usuariosOnline Número atual de usuários online
 * @param totalMensagens Total de mensagens na sala
 * @param criadaPor ID do usuário que criou a sala
 * @param ultimaAtividade Data/hora da última atividade
 * @param dataCriacao Data de criação da sala
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SalaDto(
    Long id,
    
    @NotBlank(message = "Nome da sala é obrigatório")
    @Size(max = 50, message = "Nome da sala não pode exceder 50 caracteres")
    String nome,
    
    @Size(max = 200, message = "Descrição não pode exceder 200 caracteres")
    String descricao,
    
    @NotNull(message = "Tipo da sala é obrigatório")
    TipoSala tipo,
    
    StatusSala status,
    Integer maxUsuarios,
    Boolean moderada,
    Integer usuariosOnline,
    Long totalMensagens,
    Long criadaPor,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime ultimaAtividade,
    
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