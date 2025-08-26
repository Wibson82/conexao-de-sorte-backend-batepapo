package br.tec.facilitaservicos.batepapo.aplicacao.servico;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.tec.facilitaservicos.batepapo.apresentacao.dto.SalaDto;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.UsuarioOnlineDto;
import br.tec.facilitaservicos.batepapo.dominio.entidade.SalaR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.entidade.UsuarioOnlineR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusPresenca;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusSala;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoSala;
import br.tec.facilitaservicos.batepapo.dominio.repositorio.RepositorioSalaR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.repositorio.RepositorioUsuarioOnlineR2dbc;
import br.tec.facilitaservicos.batepapo.infraestrutura.cache.ChatCacheService;
import br.tec.facilitaservicos.batepapo.infraestrutura.streaming.ChatStreamingService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * ============================================================================
 * 🏠 SERVIÇO REATIVO DE SALAS DE CHAT
 * ============================================================================
 * 
 * Gerencia salas de chat, usuários online e presença em tempo real:
 * - Criação e gerenciamento de salas (públicas, privadas, grupos)
 * - Controle de acesso e permissões
 * - Monitoramento de usuários online
 * - Presença em tempo real com heartbeat
 * - Cache Redis para performance
 * - Streaming de eventos via Redis Streams
 * 
 * Funcionalidades:
 * - Salas públicas (lobby geral)
 * - Salas privadas (chat 1:1)
 * - Grupos (múltiplos usuários)
 * - Controle de moderadores
 * - Rate limiting por sala
 * - Histórico de atividade
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Service
public class SalaService {

    private final RepositorioSalaR2dbc repositorioSala;
    private final RepositorioUsuarioOnlineR2dbc repositorioUsuarioOnline;
    private final ChatStreamingService streamingService;
    private final ChatCacheService cacheService;

    // Cache em memória para usuários online por performance
    private final Set<String> usuariosOnlineCache = ConcurrentHashMap.newKeySet();

    @Value("${chat.sala.max-usuarios:100}")
    private int maxUsuariosPorSala;

    @Value("${chat.presenca.timeout-minutes:5}")
    private int timeoutPresencaMinutos;

    @Value("${chat.sala.default-name:Sala Geral}")
    private String nomeSalaDefault;

    public SalaService(RepositorioSalaR2dbc repositorioSala,
                      RepositorioUsuarioOnlineR2dbc repositorioUsuarioOnline,
                      ChatStreamingService streamingService,
                      ChatCacheService cacheService) {
        this.repositorioSala = repositorioSala;
        this.repositorioUsuarioOnline = repositorioUsuarioOnline;
        this.streamingService = streamingService;
        this.cacheService = cacheService;
    }

    // === GERENCIAMENTO DE SALAS ===

    /**
     * Cria nova sala de chat
     */
    public Mono<SalaDto> criarSala(String nome, String descricao, TipoSala tipo, 
                                  Long criadorId, String criadorNome) {
        SalaR2dbc sala = SalaR2dbc.builder()
            .nome(nome)
            .descricao(descricao)
            .tipo(tipo)
            .criadorId(criadorId)
            .criadorNome(criadorNome)
            .maxUsuarios(maxUsuariosPorSala)
            .status(StatusSala.ATIVA)
            .build();

        return repositorioSala.save(sala)
            .map(this::paraDto)
            .doOnSuccess(salaDto -> 
                streamingService.publicarEventoSala(
                    br.tec.facilitaservicos.batepapo.apresentacao.dto.ChatEventDto
                        .salaCriada(salaDto.identificador(), criadorId, criadorNome)
                )
            );
    }

    /**
     * Busca sala por identificador
     */
    public Mono<SalaDto> buscarSala(String identificador) {
        // Buscar no cache primeiro
        String cacheKey = "sala:" + identificador;
        
        return cacheService.buscarDoCache(cacheKey, SalaDto.class)
            .switchIfEmpty(
                repositorioSala.findByIdentificador(identificador)
                    .map(this::paraDto)
                    .doOnNext(sala -> cacheService.salvarNoCache(cacheKey, sala, 300))
            );
    }

    /**
     * Lista salas públicas ativas
     */
    public Flux<SalaDto> listarSalasPublicas(int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho);
        
