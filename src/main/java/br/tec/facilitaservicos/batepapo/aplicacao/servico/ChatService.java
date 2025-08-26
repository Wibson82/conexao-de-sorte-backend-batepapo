package br.tec.facilitaservicos.batepapo.aplicacao.servico;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.tec.facilitaservicos.batepapo.aplicacao.mapper.MensagemMapper;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDto;
import br.tec.facilitaservicos.batepapo.dominio.entidade.MensagemR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusMensagem;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoMensagem;
import br.tec.facilitaservicos.batepapo.dominio.repositorio.RepositorioMensagemR2dbc;
import br.tec.facilitaservicos.batepapo.infraestrutura.cache.ChatCacheService;
import br.tec.facilitaservicos.batepapo.infraestrutura.streaming.ChatStreamingService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Serviço reativo para operações de Chat
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Service
public class ChatService {

    private final RepositorioMensagemR2dbc repositorio;
    private final MensagemMapper mapper;
    private final ChatStreamingService streamingService;
    private final ChatCacheService cacheService;

    @Value("${pagination.default-size:20}")
    private int tamanhoDefault;

    @Value("${chat.max-message-length:500}")
    private int maxMessageLength;

    public ChatService(RepositorioMensagemR2dbc repositorio, MensagemMapper mapper,
                      ChatStreamingService streamingService, ChatCacheService cacheService) {
        this.repositorio = repositorio;
        this.mapper = mapper;
        this.streamingService = streamingService;
        this.cacheService = cacheService;
    }

    /**
     * Envia nova mensagem para uma sala
     */
    public Mono<MensagemDto> enviarMensagem(String conteudo, Long usuarioId, String usuarioNome, 
                                           String sala, Long respostaParaId) {
        // Validar conteúdo
        if (conteudo == null || conteudo.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Conteúdo da mensagem é obrigatório"));
        }
        
        if (conteudo.length() > maxMessageLength) {
            return Mono.error(new IllegalArgumentException("Mensagem excede o limite de " + maxMessageLength + " caracteres"));
        }

        // Criar mensagem
        MensagemR2dbc mensagem = MensagemR2dbc.builder()
            .conteudo(conteudo.trim())
            .usuario(usuarioId, usuarioNome)
            .sala(sala)
            .tipo(TipoMensagem.TEXTO)
            .respostaParaId(respostaParaId)
            .build();

        return repositorio.save(mensagem)
            .map(mapper::paraDto)
            .doOnSuccess(mensagemDto -> {
                // Enviar para stream em tempo real
                streamingService.publicarNovaMensagem(mensagemDto);
                
                // Invalidar cache da sala
                cacheService.invalidarCacheSala(sala);
            });
    }

    /**
     * Busca mensagens de uma sala com paginação
     */
    public Flux<MensagemDto> buscarMensagensSala(String sala, int pagina, int tamanho, 
                                                LocalDateTime dataInicio, LocalDateTime dataFim) {
        pagina = Math.max(0, pagina);
        tamanho = Math.min(Math.max(1, tamanho), 100);
        
        Pageable pageable = PageRequest.of(pagina, tamanho);
        
        // Buscar no cache primeiro
        String cacheKey = String.format("mensagens:%s:%d:%d", sala, pagina, tamanho);
        
        return cacheService.buscarDoCache(cacheKey, MensagemDto.class)
            .switchIfEmpty(buscarMensagensBanco(sala, pageable, dataInicio, dataFim)
                .doOnNext(mensagens -> cacheService.salvarNoCache(cacheKey, mensagens, 180)))
            .cast(MensagemDto.class);
    }

    /**
     * Busca mensagem por ID
     */
    public Mono<MensagemDto> buscarMensagemPorId(Long id) {
        return repositorio.findById(id)
            .map(mapper::paraDto);
    }

