package br.tec.facilitaservicos.batepapo.dominio.entidade.base;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * Entidade base auditável para R2DBC reativo
 * Fornece campos de auditoria automática para todas as entidades do chat
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public abstract class ReactiveAuditableEntity {

    @CreatedDate
    private LocalDateTime dataCriacao;

    @LastModifiedDate
    private LocalDateTime dataModificacao;

    // Constructors
    public ReactiveAuditableEntity() {
        // Constructor padrão
    }

    // Getters e Setters
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataModificacao() {
        return dataModificacao;
    }

    public void setDataModificacao(LocalDateTime dataModificacao) {
        this.dataModificacao = dataModificacao;
    }

    // Métodos de conveniência

    /**
     * Verifica se a entidade foi modificada recentemente
     * @param minutosAtras Número de minutos para considerar como "recente"
     * @return true se foi modificada nos últimos X minutos
     */
    public boolean foiModificadaRecentemente(int minutosAtras) {
        return dataModificacao != null && 
               dataModificacao.isAfter(LocalDateTime.now().minusMinutes(minutosAtras));
    }

    /**
     * Verifica se a entidade foi criada recentemente
     * @param minutosAtras Número de minutos para considerar como "recente"
     * @return true se foi criada nos últimos X minutos
     */
    public boolean foiCriadaRecentemente(int minutosAtras) {
        return dataCriacao != null && 
               dataCriacao.isAfter(LocalDateTime.now().minusMinutes(minutosAtras));
    }

    /**
     * Verifica se a entidade é nova (não foi persistida ainda)
     * @return true se ainda não foi salva no banco
     */
    public boolean isNova() {
        return dataCriacao == null;
    }
}