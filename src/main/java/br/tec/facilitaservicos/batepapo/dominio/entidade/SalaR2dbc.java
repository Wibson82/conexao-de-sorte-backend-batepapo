package br.tec.facilitaservicos.batepapo.dominio.entidade;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import br.tec.facilitaservicos.common.entity.ReactiveAuditableEntity;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusSala;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoSala;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entidade R2DBC reativa para Sala de Chat
 * Representa uma sala de bate-papo no sistema com suporte a operações reativas.
 * 
 * Inclui configurações de moderação, limites e metadados da sala.
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Table("salas_chat")
public class SalaR2dbc extends ReactiveAuditableEntity {

    @Id
    private Long id;

    @NotBlank(message = "Nome da sala é obrigatório")
    @Size(max = 50, message = "Nome da sala não pode exceder 50 caracteres")
    @Column("nome")
    private String nome;

    @Size(max = 200, message = "Descrição não pode exceder 200 caracteres")
    @Column("descricao")
    private String descricao;

    @NotNull(message = "Tipo da sala é obrigatório")
    @Column("tipo")
    private TipoSala tipo;

    @NotNull(message = "Status da sala é obrigatório")
    @Column("status")
    private StatusSala status;

    @Column("max_usuarios")
    private Integer maxUsuarios;

    @Column("moderada")
    private Boolean moderada = false;

    @Column("usuarios_online")
    private Integer usuariosOnline = 0;

    @Column("total_mensagens")
    private Long totalMensagens = 0L;

    @Column("criada_por")
    private Long criadaPor;

    @Column("ultima_atividade")
    private LocalDateTime ultimaAtividade;

    @Column("configuracoes")
    private String configuracoes; // JSON com configurações específicas

    // Construtores
    public SalaR2dbc() {
        super();
        this.tipo = TipoSala.PUBLICA;
        this.status = StatusSala.ATIVA;
        this.moderada = false;
        this.usuariosOnline = 0;
        this.totalMensagens = 0L;
        this.maxUsuarios = 100; // Padrão
        this.ultimaAtividade = LocalDateTime.now();
    }

    public SalaR2dbc(String nome, TipoSala tipo, Long criadaPor) {
        this();
        this.nome = validarCampoObrigatorio(nome, "Nome da sala");
        this.tipo = Objects.requireNonNull(tipo, "Tipo da sala é obrigatório");
        this.criadaPor = Objects.requireNonNull(criadaPor, "Criador da sala é obrigatório");
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = validarCampoObrigatorio(nome, "Nome da sala");
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao != null ? descricao.trim() : null;
    }

    public TipoSala getTipo() {
        return tipo;
    }

    public void setTipo(TipoSala tipo) {
        this.tipo = Objects.requireNonNull(tipo, "Tipo da sala é obrigatório");
    }

    public StatusSala getStatus() {
        return status;
    }

    public void setStatus(StatusSala status) {
        this.status = Objects.requireNonNull(status, "Status da sala é obrigatório");
    }

    public Integer getMaxUsuarios() {
        return maxUsuarios;
    }

    public void setMaxUsuarios(Integer maxUsuarios) {
        this.maxUsuarios = maxUsuarios != null && maxUsuarios > 0 ? maxUsuarios : 100;
    }

    public Boolean getModerada() {
        return moderada;
    }

    public void setModerada(Boolean moderada) {
        this.moderada = moderada != null ? moderada : false;
    }

    public Integer getUsuariosOnline() {
        return usuariosOnline;
    }

    public void setUsuariosOnline(Integer usuariosOnline) {
        this.usuariosOnline = usuariosOnline != null && usuariosOnline >= 0 ? usuariosOnline : 0;
    }

    public Long getTotalMensagens() {
        return totalMensagens;
    }

    public void setTotalMensagens(Long totalMensagens) {
        this.totalMensagens = totalMensagens != null && totalMensagens >= 0 ? totalMensagens : 0L;
    }

    public Long getCriadaPor() {
        return criadaPor;
    }

    public void setCriadaPor(Long criadaPor) {
        this.criadaPor = criadaPor;
    }

    public LocalDateTime getUltimaAtividade() {
        return ultimaAtividade;
    }

    public void setUltimaAtividade(LocalDateTime ultimaAtividade) {
        this.ultimaAtividade = ultimaAtividade;
    }

