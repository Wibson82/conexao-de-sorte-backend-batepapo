package br.tec.facilitaservicos.batepapo.dominio.repositorio;

import java.time.LocalDateTime;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.tec.facilitaservicos.batepapo.dominio.entidade.MensagemR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusMensagem;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoMensagem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositório R2DBC reativo para Mensagem do Chat
 * Fornece operações de acesso a dados reativas para mensagens
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Repository
public interface RepositorioMensagemR2dbc extends R2dbcRepository<MensagemR2dbc, Long> {

    /**
     * Busca mensagens de uma sala específica ordenadas por data
     * @param sala Nome da sala
     * @param pageable Paginação
     * @return Flux com mensagens da sala
     */
    Flux<MensagemR2dbc> findBySalaOrderByDataEnvioDesc(String sala, Pageable pageable);

    /**
     * Busca mensagens de uma sala em período específico
     * @param sala Nome da sala
     * @param dataInicio Data de início
     * @param dataFim Data de fim
     * @param pageable Paginação
     * @return Flux com mensagens do período
     */
    @Query("SELECT * FROM mensagens_chat WHERE sala = :sala AND data_envio BETWEEN :dataInicio AND :dataFim ORDER BY data_envio DESC")
    Flux<MensagemR2dbc> findBySalaAndPeriodo(@Param("sala") String sala, 
                                            @Param("dataInicio") LocalDateTime dataInicio,
                                            @Param("dataFim") LocalDateTime dataFim,
                                            Pageable pageable);

    /**
     * Busca mensagens de um usuário em uma sala
     * @param usuarioId ID do usuário
     * @param sala Nome da sala
     * @param pageable Paginação
     * @return Flux com mensagens do usuário
     */
    Flux<MensagemR2dbc> findByUsuarioIdAndSalaOrderByDataEnvioDesc(Long usuarioId, String sala, Pageable pageable);

    /**
     * Busca mensagens por tipo em uma sala
     * @param sala Nome da sala
     * @param tipo Tipo da mensagem
     * @param pageable Paginação
     * @return Flux com mensagens do tipo
     */
    Flux<MensagemR2dbc> findBySalaAndTipoOrderByDataEnvioDesc(String sala, TipoMensagem tipo, Pageable pageable);

    /**
     * Busca mensagens por status em uma sala
     * @param sala Nome da sala
     * @param status Status da mensagem
     * @param pageable Paginação
     * @return Flux com mensagens do status
     */
    Flux<MensagemR2dbc> findBySalaAndStatusOrderByDataEnvioDesc(String sala, StatusMensagem status, Pageable pageable);

