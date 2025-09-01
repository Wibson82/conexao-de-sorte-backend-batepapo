package br.tec.facilitaservicos.batepapo.dominio.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enum que representa os tipos de mensagem do chat
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public enum TipoMensagem {
    
    /**
     * Mensagem de texto normal do usuário
     */
    @Schema(description = "Mensagem de texto normal do usuário")
    TEXTO("Texto"),
    
    /**
     * Mensagem do sistema (notificações automáticas)
     */
    @Schema(description = "Mensagem do sistema (notificações automáticas)")
    SISTEMA("Sistema"),
    
    /**
     * Mensagem de entrada de usuário na sala
     */
    @Schema(description = "Mensagem de entrada de usuário na sala")
    ENTRADA("Entrada"),
    
    /**
     * Mensagem de saída de usuário da sala
     */
    @Schema(description = "Mensagem de saída de usuário da sala")
    SAIDA("Saída"),
    
    /**
     * Mensagem de moderação (avisos, banimentos, etc.)
     */
    @Schema(description = "Mensagem de moderação (avisos, banimentos, etc.)")
    MODERACAO("Moderação"),
    
    /**
     * Mensagem com resultado de loteria
     */
    @Schema(description = "Mensagem com resultado de loteria")
    RESULTADO("Resultado"),
    
    /**
     * Mensagem com dica ou sugestão
     */
    @Schema(description = "Mensagem com dica ou sugestão")
    DICA("Dica"),
    
    /**
     * Mensagem de comando administrativo
     */
    @Schema(description = "Mensagem de comando administrativo")
    COMANDO("Comando");

    private final String descricao;

    TipoMensagem(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se é mensagem do usuário (não sistema)
     */
    public boolean isMensagemUsuario() {
        return this == TEXTO;
    }

    /**
     * Verifica se é mensagem automática do sistema
     */
    public boolean isMensagemSistema() {
        return this == SISTEMA || this == ENTRADA || this == SAIDA || 
               this == MODERACAO || this == RESULTADO || this == COMANDO;
    }

    /**
     * Verifica se é mensagem que deve ser persistida no histórico
     */
    public boolean deveSerPersistida() {
        return this == TEXTO || this == RESULTADO || this == DICA;
    }

    /**
     * Verifica se é mensagem que deve ser enviada para todos os usuários
     */
    public boolean deveSerBroadcast() {
        return true; // Todas as mensagens são enviadas por broadcast
    }

    /**
     * Obtém o ícone/emoji para o tipo de mensagem
     */
    public String getIcone() {
        return switch (this) {
            case TEXTO -> "💬";
            case SISTEMA -> "⚙️";
            case ENTRADA -> "👋";
            case SAIDA -> "👋";
            case MODERACAO -> "🔨";
            case RESULTADO -> "🎰";
            case DICA -> "💡";
            case COMANDO -> "⚡";
        };
    }

    /**
     * Converte string para enum
     */
    public static TipoMensagem fromString(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return TEXTO;
        }
        
        try {
            return TipoMensagem.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TEXTO;
        }
    }

    @Override
    public String toString() {
        return descricao;
    }
}