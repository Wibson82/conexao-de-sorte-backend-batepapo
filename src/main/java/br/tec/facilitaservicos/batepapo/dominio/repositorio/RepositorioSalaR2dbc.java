package br.tec.facilitaservicos.batepapo.dominio.repositorio;

import java.time.LocalDateTime;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.tec.facilitaservicos.batepapo.dominio.entidade.SalaR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusSala;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoSala;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositório R2DBC reativo para Sala de Chat
 * Fornece operações de acesso a dados reativas para salas
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Repository
public interface RepositorioSalaR2dbc extends R2dbcRepository<SalaR2dbc, Long> {

    /**
     * Busca sala por nome
     * @param nome Nome da sala
     * @return Mono com sala encontrada
     */
    Mono<SalaR2dbc> findByNome(String nome);

    /**
     * Busca salas por tipo
     * @param tipo Tipo da sala
     * @param pageable Paginação
     * @return Flux com salas do tipo
     */
    Flux<SalaR2dbc> findByTipoOrderByNome(TipoSala tipo, Pageable pageable);

    /**
     * Busca salas por status
     * @param status Status da sala
     * @param pageable Paginação
     * @return Flux com salas do status
     */
    Flux<SalaR2dbc> findByStatusOrderByNome(StatusSala status, Pageable pageable);

    /**
     * Busca salas ativas
     * @param pageable Paginação
     * @return Flux com salas ativas
     */
    @Query("SELECT * FROM salas_chat WHERE status = 'ATIVA' ORDER BY usuarios_online DESC, nome")
    Flux<SalaR2dbc> findSalasAtivas(Pageable pageable);

    /**
     * Busca salas públicas ativas
     * @param pageable Paginação
     * @return Flux com salas públicas ativas
     */
    @Query("SELECT * FROM salas_chat WHERE tipo IN ('PUBLICA', 'GERAL', 'RESULTADOS', 'DICAS') " +
           "AND status = 'ATIVA' ORDER BY usuarios_online DESC, nome")
    Flux<SalaR2dbc> findSalasPublicasAtivas(Pageable pageable);

    /**
     * Busca salas criadas por usuário
     * @param criadaPor ID do usuário criador
     * @param pageable Paginação
     * @return Flux com salas criadas pelo usuário
     */
    Flux<SalaR2dbc> findByCriadaPorOrderByDataCriacaoDesc(Long criadaPor, Pageable pageable);

    /**
     * Busca salas com atividade recente
     * @param dataLimite Data limite para considerar atividade recente
     * @param pageable Paginação
     * @return Flux com salas com atividade recente
     */
    @Query("SELECT * FROM salas_chat WHERE ultima_atividade >= :dataLimite AND status = 'ATIVA' " +
           "ORDER BY ultima_atividade DESC")
    Flux<SalaR2dbc> findSalasComAtividadeRecente(@Param("dataLimite") LocalDateTime dataLimite, Pageable pageable);

