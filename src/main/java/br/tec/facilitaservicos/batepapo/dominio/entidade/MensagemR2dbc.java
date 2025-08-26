package br.tec.facilitaservicos.batepapo.dominio.entidade;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import br.tec.facilitaservicos.batepapo.dominio.entidade.base.ReactiveAuditableEntity;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusMensagem;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoMensagem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entidade R2DBC reativa para Mensagem do Chat
 * Representa uma mensagem no sistema de bate-papo com suporte a operações reativas.
 * 
 * Inclui suporte a diferentes tipos de mensagem, status de entrega e metadados.
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Table("mensagens_chat")
public class MensagemR2dbc extends ReactiveAuditableEntity {

    @Id
    private Long id;

    @NotBlank(message = "Conteúdo da mensagem é obrigatório")
    @Size(max = 500, message = "Mensagem não pode exceder 500 caracteres")
    @Column("conteudo")
    private String conteudo;

    @NotNull(message = "ID do usuário é obrigatório")
    @Column("usuario_id")
    private Long usuarioId;

    @NotBlank(message = "Nome do usuário é obrigatório")
    @Column("usuario_nome")
    private String usuarioNome;

    @NotBlank(message = "Sala é obrigatória")
    @Column("sala")
    private String sala;

    @NotNull(message = "Tipo da mensagem é obrigatório")
    @Column("tipo")
    private TipoMensagem tipo;

    @NotNull(message = "Status da mensagem é obrigatório")
    @Column("status")
    private StatusMensagem status;

    @Column("resposta_para_id")
    private Long respostaParaId;

    @Column("editada")
    private Boolean editada = false;

    @Column("data_envio")
    private LocalDateTime dataEnvio;

    @Column("data_edicao")
    private LocalDateTime dataEdicao;

    @Column("ip_origem")
    private String ipOrigemString;

    @Column("user_agent")
    private String userAgent;

    // Construtores
    public MensagemR2dbc() {
        super();
        this.dataEnvio = LocalDateTime.now();
        this.tipo = TipoMensagem.TEXTO;
        this.status = StatusMensagem.ENVIADA;
        this.editada = false;
    }

    public MensagemR2dbc(String conteudo, Long usuarioId, String usuarioNome, String sala) {
        this();
        this.conteudo = validarCampoObrigatorio(conteudo, "Conteúdo");
        this.usuarioId = Objects.requireNonNull(usuarioId, "ID do usuário é obrigatório");
        this.usuarioNome = validarCampoObrigatorio(usuarioNome, "Nome do usuário");
        this.sala = validarCampoObrigatorio(sala, "Sala");
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = validarCampoObrigatorio(conteudo, "Conteúdo");
        
        // Marcar como editada se já existia
        if (this.id != null) {
            this.editada = true;
            this.dataEdicao = LocalDateTime.now();
        }
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = Objects.requireNonNull(usuarioId, "ID do usuário é obrigatório");
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = validarCampoObrigatorio(usuarioNome, "Nome do usuário");
    }

    public String getSala() {
        return sala;
    }

    public void setSala(String sala) {
        this.sala = validarCampoObrigatorio(sala, "Sala");
    }

    public TipoMensagem getTipo() {
        return tipo;
    }

    public void setTipo(TipoMensagem tipo) {
        this.tipo = tipo != null ? tipo : TipoMensagem.TEXTO;
    }

    public StatusMensagem getStatus() {
        return status;
    }

    public void setStatus(StatusMensagem status) {
        this.status = status != null ? status : StatusMensagem.ENVIADA;
    }

    public Long getRespostaParaId() {
        return respostaParaId;
    }

    public void setRespostaParaId(Long respostaParaId) {
        this.respostaParaId = respostaParaId;
    }

    public Boolean getEditada() {
        return editada;
    }

    public void setEditada(Boolean editada) {
        this.editada = editada != null ? editada : false;
    }

