package br.tec.facilitaservicos.batepapo.dominio.repositorio;

import java.time.LocalDateTime;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.tec.facilitaservicos.batepapo.dominio.entidade.UsuarioOnlineR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusPresenca;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositório R2DBC reativo para Usuário Online no Chat
 * Fornece operações de acesso a dados reativas para presença de usuários
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Repository
public interface RepositorioUsuarioOnlineR2dbc extends R2dbcRepository<UsuarioOnlineR2dbc, Long> {

    /**
     * Busca usuário online por sessão
     * @param sessionId ID da sessão
     * @return Mono com usuário encontrado
     */
    Mono<UsuarioOnlineR2dbc> findBySessionId(String sessionId);

    /**
     * Busca usuário online por usuário e sala
     * @param usuarioId ID do usuário
     * @param sala Nome da sala
     * @return Mono com usuário encontrado
     */
    Mono<UsuarioOnlineR2dbc> findByUsuarioIdAndSala(Long usuarioId, String sala);

    /**
     * Busca todos os usuários online em uma sala
     * @param sala Nome da sala
     * @param pageable Paginação
     * @return Flux com usuários online na sala
     */
    @Query("SELECT * FROM usuarios_online_chat WHERE sala = :sala AND status_presenca IN ('ONLINE', 'AUSENTE', 'OCUPADO') " +
           "ORDER BY status_presenca, usuario_nome")
    Flux<UsuarioOnlineR2dbc> findUsuariosOnlineSala(@Param("sala") String sala, Pageable pageable);

    /**
     * Busca usuários por status de presença em uma sala
     * @param sala Nome da sala
     * @param statusPresenca Status de presença
     * @param pageable Paginação
     * @return Flux com usuários do status
     */
    Flux<UsuarioOnlineR2dbc> findBySalaAndStatusPresencaOrderByUsuarioNome(String sala, 
                                                                           StatusPresenca statusPresenca, 
                                                                           Pageable pageable);

    /**
     * Busca todas as sessões de um usuário (múltiplas conexões)
     * @param usuarioId ID do usuário
     * @return Flux com sessões do usuário
     */
    Flux<UsuarioOnlineR2dbc> findByUsuarioIdOrderByDataEntradaDesc(Long usuarioId);