    /**
     * Busca mensagens recentes de uma sala (últimos X minutos)
     * @param sala Nome da sala
     * @param minutosAtras Minutos atrás
     * @return Flux com mensagens recentes
     */
    @Query("SELECT * FROM mensagens_chat WHERE sala = :sala AND data_envio >= :dataLimite ORDER BY data_envio DESC")
    Flux<MensagemR2dbc> findMensagensRecentes(@Param("sala") String sala, 
                                             @Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Busca mensagens que são respostas a uma mensagem específica
     * @param respostaParaId ID da mensagem original
     * @return Flux com respostas
     */
    Flux<MensagemR2dbc> findByRespostaParaIdOrderByDataEnvioAsc(Long respostaParaId);

    /**
     * Busca última mensagem de uma sala
     * @param sala Nome da sala
     * @return Mono com última mensagem
     */
    @Query("SELECT * FROM mensagens_chat WHERE sala = :sala ORDER BY data_envio DESC LIMIT 1")
    Mono<MensagemR2dbc> findUltimaMensagemSala(@Param("sala") String sala);

    /**
     * Busca mensagens editadas em uma sala
     * @param sala Nome da sala
     * @param pageable Paginação
     * @return Flux com mensagens editadas
     */
    @Query("SELECT * FROM mensagens_chat WHERE sala = :sala AND editada = true ORDER BY data_edicao DESC")
    Flux<MensagemR2dbc> findMensagensEditadas(@Param("sala") String sala, Pageable pageable);

    /**
     * Conta mensagens de uma sala
     * @param sala Nome da sala
     * @return Mono com contagem
     */
    @Query("SELECT COUNT(*) FROM mensagens_chat WHERE sala = :sala")
    Mono<Long> countBySala(@Param("sala") String sala);

    /**
     * Conta mensagens por usuário em uma sala
     * @param usuarioId ID do usuário
     * @param sala Nome da sala
     * @return Mono com contagem
     */
    @Query("SELECT COUNT(*) FROM mensagens_chat WHERE usuario_id = :usuarioId AND sala = :sala")
    Mono<Long> countByUsuarioIdAndSala(@Param("usuarioId") Long usuarioId, @Param("sala") String sala);

    /**
     * Conta mensagens por tipo em uma sala
     * @param sala Nome da sala
     * @param tipo Tipo da mensagem
     * @return Mono com contagem
     */
    @Query("SELECT COUNT(*) FROM mensagens_chat WHERE sala = :sala AND tipo = :tipo")
    Mono<Long> countBySalaAndTipo(@Param("sala") String sala, @Param("tipo") String tipo);

    /**
     * Busca estatísticas de mensagens por usuário em uma sala
     * @param sala Nome da sala
     * @param limite Limite de resultados
     * @return Flux com estatísticas
     */
    @Query("SELECT usuario_nome, COUNT(*) as total FROM mensagens_chat WHERE sala = :sala " +
           "GROUP BY usuario_id, usuario_nome ORDER BY total DESC LIMIT :limite")
    Flux<Object[]> findEstatisticasUsuariosSala(@Param("sala") String sala, @Param("limite") Integer limite);

    /**
     * Busca palavras mais frequentes nas mensagens de uma sala
     * @param sala Nome da sala
     * @param limite Limite de resultados
     * @return Flux com estatísticas de palavras
     */
    @Query("SELECT SUBSTRING_INDEX(SUBSTRING_INDEX(conteudo, ' ', numbers.n), ' ', -1) as palavra, " +
           "COUNT(*) as frequencia FROM mensagens_chat " +
           "CROSS JOIN (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) numbers " +
           "WHERE sala = :sala AND CHAR_LENGTH(conteudo) - CHAR_LENGTH(REPLACE(conteudo, ' ', '')) >= numbers.n - 1 " +
           "AND CHAR_LENGTH(SUBSTRING_INDEX(SUBSTRING_INDEX(conteudo, ' ', numbers.n), ' ', -1)) > 3 " +
           "GROUP BY palavra ORDER BY frequencia DESC LIMIT :limite")
    Flux<Object[]> findPalavrasFrequentesSala(@Param("sala") String sala, @Param("limite") Integer limite);

    /**
     * Remove mensagens antigas de uma sala
     * @param sala Nome da sala
     * @param dataLimite Data limite para remoção
     * @return Mono com número de mensagens removidas
     */
    @Query("DELETE FROM mensagens_chat WHERE sala = :sala AND data_envio < :dataLimite")
    Mono<Integer> removerMensagensAntigas(@Param("sala") String sala, @Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Marca mensagens como lidas por usuário
     * @param sala Nome da sala
     * @param usuarioId ID do usuário que leu
     * @param dataLimite Data limite das mensagens a marcar como lidas
     * @return Mono com número de mensagens atualizadas
     */
    @Query("UPDATE mensagens_chat SET status = 'LIDA' WHERE sala = :sala AND usuario_id != :usuarioId " +
           "AND data_envio >= :dataLimite AND status = 'ENVIADA'")
    Mono<Integer> marcarMensagensComoLidas(@Param("sala") String sala, 
                                          @Param("usuarioId") Long usuarioId,
                                          @Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Busca salas ativas (com mensagens recentes)
     * @param dataLimite Data limite para considerar ativa
     * @return Flux com nomes das salas ativas
     */
    @Query("SELECT DISTINCT sala FROM mensagens_chat WHERE data_envio >= :dataLimite ORDER BY sala")
    Flux<String> findSalasAtivas(@Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Busca usuários ativos em uma sala
     * @param sala Nome da sala
     * @param dataLimite Data limite para considerar ativo
     * @return Flux com usuários ativos
     */
    @Query("SELECT DISTINCT usuario_id, usuario_nome FROM mensagens_chat " +
           "WHERE sala = :sala AND data_envio >= :dataLimite ORDER BY usuario_nome")
    Flux<Object[]> findUsuariosAtivosSala(@Param("sala") String sala, @Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Busca mensagens que contém texto específico
     * @param sala Nome da sala
     * @param texto Texto a buscar
     * @param pageable Paginação
     * @return Flux com mensagens encontradas
     */
    @Query("SELECT * FROM mensagens_chat WHERE sala = :sala AND LOWER(conteudo) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "ORDER BY data_envio DESC")
    Flux<MensagemR2dbc> buscarMensagensPorTexto(@Param("sala") String sala, 
                                               @Param("texto") String texto, 
                                               Pageable pageable);

    /**
     * Conta total de mensagens no sistema
     * @return Mono com contagem total
     */
    @Query("SELECT COUNT(*) FROM mensagens_chat")
    Mono<Long> countTotal();

    /**
     * Busca primeira mensagem do sistema
     * @return Mono com primeira mensagem
     */
    @Query("SELECT * FROM mensagens_chat ORDER BY data_envio ASC LIMIT 1")
    Mono<MensagemR2dbc> findPrimeiraMensagem();

    /**
     * Busca mensagem mais recente do sistema
     * @return Mono com mensagem mais recente
     */
    @Query("SELECT * FROM mensagens_chat ORDER BY data_envio DESC LIMIT 1")
    Mono<MensagemR2dbc> findMensagemMaisRecente();
}