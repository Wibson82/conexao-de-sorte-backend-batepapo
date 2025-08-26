package br.tec.facilitaservicos.batepapo.infraestrutura.cache;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * ============================================================================
 * 💾 SERVIÇO DE CACHE DO CHAT
 * ============================================================================
 * 
 * Gerencia cache reativo do sistema de bate-papo:
 * - Cache de mensagens por sala
 * - Cache de usuários online
 * - Cache de configurações de sala
 * - TTL automático e invalidação inteligente
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Service
public class ChatCacheService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Prefixos para diferentes tipos de cache
    private static final String MENSAGENS_PREFIX = "chat:mensagens:";
    private static final String USUARIOS_ONLINE_PREFIX = "chat:online:";
    private static final String SALA_CONFIG_PREFIX = "chat:config:";
    private static final String STATS_PREFIX = "chat:stats:";
    
    // TTLs padrão
    private static final Duration MENSAGENS_TTL = Duration.ofMinutes(15);
    private static final Duration USUARIOS_TTL = Duration.ofMinutes(5);
    private static final Duration CONFIG_TTL = Duration.ofHours(1);
    private static final Duration STATS_TTL = Duration.ofMinutes(10);

    public ChatCacheService(ReactiveRedisTemplate<String, String> redisTemplate, 
                           ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Busca dados do cache por chave
     */
    public <T> Mono<T> buscarDoCache(String chave, Class<T> classe) {
        return redisTemplate.opsForValue()
            .get(chave)
            .map(json -> {
                try {
                    return objectMapper.readValue(json, classe);
                } catch (Exception e) {
                    throw new RuntimeException("Erro ao deserializar cache: " + chave, e);
                }
            });
    }
    
    /**
     * Busca lista do cache por chave
     */
    public <T> Mono<List<T>> buscarListaDoCache(String chave, TypeReference<List<T>> typeRef) {
        return redisTemplate.opsForValue()
            .get(chave)
            .map(json -> {
                try {
                    return objectMapper.readValue(json, typeRef);
                } catch (Exception e) {
                    throw new RuntimeException("Erro ao deserializar lista do cache: " + chave, e);
                }
            });
    }

    /**
     * Salva dados no cache com TTL
     */
    public <T> Mono<Void> salvarNoCache(String chave, T dados, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(dados);
            return redisTemplate.opsForValue()
                .set(chave, json, ttl)
                .then();
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Erro ao serializar para cache: " + chave, e));
        }
    }
    
    /**
     * Salva dados no cache com TTL padrão
     */
    public <T> Mono<Void> salvarNoCache(String chave, T dados) {
        return salvarNoCache(chave, dados, MENSAGENS_TTL);
    }

    /**
     * Cache de mensagens de uma sala
     */
    public Mono<List<MensagemDto>> buscarMensagensSala(String sala, int pagina, int tamanho) {
        String chave = MENSAGENS_PREFIX + sala + ":" + pagina + ":" + tamanho;
        return buscarListaDoCache(chave, new TypeReference<List<MensagemDto>>() {});
    }
    
    /**
     * Cache mensagens de uma sala
     */
    public Mono<Void> salvarMensagensSala(String sala, int pagina, int tamanho, List<MensagemDto> mensagens) {
        String chave = MENSAGENS_PREFIX + sala + ":" + pagina + ":" + tamanho;
        return salvarNoCache(chave, mensagens, MENSAGENS_TTL);
    }

    /**
     * Cache de usuários online em uma sala
     */
    public Flux<String> buscarUsuariosOnlineSala(String sala) {
        String chave = USUARIOS_ONLINE_PREFIX + sala;
        return redisTemplate.opsForSet().members(chave);
    }
    
    /**
     * Adiciona usuário online na sala
     */
    public Mono<Void> adicionarUsuarioOnline(String sala, String usuarioId) {
        String chave = USUARIOS_ONLINE_PREFIX + sala;
        return redisTemplate.opsForSet()
            .add(chave, usuarioId)
            .then(redisTemplate.expire(chave, USUARIOS_TTL))
            .then();
    }
    
    /**
     * Remove usuário online da sala
     */
    public Mono<Void> removerUsuarioOnline(String sala, String usuarioId) {
        String chave = USUARIOS_ONLINE_PREFIX + sala;
        return redisTemplate.opsForSet()
            .remove(chave, usuarioId)
            .then();
    }
    
    /**
     * Conta usuários online na sala
     */
    public Mono<Long> contarUsuariosOnline(String sala) {
        String chave = USUARIOS_ONLINE_PREFIX + sala;
        return redisTemplate.opsForSet().size(chave);
    }

    /**
     * Invalidar cache de uma sala específica
     */
    public Mono<Void> invalidarCacheSala(String sala) {
        String patternMensagens = MENSAGENS_PREFIX + sala + ":*";
        String chaveConfig = SALA_CONFIG_PREFIX + sala;
        String chaveStats = STATS_PREFIX + sala;
        
        return redisTemplate.keys(patternMensagens)
            .flatMap(redisTemplate::delete)
            .then(redisTemplate.delete(chaveConfig))
            .then(redisTemplate.delete(chaveStats))
            .then();
    }

    /**
     * Cache de configurações da sala
     */
    public <T> Mono<T> buscarConfigSala(String sala, Class<T> classe) {
        String chave = SALA_CONFIG_PREFIX + sala;
        return buscarDoCache(chave, classe);
    }
    
    /**
     * Salvar configurações da sala
     */
    public <T> Mono<Void> salvarConfigSala(String sala, T config) {
        String chave = SALA_CONFIG_PREFIX + sala;
        return salvarNoCache(chave, config, CONFIG_TTL);
    }

    /**
     * Cache de estatísticas da sala
     */
    public <T> Mono<T> buscarStatsSala(String sala, Class<T> classe) {
        String chave = STATS_PREFIX + sala;
        return buscarDoCache(chave, classe);
    }
    
    /**
     * Salvar estatísticas da sala
     */
    public <T> Mono<Void> salvarStatsSala(String sala, T stats) {
        String chave = STATS_PREFIX + sala;
        return salvarNoCache(chave, stats, STATS_TTL);
    }

    /**
     * Atualizar heartbeat de usuário
     */
    public Mono<Void> atualizarHeartbeat(String usuarioId, String sala) {
        String chave = "chat:heartbeat:" + usuarioId + ":" + sala;
        return redisTemplate.opsForValue()
            .set(chave, String.valueOf(System.currentTimeMillis()), Duration.ofMinutes(2))
            .then();
    }
    
    /**
     * Verificar se usuário está ativo
     */
    public Mono<Boolean> usuarioAtivo(String usuarioId, String sala) {
        String chave = "chat:heartbeat:" + usuarioId + ":" + sala;
        return redisTemplate.hasKey(chave);
    }

    /**
     * Limpar cache de usuários inativos
     */
    public Mono<Void> limparUsuariosInativos(String sala) {
        return buscarUsuariosOnlineSala(sala)
            .flatMap(usuarioId -> usuarioAtivo(usuarioId, sala)
                .flatMap(ativo -> {
                    if (!ativo) {
                        return removerUsuarioOnline(sala, usuarioId);
                    }
                    return Mono.empty();
                })
            )
            .then();
    }

    /**
     * Invalidar todo cache do chat (usar com cuidado)
     */
    public Mono<Void> invalidarTodoCache() {
        return Flux.just(
                MENSAGENS_PREFIX + "*",
                USUARIOS_ONLINE_PREFIX + "*", 
                SALA_CONFIG_PREFIX + "*",
                STATS_PREFIX + "*",
                "chat:heartbeat:*"
            )
            .flatMap(redisTemplate::keys)
            .flatMap(redisTemplate::delete)
            .then();
    }

    /**
     * Obter estatísticas do cache
     */
    public Mono<CacheStats> obterEstatisticas() {
        return Mono.zip(
            redisTemplate.keys(MENSAGENS_PREFIX + "*").count(),
            redisTemplate.keys(USUARIOS_ONLINE_PREFIX + "*").count(),
            redisTemplate.keys(SALA_CONFIG_PREFIX + "*").count(),
            redisTemplate.keys("chat:heartbeat:*").count()
        ).map(tuple -> new CacheStats(
            tuple.getT1(), // mensagens
            tuple.getT2(), // usuarios online
            tuple.getT3(), // configs
            tuple.getT4()  // heartbeats
        ));
    }

    // DTO para estatísticas do cache
    public record CacheStats(
        Long totalMensagensCache,
        Long totalUsuariosOnlineCache,
        Long totalConfigsCache,
        Long totalHeartbeatsCache
    ) {}
}