    public LocalDateTime getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(LocalDateTime dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    public LocalDateTime getDataEdicao() {
        return dataEdicao;
    }

    public void setDataEdicao(LocalDateTime dataEdicao) {
        this.dataEdicao = dataEdicao;
    }

    public String getIpOrigemString() {
        return ipOrigemString;
    }

    public void setIpOrigemString(String ipOrigemString) {
        this.ipOrigemString = ipOrigemString;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    // Métodos de negócio

    /**
     * Verifica se a mensagem é uma resposta
     */
    public boolean isResposta() {
        return respostaParaId != null;
    }

    /**
     * Verifica se a mensagem foi editada
     */
    public boolean foiEditada() {
        return Boolean.TRUE.equals(editada);
    }

    /**
     * Verifica se a mensagem é do sistema
     */
    public boolean isMensagemSistema() {
        return tipo == TipoMensagem.SISTEMA;
    }

    /**
     * Verifica se a mensagem pode ser editada
     */
    public boolean podeSerEditada() {
        if (isMensagemSistema()) {
            return false;
        }
        
        // Permitir edição até 15 minutos após o envio
        return dataEnvio != null && 
               dataEnvio.isAfter(LocalDateTime.now().minusMinutes(15));
    }

    /**
     * Verifica se a mensagem é recente (últimos 5 minutos)
     */
    public boolean isRecente() {
        return dataEnvio != null && 
               dataEnvio.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    /**
     * Marca mensagem como lida
     */
    public void marcarComoLida() {
        if (this.status == StatusMensagem.ENVIADA) {
            this.status = StatusMensagem.LIDA;
        }
    }

    /**
     * Obter chave para cache/streams
     */
    public String getChaveCache() {
        return String.format("msg:%s:%d", sala, id != null ? id : 0);
    }

    /**
     * Obter sumário da mensagem para logs
     */
    public String getSumario() {
        String conteudoLimitado = conteudo.length() > 50 
            ? conteudo.substring(0, 47) + "..."
            : conteudo;
        
        return String.format("[%s] %s: %s", sala, usuarioNome, conteudoLimitado);
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
        private String conteudo;
        private Long usuarioId;
        private String usuarioNome;
        private String sala;
        private TipoMensagem tipo = TipoMensagem.TEXTO;
        private Long respostaParaId;
        private String ipOrigemString;
        private String userAgent;

        public Builder conteudo(String conteudo) {
            this.conteudo = conteudo;
            return this;
        }

        public Builder usuario(Long usuarioId, String usuarioNome) {
            this.usuarioId = usuarioId;
            this.usuarioNome = usuarioNome;
            return this;
        }

        public Builder sala(String sala) {
            this.sala = sala;
            return this;
        }

        public Builder tipo(TipoMensagem tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder respostaParaId(Long respostaParaId) {
            this.respostaParaId = respostaParaId;
            return this;
        }

        public Builder metadados(String ipOrigemString, String userAgent) {
            this.ipOrigemString = ipOrigemString;
            this.userAgent = userAgent;
            return this;
        }

        public MensagemR2dbc build() {
            MensagemR2dbc mensagem = new MensagemR2dbc(conteudo, usuarioId, usuarioNome, sala);
            mensagem.setTipo(tipo);
            mensagem.setRespostaParaId(respostaParaId);
            mensagem.setIpOrigemString(ipOrigemString);
            mensagem.setUserAgent(userAgent);
            return mensagem;
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
        MensagemR2dbc that = (MensagemR2dbc) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(usuarioId, that.usuarioId) &&
               Objects.equals(sala, that.sala) &&
               Objects.equals(dataEnvio, that.dataEnvio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, usuarioId, sala, dataEnvio);
    }

    @Override
    public String toString() {
        return "MensagemR2dbc{" +
                "id=" + id +
                ", usuarioNome='" + usuarioNome + '\'' +
                ", sala='" + sala + '\'' +
                ", tipo=" + tipo +
                ", editada=" + editada +
                ", dataEnvio=" + dataEnvio +
                ", dataCriacao=" + getDataCriacao() +
                '}';
    }
}