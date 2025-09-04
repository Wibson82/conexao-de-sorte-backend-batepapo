package br.tec.facilitaservicos.batepapo.dominio.entidade;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import br.tec.facilitaservicos.common.entity.ReactiveAuditableEntity;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusPresenca;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entidade R2DBC reativa para Usuário Online no Chat
 * Representa a presença de um usuário em uma sala de bate-papo.
 * 
 * Controla conexões ativas, heartbeats e status de presença.
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Table("usuarios_online_chat")
public class UsuarioOnlineR2dbc extends ReactiveAuditableEntity {

    @Id
    private Long id;

    @NotNull(message = "ID do usuário é obrigatório")
    @Column("usuario_id")
    private Long usuarioId;

    @NotBlank(message = "Nome do usuário é obrigatório")
    @Column("usuario_nome")
    private String usuarioNome;

    @NotBlank(message = "Sala é obrigatória")
    @Column("sala")
    private String sala;

    @NotNull(message = "Status de presença é obrigatório")
    @Column("status_presenca")
    private StatusPresenca statusPresenca;

    @NotBlank(message = "ID da sessão é obrigatório")
    @Column("session_id")
    private String sessionId;

    @Column("connection_id")
    private String connectionId;

    @Column("data_entrada")
    private LocalDateTime dataEntrada;

    @Column("ultimo_heartbeat")
    private LocalDateTime ultimoHeartbeat;

    @Column("data_saida")
    private LocalDateTime dataSaida;

    @Column("ip_origem")
    private String ipOrigemString;

    @Column("user_agent")
    private String userAgent;

    @Column("dispositivo")
    private String dispositivo;

    // Construtores
    public UsuarioOnlineR2dbc() {
        super();
        this.dataEntrada = LocalDateTime.now();
        this.ultimoHeartbeat = LocalDateTime.now();
        this.statusPresenca = StatusPresenca.ONLINE;
    }

    public UsuarioOnlineR2dbc(Long usuarioId, String usuarioNome, String sala, String sessionId) {
        this();
        this.usuarioId = Objects.requireNonNull(usuarioId, "ID do usuário é obrigatório");
        this.usuarioNome = validarCampoObrigatorio(usuarioNome, "Nome do usuário");
        this.sala = validarCampoObrigatorio(sala, "Sala");
        this.sessionId = validarCampoObrigatorio(sessionId, "ID da sessão");
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public StatusPresenca getStatusPresenca() {
        return statusPresenca;
    }

    public void setStatusPresenca(StatusPresenca statusPresenca) {
        this.statusPresenca = statusPresenca != null ? statusPresenca : StatusPresenca.ONLINE;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = validarCampoObrigatorio(sessionId, "ID da sessão");
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public LocalDateTime getDataEntrada() {
        return dataEntrada;
    }

    public void setDataEntrada(LocalDateTime dataEntrada) {
        this.dataEntrada = dataEntrada;
    }

    public LocalDateTime getUltimoHeartbeat() {
        return ultimoHeartbeat;
    }

    public void setUltimoHeartbeat(LocalDateTime ultimoHeartbeat) {
        this.ultimoHeartbeat = ultimoHeartbeat;
    }

    public LocalDateTime getDataSaida() {
        return dataSaida;
    }

    public void setDataSaida(LocalDateTime dataSaida) {
        this.dataSaida = dataSaida;
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

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }

    // Métodos de negócio

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
     * Verifica se a conexão ainda está ativa baseada no heartbeat
     */
    public boolean isConexaoAtiva(int timeoutMinutos) {
        return ultimoHeartbeat != null && 
               ultimoHeartbeat.isAfter(LocalDateTime.now().minusMinutes(timeoutMinutos));
    }

    /**
     * Atualiza heartbeat para manter conexão ativa
     */
    public void atualizarHeartbeat() {
        this.ultimoHeartbeat = LocalDateTime.now();
        
        // Se estava ausente, volta para online
        if (this.statusPresenca == StatusPresenca.AUSENTE) {
            this.statusPresenca = StatusPresenca.ONLINE;
        }
    }

    /**
     * Marca usuário como ausente
     */
    public void marcarComoAusente() {
        this.statusPresenca = StatusPresenca.AUSENTE;
    }

    /**
     * Marca usuário como saído da sala
     */
    public void marcarComoSaiu() {
        this.statusPresenca = StatusPresenca.OFFLINE;
        this.dataSaida = LocalDateTime.now();
    }

    /**
     * Calcula tempo online em minutos
     */
    public long getTempoOnlineMinutos() {
        LocalDateTime fim = dataSaida != null ? dataSaida : LocalDateTime.now();
        if (dataEntrada == null) {
            return 0;
        }
        
        return java.time.Duration.between(dataEntrada, fim).toMinutes();
    }

    /**
     * Verifica se é uma nova sessão (entrou há pouco tempo)
     */
    public boolean isNovaSessao() {
        return dataEntrada != null && 
               dataEntrada.isAfter(LocalDateTime.now().minusMinutes(2));
    }

    /**
     * Obter chave para cache
     */
    public String getChaveCache() {
        return String.format("online:%s:%d:%s", sala, usuarioId, sessionId);
    }

    /**
     * Obter chave para presença por sala
     */
    public String getChavePresencaSala() {
        return String.format("presenca:%s", sala);
    }

    /**
     * Obter sumário da sessão para logs
     */
    public String getSumario() {
        return String.format("[%s] %s (%s) - %d min online", 
            sala, usuarioNome, statusPresenca, getTempoOnlineMinutos());
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
        private Long usuarioId;
        private String usuarioNome;
        private String sala;
        private String sessionId;
        private String connectionId;
        private String ipOrigemString;
        private String userAgent;
        private String dispositivo;

        public Builder usuario(Long usuarioId, String usuarioNome) {
            this.usuarioId = usuarioId;
            this.usuarioNome = usuarioNome;
            return this;
        }

        public Builder sala(String sala) {
            this.sala = sala;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder connectionId(String connectionId) {
            this.connectionId = connectionId;
            return this;
        }

        public Builder metadados(String ipOrigemString, String userAgent, String dispositivo) {
            this.ipOrigemString = ipOrigemString;
            this.userAgent = userAgent;
            this.dispositivo = dispositivo;
            return this;
        }

        public UsuarioOnlineR2dbc build() {
            UsuarioOnlineR2dbc usuario = new UsuarioOnlineR2dbc(usuarioId, usuarioNome, sala, sessionId);
            usuario.setConnectionId(connectionId);
            usuario.setIpOrigemString(ipOrigemString);
            usuario.setUserAgent(userAgent);
            usuario.setDispositivo(dispositivo);
            return usuario;
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
        UsuarioOnlineR2dbc that = (UsuarioOnlineR2dbc) o;
        return Objects.equals(usuarioId, that.usuarioId) &&
               Objects.equals(sala, that.sala) &&
               Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, sala, sessionId);
    }

    @Override
    public String toString() {
        return "UsuarioOnlineR2dbc{" +
                "id=" + id +
                ", usuarioNome='" + usuarioNome + '\'' +
                ", sala='" + sala + '\'' +
                ", statusPresenca=" + statusPresenca +
                ", dataEntrada=" + dataEntrada +
                ", ultimoHeartbeat=" + ultimoHeartbeat +
                '}';
    }
}