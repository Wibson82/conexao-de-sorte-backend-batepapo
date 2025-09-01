package br.tec.facilitaservicos.batepapo.dominio.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enum que representa o status de presença de um usuário no chat
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public enum StatusPresenca {
    
    /**
     * Usuário ativo e online
     */
    @Schema(description = "Usuário ativo e online")
    ONLINE("Online"),
    
    /**
     * Usuário ausente/inativo mas ainda conectado
     */
    @Schema(description = "Usuário ausente/inativo mas ainda conectado")
    AUSENTE("Ausente"),
    
    /**
     * Usuário ocupado - não quer ser incomodado
     */
    @Schema(description = "Usuário ocupado - não quer ser incomodado")
    OCUPADO("Ocupado"),
    
    /**
     * Usuário invisível - aparece como offline para outros
     */
    @Schema(description = "Usuário invisível - aparece como offline para outros")
    INVISIVEL("Invisível"),
    
    /**
     * Usuário desconectado/offline
     */
    @Schema(description = "Usuário desconectado/offline")
    OFFLINE("Offline"),
    
    /**
     * Conexão perdida/timeout
     */
    @Schema(description = "Conexão perdida/timeout")
    DESCONECTADO("Desconectado");

    private final String descricao;

    StatusPresenca(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se o usuário está ativo
     */
    public boolean isAtivo() {
        return this == ONLINE;
    }

    /**
     * Verifica se o usuário está conectado (mas pode estar inativo)
     */
    public boolean isConectado() {
        return this == ONLINE || this == AUSENTE || this == OCUPADO;
    }

    /**
     * Verifica se o usuário está visível para outros
     */
    public boolean isVisivel() {
        return this != INVISIVEL && this != OFFLINE && this != DESCONECTADO;
    }

    /**
     * Verifica se o usuário pode receber mensagens
     */
    public boolean podeReceberMensagens() {
        return this == ONLINE || this == AUSENTE;
    }

    /**
     * Verifica se o usuário deve receber notificações
     */
    public boolean deveReceberNotificacoes() {
        return this == ONLINE || this == AUSENTE;
    }

    /**
     * Verifica se o status indica problemas de conexão
     */
    public boolean temProblemaConexao() {
        return this == DESCONECTADO;
    }

    /**
     * Obtém a cor/estilo para exibição do status
     */
    public String getCor() {
        return switch (this) {
            case ONLINE -> "success";
            case AUSENTE -> "warning";
            case OCUPADO -> "danger";
            case INVISIVEL -> "secondary";
            case OFFLINE, DESCONECTADO -> "muted";
        };
    }

    /**
     * Obtém o ícone para o status de presença
     */
    public String getIcone() {
        return switch (this) {
            case ONLINE -> "🟢";
            case AUSENTE -> "🟡";
            case OCUPADO -> "🔴";
            case INVISIVEL -> "👻";
            case OFFLINE -> "⚫";
            case DESCONECTADO -> "❌";
        };
    }

    /**
     * Obtém a descrição estendida do status
     */
    public String getDescricaoEstendida() {
        return switch (this) {
            case ONLINE -> "Disponível para chat";
            case AUSENTE -> "Temporariamente ausente";
            case OCUPADO -> "Ocupado - não incomodar";
            case INVISIVEL -> "Aparece como offline";
            case OFFLINE -> "Não está conectado";
            case DESCONECTADO -> "Perdeu a conexão";
        };
    }

    /**
     * Prioridade de exibição na lista de usuários (menor = mais prioritário)
     */
    public int getPrioridadeExibicao() {
        return switch (this) {
            case ONLINE -> 1;
            case OCUPADO -> 2;
            case AUSENTE -> 3;
            case INVISIVEL -> 4;
            case OFFLINE -> 5;
            case DESCONECTADO -> 6;
        };
    }

    /**
     * Verifica se o status é válido para transição
     */
    public boolean podeTransicionarPara(StatusPresenca novoStatus) {
        // Qualquer status pode ir para OFFLINE ou DESCONECTADO
        if (novoStatus == OFFLINE || novoStatus == DESCONECTADO) {
            return true;
        }
        
        // OFFLINE só pode ir para ONLINE
        if (this == OFFLINE) {
            return novoStatus == ONLINE;
        }
        
        // DESCONECTADO só pode ir para ONLINE
        if (this == DESCONECTADO) {
            return novoStatus == ONLINE;
        }
        
        // Outros status podem transicionar entre si
        return true;
    }

    /**
     * Converte string para enum
     */
    public static StatusPresenca fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            return ONLINE;
        }
        
        try {
            return StatusPresenca.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ONLINE;
        }
    }

    @Override
    public String toString() {
        return descricao;
    }
}