    /**
     * Busca salas por parte do nome (busca por texto)
     * @param texto Texto a buscar no nome
     * @param pageable Paginação
     * @return Flux com salas encontradas
     */
    @Query("SELECT * FROM salas_chat WHERE LOWER(nome) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "AND status = 'ATIVA' ORDER BY usuarios_online DESC")
    Flux<SalaR2dbc> buscarSalasPorNome(@Param("texto") String texto, Pageable pageable);

    /**
     * Conta salas por tipo
     * @param tipo Tipo da sala
     * @return Mono com contagem
     */
    @Query("SELECT COUNT(*) FROM salas_chat WHERE tipo = :tipo")
    Mono<Long> countByTipo(@Param("tipo") String tipo);

    /**
     * Conta salas por status
     * @param status Status da sala
     * @return Mono com contagem
     */
    @Query("SELECT COUNT(*) FROM salas_chat WHERE status = :status")
    Mono<Long> countByStatus(@Param("status") String status);

    /**
     * Conta salas ativas
     * @return Mono com contagem de salas ativas
     */
    @Query("SELECT COUNT(*) FROM salas_chat WHERE status = 'ATIVA'")
    Mono<Long> countSalasAtivas();

    /**
     * Verifica se nome da sala já existe
     * @param nome Nome da sala
     * @return Mono com true se existe
     */
    @Query("SELECT EXISTS(SELECT 1 FROM salas_chat WHERE LOWER(nome) = LOWER(:nome))")
    Mono<Boolean> existsByNome(@Param("nome") String nome);

    /**
     * Atualiza contador de usuários online
     * @param salaId ID da sala
     * @param novoValor Novo valor do contador
     * @return Mono com número de registros atualizados
     */
    @Query("UPDATE salas_chat SET usuarios_online = :novoValor, ultima_atividade = NOW() WHERE id = :salaId")
    Mono<Integer> atualizarUsuariosOnline(@Param("salaId") Long salaId, @Param("novoValor") Integer novoValor);

    /**
     * Incrementa contador de mensagens
     * @param salaId ID da sala
     * @return Mono com número de registros atualizados
     */
    @Query("UPDATE salas_chat SET total_mensagens = total_mensagens + 1, ultima_atividade = NOW() WHERE id = :salaId")
    Mono<Integer> incrementarTotalMensagens(@Param("salaId") Long salaId);

    /**
     * Atualiza última atividade da sala
     * @param salaId ID da sala
     * @return Mono com número de registros atualizados
     */
    @Query("UPDATE salas_chat SET ultima_atividade = NOW() WHERE id = :salaId")
    Mono<Integer> atualizarUltimaAtividade(@Param("salaId") Long salaId);

    /**
     * Busca salas ordenadas por popularidade
     * @param limite Limite de resultados
     * @return Flux com salas mais populares
     */
    @Query("SELECT * FROM salas_chat WHERE status = 'ATIVA' " +
           "ORDER BY usuarios_online DESC, total_mensagens DESC LIMIT :limite")
    Flux<SalaR2dbc> findSalasMaisPopulares(@Param("limite") Integer limite);

    /**
     * Busca salas com poucos usuários (para limpeza)
     * @param maxUsuarios Máximo de usuários
     * @param dataLimite Data limite de atividade
     * @return Flux com salas com poucos usuários
     */
    @Query("SELECT * FROM salas_chat WHERE usuarios_online <= :maxUsuarios AND " +
           "(ultima_atividade IS NULL OR ultima_atividade < :dataLimite) " +
           "AND tipo NOT IN ('GERAL', 'RESULTADOS', 'DICAS', 'SUPORTE') ORDER BY ultima_atividade")
    Flux<SalaR2dbc> findSalasComPoucosUsuarios(@Param("maxUsuarios") Integer maxUsuarios, 
                                              @Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Busca estatísticas de tipos de sala
     * @return Flux com estatísticas por tipo
     */
    @Query("SELECT tipo, COUNT(*) as total, AVG(usuarios_online) as media_usuarios " +
           "FROM salas_chat WHERE status = 'ATIVA' GROUP BY tipo ORDER BY total DESC")
    Flux<Object[]> findEstatisticasTiposSala();

    /**
     * Remove salas inativas e vazias
     * @param dataLimite Data limite de inatividade
     * @return Mono com número de salas removidas
     */
    @Query("DELETE FROM salas_chat WHERE usuarios_online = 0 AND " +
           "(ultima_atividade IS NULL OR ultima_atividade < :dataLimite) " +
           "AND tipo NOT IN ('GERAL', 'RESULTADOS', 'DICAS', 'SUPORTE')")
    Mono<Integer> removerSalasInativas(@Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Busca salas que precisam de moderação
     * @return Flux com salas para moderar
     */
    @Query("SELECT * FROM salas_chat WHERE moderada = true AND status = 'ATIVA' " +
           "ORDER BY usuarios_online DESC")
    Flux<SalaR2dbc> findSalasParaModeracao();

    /**
     * Busca total de usuários online no sistema
     * @return Mono com total de usuários online
     */
    @Query("SELECT COALESCE(SUM(usuarios_online), 0) FROM salas_chat WHERE status = 'ATIVA'")
    Mono<Long> countTotalUsuariosOnline();

    /**
     * Busca salas padrão do sistema
     * @return Flux com salas padrão
     */
    @Query("SELECT * FROM salas_chat WHERE tipo IN ('GERAL', 'RESULTADOS', 'DICAS', 'SUPORTE') " +
           "ORDER BY FIELD(tipo, 'GERAL', 'RESULTADOS', 'DICAS', 'SUPORTE')")
    Flux<SalaR2dbc> findSalasPadrao();

    /**
     * Conta total de salas no sistema
     * @return Mono com contagem total
     */
    @Query("SELECT COUNT(*) FROM salas_chat")
    Mono<Long> countTotal();

    /**
     * Busca sala mais antiga
     * @return Mono com sala mais antiga
     */
    @Query("SELECT * FROM salas_chat ORDER BY data_criacao ASC LIMIT 1")
    Mono<SalaR2dbc> findSalaMaisAntiga();

    /**
     * Busca sala criada mais recentemente
     * @return Mono com sala mais recente
     */
    @Query("SELECT * FROM salas_chat ORDER BY data_criacao DESC LIMIT 1")
    Mono<SalaR2dbc> findSalaMaisRecente();
}