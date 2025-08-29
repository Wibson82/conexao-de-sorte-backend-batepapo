package br.tec.facilitaservicos.batepapo.dominio.enums;

/**
 * Enum que representa o status de uma mensagem no chat
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public enum StatusMensagem {
    
    /**
     * Mensagem foi enviada com sucesso
     */
    ENVIADA("Enviada"),
    
    /**
     * Mensagem foi entregue aos destinatários
     */
    ENTREGUE("Entregue"),
    
    /**
     * Mensagem foi lida por pelo menos um destinatário
     */
    LIDA("Lida"),
    
    /**
     * Mensagem falhou no envio
     */
    ERRO("Erro"),
    
    /**
     * Mensagem foi moderada (oculta/censurada)
     */
    MODERADA("Moderada"),
    
    /**
     * Mensagem foi excluída pelo usuário ou moderador
     */
    EXCLUIDA("Excluída"),
    
    /**
     * Mensagem está em fila para processamento
     */
    PENDENTE("Pendente"),
    
    /**
     * Mensagem removida por moderação
     */
    REMOVIDA_MODERACAO("Removida por moderação"),
    
    /**
     * Mensagem em quarentena (aguardando moderação)
     */
    QUARENTENA("Em quarentena");

    private final String descricao;

    StatusMensagem(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se a mensagem foi entregue com sucesso
     */
    public boolean foiEntregue() {
        return this == ENVIADA || this == ENTREGUE || this == LIDA;
    }

    /**
     * Verifica se a mensagem está visível para os usuários
     */
    public boolean isVisivel() {
        return this == ENVIADA || this == ENTREGUE || this == LIDA;
    }

    /**
     * Verifica se a mensagem teve algum problema
     */
    public boolean temProblema() {
        return this == ERRO || this == MODERADA || this == EXCLUIDA;
    }

    /**
     * Verifica se a mensagem ainda está sendo processada
     */
    public boolean isProcessando() {
        return this == PENDENTE;
    }

    /**
     * Verifica se a mensagem pode ser editada
     */
    public boolean podeSerEditada() {
        return this == ENVIADA || this == ENTREGUE;
    }

    /**
     * Verifica se a mensagem pode ser excluída
     */
    public boolean podeSerExcluida() {
        return this != EXCLUIDA && this != MODERADA;
    }

    /**
     * Obtém a cor/estilo para exibição do status
     */
    public String getCor() {
        return switch (this) {
            case ENVIADA, ENTREGUE, LIDA -> "success";
            case ERRO -> "danger";
            case MODERADA, EXCLUIDA, REMOVIDA_MODERACAO -> "warning";
            case PENDENTE, QUARENTENA -> "info";
        };
    }

    /**
     * Obtém o ícone para o status
     */
    public String getIcone() {
        return switch (this) {
            case ENVIADA -> "✓";
            case ENTREGUE -> "✓✓";
            case LIDA -> "👁️";
            case ERRO -> "❌";
            case MODERADA, REMOVIDA_MODERACAO -> "🔒";
            case EXCLUIDA -> "🗑️";
            case PENDENTE -> "⏳";
            case QUARENTENA -> "⚠️";
        };
    }

    /**
     * Próximo status na sequência normal de entrega
     */
    public StatusMensagem proximoStatus() {
        return switch (this) {
            case PENDENTE -> ENVIADA;
            case ENVIADA -> ENTREGUE;
            case ENTREGUE -> LIDA;
            default -> this; // Mantém o status atual
        };
    }

    /**
     * Converte string para enum
     */
    public static StatusMensagem fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            return ENVIADA;
        }
        
        try {
            return StatusMensagem.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ENVIADA;
        }
    }

    @Override
    public String toString() {
        return descricao;
    }
}