    /**
     * Edita conteúdo de uma mensagem
     */
    public Mono<MensagemDto> editarMensagem(Long mensagemId, String novoConteudo, Long usuarioId) {
        return repositorio.findById(mensagemId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Mensagem não encontrada")))
            .filter(mensagem -> mensagem.getUsuarioId().equals(usuarioId))
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Usuário não pode editar esta mensagem")))
            .filter(MensagemR2dbc::podeSerEditada)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Mensagem não pode mais ser editada")))
            .doOnNext(mensagem -> mensagem.setConteudo(novoConteudo))
            .flatMap(repositorio::save)
            .map(mapper::paraDto)
            .doOnSuccess(mensagemDto -> {
                streamingService.publicarMensagemEditada(mensagemDto);
                cacheService.invalidarCacheSala(mensagemDto.sala());
            });
    }

    /**
     * Exclui uma mensagem
     */
    public Mono<Void> excluirMensagem(Long mensagemId, Long usuarioId) {
        return repositorio.findById(mensagemId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Mensagem não encontrada")))
            .filter(mensagem -> mensagem.getUsuarioId().equals(usuarioId))
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Usuário não pode excluir esta mensagem")))
            .doOnNext(mensagem -> mensagem.setStatus(StatusMensagem.EXCLUIDA))
            .flatMap(repositorio::save)
            .doOnSuccess(mensagem -> {
                MensagemDto dto = mapper.paraDto(mensagem);
                streamingService.publicarMensagemExcluida(dto);
                cacheService.invalidarCacheSala(dto.sala());
            })
            .then();
    }

    /**
     * Busca mensagens por texto
     */
    public Flux<MensagemDto> buscarMensagensPorTexto(String sala, String texto, int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho);
        
        return repositorio.buscarMensagensPorTexto(sala, texto, pageable)
            .map(mapper::paraDto);
    }

    /**
     * Conta mensagens de uma sala
     */
    public Mono<Long> contarMensagensSala(String sala) {
        return repositorio.countBySala(sala);
    }

    /**
     * Busca última mensagem de uma sala
     */
    public Mono<MensagemDto> buscarUltimaMensagemSala(String sala) {
        return repositorio.findUltimaMensagemSala(sala)
            .map(mapper::paraDto);
    }

    /**
     * Busca mensagens recentes de uma sala
     */
    public Flux<MensagemDto> buscarMensagensRecentes(String sala, int minutosAtras) {
        LocalDateTime dataLimite = LocalDateTime.now().minusMinutes(minutosAtras);
        
        return repositorio.findMensagensRecentes(sala, dataLimite)
            .map(mapper::paraDto);
    }

    /**
     * Busca estatísticas de mensagens por usuário
     */
    public Flux<Object[]> buscarEstatisticasUsuariosSala(String sala, int limite) {
        return repositorio.findEstatisticasUsuariosSala(sala, limite);
    }

    /**
     * Marca mensagens como lidas por um usuário
     */
    public Mono<Integer> marcarMensagensComoLidas(String sala, Long usuarioId, int minutosAtras) {
        LocalDateTime dataLimite = LocalDateTime.now().minusMinutes(minutosAtras);
        
        return repositorio.marcarMensagensComoLidas(sala, usuarioId, dataLimite);
    }

    /**
     * Remove mensagens antigas de uma sala
     */
    public Mono<Integer> limparMensagensAntigas(String sala, int diasAtras) {
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(diasAtras);
        
        return repositorio.removerMensagensAntigas(sala, dataLimite)
            .doOnSuccess(count -> {
                if (count > 0) {
                    cacheService.invalidarCacheSala(sala);
                }
            });
    }

    // Métodos auxiliares

    private Flux<MensagemDto> buscarMensagensBanco(String sala, Pageable pageable, 
                                                  LocalDateTime dataInicio, LocalDateTime dataFim) {
        if (dataInicio != null && dataFim != null) {
            return repositorio.findBySalaAndPeriodo(sala, dataInicio, dataFim, pageable)
                .map(mapper::paraDto);
        } else {
            return repositorio.findBySalaOrderByDataEnvioDesc(sala, pageable)
                .map(mapper::paraDto);
        }
    }
}