package br.tec.facilitaservicos.batepapo.dominio.entidade;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import br.tec.facilitaservicos.batepapo.dominio.enums.StatusMensagem;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoMensagem;

/**
 * Entidade R2DBC simplificada para Mensagem de Chat
 * Versão mínima viável focada no core das funcionalidades
 */
@Table("mensagens_chat")
public class MensagemSimplesR2dbc {
    
    @Id
    private Long id;
    
    @Column("conteudo")
    private String conteudo;
    
    @Column("usuario_id")
    private Long usuarioId;
    
    @Column("usuario_nome")
    private String usuarioNome;
    
    @Column("sala")
    private String sala;
    
    @Column("tipo")
    private TipoMensagem tipo;
    
    @Column("status")
    private StatusMensagem status;
    
    @Column("data_envio")
    private LocalDateTime dataEnvio;
    
    @Column("data_criacao")
    private LocalDateTime dataCriacao;
    
    // Construtores
    public MensagemSimplesR2dbc() {}
    
    public MensagemSimplesR2dbc(Long id, String conteudo, Long usuarioId, String usuarioNome, 
                               String sala, TipoMensagem tipo, StatusMensagem status, 
                               LocalDateTime dataEnvio, LocalDateTime dataCriacao) {
        this.id = id;
        this.conteudo = conteudo;
        this.usuarioId = usuarioId;
        this.usuarioNome = usuarioNome;
        this.sala = sala;
        this.tipo = tipo;
        this.status = status;
        this.dataEnvio = dataEnvio;
        this.dataCriacao = dataCriacao;
    }
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }
    
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    
    public String getUsuarioNome() { return usuarioNome; }
    public void setUsuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; }
    
    public String getSala() { return sala; }
    public void setSala(String sala) { this.sala = sala; }
    
    public TipoMensagem getTipo() { return tipo; }
    public void setTipo(TipoMensagem tipo) { this.tipo = tipo; }
    
    public StatusMensagem getStatus() { return status; }
    public void setStatus(StatusMensagem status) { this.status = status; }
    
    public LocalDateTime getDataEnvio() { return dataEnvio; }
    public void setDataEnvio(LocalDateTime dataEnvio) { this.dataEnvio = dataEnvio; }
    
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    
    /**
     * Cria nova mensagem para persistência
     */
    public static MensagemSimplesR2dbc nova(String conteudo, Long usuarioId, String usuarioNome, String sala) {
        MensagemSimplesR2dbc mensagem = new MensagemSimplesR2dbc();
        mensagem.setConteudo(conteudo);
        mensagem.setUsuarioId(usuarioId);
        mensagem.setUsuarioNome(usuarioNome);
        mensagem.setSala(sala);
        mensagem.setTipo(TipoMensagem.TEXTO);
        mensagem.setStatus(StatusMensagem.ENVIADA);
        mensagem.setDataEnvio(LocalDateTime.now());
        mensagem.setDataCriacao(LocalDateTime.now());
        return mensagem;
    }
    
    /**
     * Cria mensagem do sistema
     */
    public static MensagemSimplesR2dbc sistema(String conteudo, String sala) {
        MensagemSimplesR2dbc mensagem = new MensagemSimplesR2dbc();
        mensagem.setConteudo(conteudo);
        mensagem.setUsuarioId(-1L);
        mensagem.setUsuarioNome("Sistema");
        mensagem.setSala(sala);
        mensagem.setTipo(TipoMensagem.SISTEMA);
        mensagem.setStatus(StatusMensagem.ENVIADA);
        mensagem.setDataEnvio(LocalDateTime.now());
        mensagem.setDataCriacao(LocalDateTime.now());
        return mensagem;
    }
}