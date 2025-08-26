package br.tec.facilitaservicos.batepapo.dominio.enums;

/**
 * Enum que representa os tipos de sala de chat
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public enum TipoSala {
    
    /**
     * Sala pública - todos podem entrar
     */
    PUBLICA("Pública"),
    
    /**
     * Sala privada - apenas usuários convidados
     */
    PRIVADA("Privada"),
    
    /**
     * Sala para discussão de resultados
     */
    RESULTADOS("Resultados"),
    
    /**
     * Sala para dicas e estratégias
     */
    DICAS("Dicas"),
    
    /**
     * Sala para suporte ao usuário
     */
    SUPORTE("Suporte"),
    
    /**
     * Sala geral para conversa livre
     */
    GERAL("Geral"),
    
    /**
     * Sala temporária/evento especial
     */
    TEMPORARIA("Temporária"),
    
    /**
     * Sala para moderadores apenas
     */
    MODERADORES("Moderadores");

    private final String descricao;

    TipoSala(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se qualquer usuário pode entrar na sala
     */
    public boolean isAcessoLivre() {
        return this == PUBLICA || this == RESULTADOS || this == DICAS || this == GERAL;
    }

    /**
     * Verifica se a sala requer permissões especiais
     */
    public boolean requerPermissaoEspecial() {
        return this == PRIVADA || this == SUPORTE || this == MODERADORES;
    }

    /**
     * Verifica se é sala do sistema (não criada por usuários)
     */
    public boolean isSalaSistema() {
        return this == RESULTADOS || this == DICAS || this == SUPORTE || 
               this == GERAL || this == MODERADORES;
    }

    /**
     * Verifica se usuários podem criar salas deste tipo
     */
    public boolean podeSerCriadaPorUsuario() {
        return this == PUBLICA || this == PRIVADA || this == TEMPORARIA;
    }

    /**
     * Verifica se a sala tem moderação automática
     */
    public boolean temModeracaoAutomatica() {
        return this == SUPORTE || this == MODERADORES;
    }

    /**
     * Obtém o número máximo padrão de usuários para este tipo de sala
     */
    public int getMaxUsuariosPadrao() {
        return switch (this) {
            case PUBLICA, GERAL, RESULTADOS -> 200;
            case DICAS -> 100;
            case SUPORTE -> 50;
            case PRIVADA, TEMPORARIA -> 20;
            case MODERADORES -> 10;
        };
    }

    /**
     * Obtém a cor/tema para exibição da sala
     */
    public String getCor() {
        return switch (this) {
            case PUBLICA, GERAL -> "primary";
            case RESULTADOS -> "success";
            case DICAS -> "info";
            case SUPORTE -> "warning";
            case PRIVADA -> "secondary";
            case TEMPORARIA -> "light";
            case MODERADORES -> "danger";
        };
    }

    /**
     * Obtém o ícone para o tipo de sala
     */
    public String getIcone() {
        return switch (this) {
            case PUBLICA -> "🌍";
            case PRIVADA -> "🔒";
            case RESULTADOS -> "🎰";
            case DICAS -> "💡";
            case SUPORTE -> "🆘";
            case GERAL -> "💬";
            case TEMPORARIA -> "⏰";
            case MODERADORES -> "👮";
        };
    }

    /**
     * Obtém a descrição estendida da sala
     */
    public String getDescricaoEstendida() {
        return switch (this) {
            case PUBLICA -> "Sala aberta para todos os usuários";
            case PRIVADA -> "Sala restrita para usuários convidados";
            case RESULTADOS -> "Discussão sobre resultados da loteria";
            case DICAS -> "Compartilhamento de dicas e estratégias";
            case SUPORTE -> "Atendimento e suporte aos usuários";
            case GERAL -> "Conversa livre sobre qualquer assunto";
            case TEMPORARIA -> "Sala criada para evento específico";
            case MODERADORES -> "Sala exclusiva para moderadores";
        };
    }

    /**
     * Obtém as salas padrão do sistema
     */
    public static TipoSala[] getSalasPadrao() {
        return new TipoSala[]{GERAL, RESULTADOS, DICAS, SUPORTE};
    }

    /**
     * Converte string para enum
     */
    public static TipoSala fromString(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return PUBLICA;
        }
        
        try {
            return TipoSala.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PUBLICA;
        }
    }

    @Override
    public String toString() {
        return descricao;
    }
}