    /**
     * Busca conexões ativas baseadas no heartbeat
     * @param dataLimite Data limite do último heartbeat
     * @return Flux com conexões ativas
     */
    @Query("SELECT * FROM usuarios_online_chat WHERE ultimo_heartbeat >= :dataLimite " +
           "AND status_presenca IN ('ONLINE', 'AUSENTE', 'OCUPADO')")
    Flux<UsuarioOnlineR2dbc> findConexoesAtivas(@Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Busca conexões expiradas (sem heartbeat)
     * @param dataLimite Data limite do último heartbeat
     * @return Flux com conexões expiradas
     */
    @Query("SELECT * FROM usuarios_online_chat WHERE ultimo_heartbeat < :dataLimite " +
           "AND status_presenca IN ('ONLINE', 'AUSENTE', 'OCUPADO')")
    Flux<UsuarioOnlineR2dbc> findConexoesExpiradas(@Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Conta usuários online em uma sala
     * @param sala Nome da sala
     * @return Mono com contagem de usuários online
     */
    @Query("SELECT COUNT(*) FROM usuarios_online_chat WHERE sala = :sala " +
           "AND status_presenca IN ('ONLINE', 'AUSENTE', 'OCUPADO')")
    Mono<Long> countUsuariosOnlineSala(@Param("sala") String sala);

    /**
     * Conta total de usuários online no sistema
     * @return Mono com contagem total
     */
    @Query("SELECT COUNT(DISTINCT usuario_id) FROM usuarios_online_chat " +
           "WHERE status_presenca IN ('ONLINE', 'AUSENTE', 'OCUPADO')")
    Mono<Long> countTotalUsuariosOnline();

    /**
     * Verifica se usuário está online em uma sala
     * @param usuarioId ID do usuário
     * @param sala Nome da sala
     * @return Mono com true se está online
     */
    @Query("SELECT EXISTS(SELECT 1 FROM usuarios_online_chat WHERE usuario_id = :usuarioId " +
           "AND sala = :sala AND status_presenca IN ('ONLINE', 'AUSENTE', 'OCUPADO'))")
    Mono<Boolean> isUsuarioOnlineSala(@Param("usuarioId") Long usuarioId, @Param("sala") String sala);

    /**
     * Busca estatísticas de presença por sala
     * @param sala Nome da sala
     * @return Flux com estatísticas de status
     */
    @Query("SELECT status_presenca, COUNT(*) as total FROM usuarios_online_chat " +
           "WHERE sala = :sala GROUP BY status_presenca")
    Flux<Object[]> findEstatisticasPresencaSala(@Param("sala") String sala);

    /**
     * Atualiza heartbeat de uma sessão
     * @param sessionId ID da sessão
     * @param novoHeartbeat Novo timestamp do heartbeat
     * @return Mono com número de registros atualizados
     */
    @Query("UPDATE usuarios_online_chat SET ultimo_heartbeat = :novoHeartbeat, " +
           "status_presenca = CASE WHEN status_presenca = 'AUSENTE' THEN 'ONLINE' ELSE status_presenca END " +
           "WHERE session_id = :sessionId")
    Mono<Integer> atualizarHeartbeat(@Param("sessionId") String sessionId, 
                                    @Param("novoHeartbeat") LocalDateTime novoHeartbeat);

    /**
     * Marca usuário como saído da sala
     * @param sessionId ID da sessão
     * @return Mono com número de registros atualizados
     */
    @Query("UPDATE usuarios_online_chat SET status_presenca = 'OFFLINE', data_saida = NOW() " +
           "WHERE session_id = :sessionId")
    Mono<Integer> marcarComoSaiu(@Param("sessionId") String sessionId);

    /**
     * Marca conexões expiradas como desconectadas
     * @param dataLimite Data limite do heartbeat
     * @return Mono com número de conexões marcadas
     */
    @Query("UPDATE usuarios_online_chat SET status_presenca = 'DESCONECTADO', data_saida = NOW() " +
           "WHERE ultimo_heartbeat < :dataLimite AND status_presenca IN ('ONLINE', 'AUSENTE', 'OCUPADO')")
    Mono<Integer> marcarConexoesExpiradas(@Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Remove registros de sessões muito antigas
     * @param dataLimite Data limite para remoção
     * @return Mono com número de registros removidos
     */
    @Query("DELETE FROM usuarios_online_chat WHERE data_saida < :dataLimite")
    Mono<Integer> removerSessoesAntigas(@Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Busca usuários mais ativos (mais tempo online)
     * @param limite Limite de resultados
     * @return Flux com usuários mais ativos
     */
    @Query("SELECT usuario_id, usuario_nome, SUM(TIMESTAMPDIFF(MINUTE, data_entrada, COALESCE(data_saida, NOW()))) as minutos_total " +
           "FROM usuarios_online_chat GROUP BY usuario_id, usuario_nome " +
           "ORDER BY minutos_total DESC LIMIT :limite")
    Flux<Object[]> findUsuariosMaisAtivos(@Param("limite") Integer limite);

    /**
     * Busca salas com mais usuários online
     * @param limite Limite de resultados
     * @return Flux com salas mais movimentadas
     */
    @Query("SELECT sala, COUNT(*) as usuarios_online FROM usuarios_online_chat " +
           "WHERE status_presenca IN ('ONLINE', 'AUSENTE', 'OCUPADO') " +
           "GROUP BY sala ORDER BY usuarios_online DESC LIMIT :limite")
    Flux<Object[]> findSalasMaisMovimentadas(@Param("limite") Integer limite);

    /**
     * Busca dispositivos mais utilizados
     * @return Flux com estatísticas de dispositivos
     */
    @Query("SELECT dispositivo, COUNT(*) as total FROM usuarios_online_chat " +
           "WHERE dispositivo IS NOT NULL GROUP BY dispositivo ORDER BY total DESC")
    Flux<Object[]> findEstatisticasDispositivos();

    /**
     * Busca conexões simultâneas de um usuário
     * @param usuarioId ID do usuário
     * @return Flux com conexões ativas do usuário
     */
    @Query("SELECT * FROM usuarios_online_chat WHERE usuario_id = :usuarioId " +
           "AND status_presenca IN ('ONLINE', 'AUSENTE', 'OCUPADO') ORDER BY data_entrada DESC")
    Flux<UsuarioOnlineR2dbc> findConexoesSimultaneasUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * Conta conexões simultâneas de um usuário
     * @param usuarioId ID do usuário
     * @return Mono com número de conexões ativas
     */
    @Query("SELECT COUNT(*) FROM usuarios_online_chat WHERE usuario_id = :usuarioId " +
           "AND status_presenca IN ('ONLINE', 'AUSENTE', 'OCUPADO')")
    Mono<Long> countConexoesSimultaneasUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * Busca histórico de sessões de um usuário
     * @param usuarioId ID do usuário
     * @param pageable Paginação
     * @return Flux com histórico de sessões
     */
    Flux<UsuarioOnlineR2dbc> findByUsuarioIdOrderByDataEntradaDesc(Long usuarioId, Pageable pageable);

    /**
     * Conta total de sessões no sistema
     * @return Mono com contagem total
     */
    @Query("SELECT COUNT(*) FROM usuarios_online_chat")
    Mono<Long> countTotal();
}