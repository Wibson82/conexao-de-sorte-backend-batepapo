package br.tec.facilitaservicos.batepapo.dominio.repositorio;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import br.tec.facilitaservicos.batepapo.dominio.entidade.MensagemSimplesR2dbc;
import reactor.core.publisher.Flux;

/**
 * Repositório R2DBC simplificado para Mensagens
 * Foca apenas nas operações essenciais do chat
 */
@Repository
public interface RepositorioMensagemSimplesR2dbc extends ReactiveCrudRepository<MensagemSimplesR2dbc, Long> {
    
    /**
     * Busca mensagens de uma sala específica ordenadas por data
     */
    @Query("SELECT * FROM mensagens_chat WHERE sala = :sala ORDER BY data_envio DESC LIMIT :limit")
    Flux<MensagemSimplesR2dbc> findBySalaOrderByDataEnvioDesc(String sala, int limit);
    
    /**
     * Busca mensagens recentes de uma sala (últimas 50)
     */
    @Query("SELECT * FROM mensagens_chat WHERE sala = :sala ORDER BY data_envio DESC LIMIT 50")
    Flux<MensagemSimplesR2dbc> findMensagensRecentes(String sala);
    
    /**
     * Conta mensagens de uma sala
     */
    @Query("SELECT COUNT(*) FROM mensagens_chat WHERE sala = :sala")
    reactor.core.publisher.Mono<Long> countBySala(String sala);
}