        return repositorioSala.findSalasPublicasAtivas(pageable)
            .map(this::paraDto);
    }

    /**
     * Busca salas do usuário (criadas ou participando)
     */
    public Flux<SalaDto> buscarSalasUsuario(Long usuarioId, int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho);
        
        return repositorioSala.findSalasUsuario(usuarioId, pageable)
            .map(this::paraDto);
    }

    /**
     * Atualiza informações da sala
     */
    public Mono<SalaDto> atualizarSala(String identificador, String nome, String descricao, 
                                      Integer maxUsuarios, Long moderadorId) {
        return repositorioSala.findByIdentificador(identificador)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Sala não encontrada")))
            .filter(sala -> podeModificarSala(sala, moderadorId))
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Sem permissão para modificar sala")))
            .doOnNext(sala -> {
                if (nome != null) sala.setNome(nome);
                if (descricao != null) sala.setDescricao(descricao);
                if (maxUsuarios != null) sala.setMaxUsuarios(maxUsuarios);
            })
            .flatMap(repositorioSala::save)
            .map(this::paraDto)
            .doOnSuccess(sala -> {
                cacheService.invalidarCache("sala:" + identificador);
                streamingService.publicarEventoSala(
                    br.tec.facilitaservicos.batepapo.apresentacao.dto.ChatEventDto
                        .salaAtualizada(identificador, moderadorId, "Sistema")
                );
            });
    }

    /**
     * Arquiva/desativa sala
     */
    public Mono<Void> arquivarSala(String identificador, Long moderadorId) {
        return repositorioSala.findByIdentificador(identificador)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Sala não encontrada")))
            .filter(sala -> podeModificarSala(sala, moderadorId))
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Sem permissão para arquivar sala")))
            .doOnNext(sala -> sala.setStatus(StatusSala.ARQUIVADA))
            .flatMap(repositorioSala::save)
            .doOnSuccess(sala -> {
                // Remover todos usuários da sala
                removerTodosUsuarios(identificador).subscribe();
                cacheService.invalidarCache("sala:" + identificador);
                streamingService.publicarEventoSala(
                    br.tec.facilitaservicos.batepapo.apresentacao.dto.ChatEventDto
                        .salaArquivada(identificador, moderadorId, "Sistema")
                );
            })
            .then();
    }

    // === GERENCIAMENTO DE PRESENÇA ===

    /**
     * Conecta usuário à sala
     */
    public Mono<UsuarioOnlineDto> conectarUsuario(String sala, Long usuarioId, String usuarioNome, String userAgent) {
        return verificarCapacidadeSala(sala)
            .then(criarOuAtualizarPresenca(sala, usuarioId, usuarioNome, userAgent))
            .map(this::paraUsuarioOnlineDto)
            .doOnSuccess(usuario -> {
                usuariosOnlineCache.add(criarChaveUsuario(sala, usuarioId));
                streamingService.publicarEventoUsuario(
                    br.tec.facilitaservicos.batepapo.apresentacao.dto.ChatEventDto
                        .usuarioConectado(sala, usuarioId, usuarioNome)
                );
                cacheService.invalidarCache("usuarios-online:" + sala);
            });
    }

    /**
     * Desconecta usuário da sala
     */
    public Mono<Void> desconectarUsuario(String sala, Long usuarioId) {
        return repositorioUsuarioOnline.findBySalaAndUsuarioId(sala, usuarioId)
            .doOnNext(usuario -> usuario.setStatus(StatusPresenca.OFFLINE))
            .flatMap(repositorioUsuarioOnline::save)
            .doOnSuccess(usuario -> {
                usuariosOnlineCache.remove(criarChaveUsuario(sala, usuarioId));
                streamingService.publicarEventoUsuario(
                    br.tec.facilitaservicos.batepapo.apresentacao.dto.ChatEventDto
                        .usuarioDesconectado(sala, usuarioId, usuario.getUsuarioNome())
                );
                cacheService.invalidarCache("usuarios-online:" + sala);
            })
            .then();
    }

    /**
     * Atualiza heartbeat do usuário (manter vivo)
     */
    public Mono<Void> atualizarHeartbeat(String sala, Long usuarioId) {
        return repositorioUsuarioOnline.findBySalaAndUsuarioId(sala, usuarioId)
            .doOnNext(usuario -> usuario.atualizarHeartbeat())
            .flatMap(repositorioUsuarioOnline::save)
            .doOnSuccess(usuario -> usuariosOnlineCache.add(criarChaveUsuario(sala, usuarioId)))
            .then();
    }

    /**
     * Lista usuários online da sala
     */
    public Flux<UsuarioOnlineDto> listarUsuariosOnline(String sala) {
        String cacheKey = "usuarios-online:" + sala;
        
        return cacheService.buscarDoCache(cacheKey, UsuarioOnlineDto.class)
            .switchIfEmpty(
                repositorioUsuarioOnline.findUsuariosOnlineSala(sala)
                    .map(this::paraUsuarioOnlineDto)
                    .doOnNext(usuarios -> cacheService.salvarNoCache(cacheKey, usuarios, 60))
            )
            .cast(UsuarioOnlineDto.class);
    }

    /**
     * Conta usuários online da sala
     */
    public Mono<Long> contarUsuariosOnline(String sala) {
        return repositorioUsuarioOnline.countUsuariosOnlineSala(sala);
    }

    /**
     * Verifica se usuário está online na sala
     */
    public Mono<Boolean> usuarioOnline(String sala, Long usuarioId) {
        String chave = criarChaveUsuario(sala, usuarioId);
        
        if (usuariosOnlineCache.contains(chave)) {
            return Mono.just(true);
        }
        
        return repositorioUsuarioOnline.existsBySalaAndUsuarioIdAndStatus(
            sala, usuarioId, StatusPresenca.ONLINE
        );
    }

    // === OPERAÇÕES DE MANUTENÇÃO ===

    /**
     * Remove usuários inativos (timeout)
     */
    public Mono<Integer> removerUsuariosInativos() {
        LocalDateTime tempoLimite = LocalDateTime.now().minusMinutes(timeoutPresencaMinutos);
        
        return repositorioUsuarioOnline.removerUsuariosInativos(tempoLimite)
            .doOnSuccess(count -> {
                // Limpar cache em memória
                usuariosOnlineCache.clear();
                if (count > 0) {
                    cacheService.invalidarCachePattern("usuarios-online:*");
                }
            });
    }

    /**
     * Estatísticas das salas
     */
    public Flux<Object[]> estatisticasSalas() {
        return repositorioSala.getEstatisticasSalas();
    }

    /**
     * Remove usuários offline antigos
     */
    public Mono<Integer> limparHistoricoPresenca(int diasAtras) {
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(diasAtras);
        
        return repositorioUsuarioOnline.removerHistoricoAntigo(dataLimite);
    }

    // === MÉTODOS AUXILIARES ===

    private Mono<Void> verificarCapacidadeSala(String identificador) {
        return buscarSala(identificador)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Sala não encontrada")))
            .filter(sala -> sala.status() == StatusSala.ATIVA)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Sala não está ativa")))
            .flatMap(sala -> contarUsuariosOnline(identificador)
                .filter(count -> count < sala.maxUsuarios())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Sala lotada")))
            )
            .then();
    }

    private Mono<UsuarioOnlineR2dbc> criarOuAtualizarPresenca(String sala, Long usuarioId, 
                                                             String usuarioNome, String userAgent) {
        return repositorioUsuarioOnline.findBySalaAndUsuarioId(sala, usuarioId)
            .switchIfEmpty(Mono.just(UsuarioOnlineR2dbc.builder()
                .sala(sala)
                .usuarioId(usuarioId)
                .usuarioNome(usuarioNome)
                .userAgent(userAgent)
                .build()))
            .doOnNext(usuario -> {
                usuario.setStatus(StatusPresenca.ONLINE);
                usuario.atualizarHeartbeat();
                if (userAgent != null) {
                    usuario.setUserAgent(userAgent);
                }
            })
            .flatMap(repositorioUsuarioOnline::save);
    }

    private Mono<Void> removerTodosUsuarios(String sala) {
        return repositorioUsuarioOnline.findBySala(sala)
            .doOnNext(usuario -> usuario.setStatus(StatusPresenca.OFFLINE))
            .flatMap(repositorioUsuarioOnline::save)
            .then();
    }

    private boolean podeModificarSala(SalaR2dbc sala, Long usuarioId) {
        return sala.getCriadorId().equals(usuarioId);
    }

    private String criarChaveUsuario(String sala, Long usuarioId) {
        return sala + ":" + usuarioId;
    }

    // Conversores DTO

    private SalaDto paraDto(SalaR2dbc sala) {
        return new SalaDto(
            sala.getIdentificador(),
            sala.getNome(),
            sala.getDescricao(),
            sala.getTipo(),
            sala.getStatus(),
            sala.getCriadorId(),
            sala.getCriadorNome(),
            sala.getMaxUsuarios(),
            0, // usuários online será calculado separadamente
            sala.getCriadoEm(),
            sala.getAtualizadoEm()
        );
    }

    private UsuarioOnlineDto paraUsuarioOnlineDto(UsuarioOnlineR2dbc usuario) {
        return new UsuarioOnlineDto(
            usuario.getId(),
            usuario.getUsuarioId(),
            usuario.getUsuarioNome(),
            usuario.getSala(),
            usuario.getStatus(),
            usuario.getConectadoEm(),
            usuario.getUltimoHeartbeat(),
            usuario.getUserAgent()
        );
    }
}