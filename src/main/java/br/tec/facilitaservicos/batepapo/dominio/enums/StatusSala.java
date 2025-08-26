package br.tec.facilitaservicos.batepapo.dominio.enums;

/**
 * Enum que representa o status de uma sala de chat
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public enum StatusSala {
    
    /**
     * Sala ativa e disponível para uso
     */
    ATIVA("Ativa"),
    
    /**
     * Sala temporariamente inativa
     */
    INATIVA("Inativa"),
    
    /**
     * Sala em manutenção
     */
    MANUTENCAO("Manutenção"),
    
    /**
     * Sala arquivada (não aceita novas mensagens)
     */
    ARQUIVADA("Arquivada"),
    
    /**
     * Sala suspensa por violação de regras
     */
    SUSPENSA("Suspensa"),
    
    /**
     * Sala excluída permanentemente
     */
    EXCLUIDA("Excluída");

    private final String descricao;

    StatusSala(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se a sala está disponível para uso
     */
    public boolean isDisponivel() {
        return this == ATIVA;
    }

    /**
     * Verifica se usuários podem entrar na sala
     */
    public boolean aceitaNovasConexoes() {
        return this == ATIVA;
    }

    /**
     * Verifica se a sala aceita novas mensagens
     */
    public boolean aceitaNovasMensagens() {
        return this == ATIVA;
    }

    /**
     * Verifica se a sala está temporariamente indisponível
     */
    public boolean isTemporariamenteIndisponivel() {
        return this == INATIVA || this == MANUTENCAO;
    }

    /**
     * Verifica se a sala está permanentemente indisponível
     */
    public boolean isPermanentementeIndisponivel() {
        return this == ARQUIVADA || this == SUSPENSA || this == EXCLUIDA;
    }

    /**
     * Verifica se a sala pode ser reativada
     */
    public boolean podeSerReativada() {
        return this == INATIVA || this == MANUTENCAO || this == SUSPENSA;
    }

    /**
     * Verifica se o histórico da sala está acessível
     */
    public boolean historicoAcessivel() {
        return this != EXCLUIDA;
    }

    /**
     * Verifica se a sala ainda aparece nas listagens
     */
    public boolean apareceNasListagens() {
        return this == ATIVA || this == INATIVA || this == MANUTENCAO;
    }

    /**
     * Obtém a cor/estilo para exibição do status
     */
    public String getCor() {
        return switch (this) {
            case ATIVA -> "success";
            case INATIVA -> "secondary";
            case MANUTENCAO -> "warning";
            case ARQUIVADA -> "info";
            case SUSPENSA -> "danger";
            case EXCLUIDA -> "dark";
        };
    }

    /**
     * Obtém o ícone para o status
     */
    public String getIcone() {
        return switch (this) {
            case ATIVA -> "🟢";
            case INATIVA -> "⚫";
            case MANUTENCAO -> "🔧";
            case ARQUIVADA -> "📦";
            case SUSPENSA -> "⛔";
            case EXCLUIDA -> "🗑️";
        };
    }

    /**
     * Obtém a mensagem explicativa para o status
     */
    public String getMensagemStatus() {
        return switch (this) {
            case ATIVA -> "Sala funcionando normalmente";
            case INATIVA -> "Sala temporariamente desabilitada";
            case MANUTENCAO -> "Sala em manutenção";
            case ARQUIVADA -> "Sala arquivada - somente leitura";
            case SUSPENSA -> "Sala suspensa por violação de regras";
            case EXCLUIDA -> "Sala excluída permanentemente";
        };
    }

    /**
     * Converte string para enum
     */
    public static StatusSala fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            return ATIVA;
        }
        
        try {
            return StatusSala.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ATIVA;
        }
    }

    @Override
    public String toString() {
        return descricao;
    }
}