    public String getConfiguracoes() {
        return configuracoes;
    }

    public void setConfiguracoes(String configuracoes) {
        this.configuracoes = configuracoes;
    }

    // Métodos de negócio

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
        return tipo == TipoSala.PUBLICA;
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
     * Verifica se um usuário pode entrar na sala
     */
    public boolean podeEntrar(Long usuarioId) {
        if (!isAtiva()) {
            return false;
        }
        
        if (isCheia()) {
            return false;
        }
        
        // Verificações adicionais podem ser implementadas aqui
        // (banimento, permissões especiais, etc.)
        
        return true;
    }

    /**
     * Incrementa contador de usuários online
     */
    public void incrementarUsuariosOnline() {
        this.usuariosOnline = (usuariosOnline != null ? usuariosOnline : 0) + 1;
        atualizarUltimaAtividade();
    }

    /**
     * Decrementa contador de usuários online
     */
    public void decrementarUsuariosOnline() {
        if (usuariosOnline != null && usuariosOnline > 0) {
            this.usuariosOnline = usuariosOnline - 1;
        }
        atualizarUltimaAtividade();
    }

    /**
     * Incrementa contador de mensagens
     */
    public void incrementarTotalMensagens() {
        this.totalMensagens = (totalMensagens != null ? totalMensagens : 0L) + 1;
        atualizarUltimaAtividade();
    }

    /**
     * Atualiza timestamp da última atividade
     */
    public void atualizarUltimaAtividade() {
        this.ultimaAtividade = LocalDateTime.now();
    }

    /**
     * Verifica se a sala teve atividade recente
     */
    public boolean temAtividadeRecente(int minutos) {
        return ultimaAtividade != null && 
               ultimaAtividade.isAfter(LocalDateTime.now().minusMinutes(minutos));
    }

    /**
     * Ativa a sala
     */
    public void ativar() {
        this.status = StatusSala.ATIVA;
        atualizarUltimaAtividade();
    }

    /**
     * Desativa a sala
     */
    public void desativar() {
        this.status = StatusSala.INATIVA;
        this.usuariosOnline = 0;
    }

    /**
     * Obter chave para cache
     */
    public String getChaveCache() {
        return String.format("sala:%s", nome.toLowerCase());
    }

    /**
     * Obter sumário da sala para logs
     */
    public String getSumario() {
        return String.format("[%s] %s - %d usuários online, %d mensagens", 
            nome, tipo, usuariosOnline, totalMensagens);
    }

    // Métodos auxiliares

    /**
     * Valida campo obrigatório
     */
    private String validarCampoObrigatorio(String valor, String nomeCampo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(nomeCampo + " é obrigatório e não pode ser vazio");
        }
        return valor.trim();
    }

    // Builder Pattern para construção segura

    public static class Builder {
        private String nome;
        private String descricao;
        private TipoSala tipo = TipoSala.PUBLICA;
        private Integer maxUsuarios = 100;
        private Boolean moderada = false;
        private Long criadaPor;

        public Builder nome(String nome) {
            this.nome = nome;
            return this;
        }

        public Builder descricao(String descricao) {
            this.descricao = descricao;
            return this;
        }

        public Builder tipo(TipoSala tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder maxUsuarios(Integer maxUsuarios) {
            this.maxUsuarios = maxUsuarios;
            return this;
        }

        public Builder moderada(Boolean moderada) {
            this.moderada = moderada;
            return this;
        }

        public Builder criadaPor(Long criadaPor) {
            this.criadaPor = criadaPor;
            return this;
        }

        public SalaR2dbc build() {
            SalaR2dbc sala = new SalaR2dbc(nome, tipo, criadaPor);
            sala.setDescricao(descricao);
            sala.setMaxUsuarios(maxUsuarios);
            sala.setModerada(moderada);
            return sala;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Equals, HashCode e ToString

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SalaR2dbc that = (SalaR2dbc) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(nome, that.nome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nome);
    }

    @Override
    public String toString() {
        return "SalaR2dbc{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", tipo=" + tipo +
                ", status=" + status +
                ", usuariosOnline=" + usuariosOnline +
                ", totalMensagens=" + totalMensagens +
                ", dataCriacao=" + getDataCriacao() +
                '}';
    }
}