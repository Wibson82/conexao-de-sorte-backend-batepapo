package br.tec.facilitaservicos.batepapo.infraestrutura.streaming;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamReadOptions;
import org.springframework.stereotype.Service;

import br.tec.facilitaservicos.batepapo.apresentacao.dto.ChatEventDto;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Serviço de streaming distribuído usando Redis Streams
 * Gerencia broadcasting de eventos de chat entre instâncias do microserviço
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Service
public class ChatStreamingService {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${redis.streams.chat-messages:chat:messages}")
    private String streamMensagens;

    @Value("${redis.streams.user-presence:chat:presence}")
    private String streamPresenca;

    @Value("${redis.streams.room-events:chat:rooms}")
    private String streamSalas;

    @Value("${redis.streams.consumer-group:batepapo-service}")
    private String consumerGroup;

    @Value("${redis.streams.consumer-name:batepapo-${random.uuid}}")
    private String consumerName;

    @Value("${redis.streams.block-duration:5000}")
    private long blockDuration;

    public ChatStreamingService(ReactiveRedisTemplate<String, Object> redisTemplate,
                               ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica nova mensagem no stream
     */
    public Mono<Void> publicarNovaMensagem(MensagemDto mensagem) {
        return publicarEvento(streamMensagens, "NOVA_MENSAGEM", 
                             Map.of("mensagem", serializar(mensagem)));
    }

    /**
     * Publica mensagem editada no stream
     */
    public Mono<Void> publicarMensagemEditada(MensagemDto mensagem) {
        return publicarEvento(streamMensagens, "MENSAGEM_EDITADA", 
                             Map.of("mensagem", serializar(mensagem)));
    }

    /**
     * Publica mensagem excluída no stream
     */
    public Mono<Void> publicarMensagemExcluida(MensagemDto mensagem) {
        return publicarEvento(streamMensagens, "MENSAGEM_EXCLUIDA", 
                             Map.of("mensagem", serializar(mensagem)));
    }

    /**
     * Publica evento de usuário no stream
     */
    public Mono<Void> publicarEventoUsuario(ChatEventDto evento) {
        return publicarEvento(streamPresenca, evento.tipo().name(), 
                             Map.of("evento", serializar(evento)));
    }

    /**
     * Publica evento de sala no stream
     */
    public Mono<Void> publicarEventoSala(ChatEventDto evento) {
        return publicarEvento(streamSalas, evento.tipo().name(), 
                             Map.of("evento", serializar(evento)));
    }

    /**
     * Stream de eventos de mensagens
     */
    public Flux<ChatEventDto> streamEventosMensagens() {
        return lerStream(streamMensagens)
            .mapNotNull(this::deserializarEventoMensagem)
            .onErrorResume(this::tratarErroStream);
    }

    /**
     * Stream de eventos de usuários
     */
    public Flux<ChatEventDto> streamEventosUsuarios(String sala) {
        return lerStream(streamPresenca)
            .mapNotNull(this::deserializarEvento)
            .filter(evento -> evento.sala().equals(sala))
            .onErrorResume(this::tratarErroStream);
    }

    /**
     * Stream de eventos de salas
     */
    public Flux<ChatEventDto> streamEventosSala(String sala) {
        return lerStream(streamSalas)
            .mapNotNull(this::deserializarEvento)
            .filter(evento -> evento.sala().equals(sala))
            .onErrorResume(this::tratarErroStream);
    }

    /**
     * Stream global de presença
     */
    public Flux<ChatEventDto> streamPresencaGlobal() {
        return lerStream(streamPresenca)
            .mapNotNull(this::deserializarEvento)
            .onErrorResume(this::tratarErroStream);
    }

    /**
     * Inicializa consumer groups
     */
    public Mono<Void> inicializarConsumerGroups() {
        return criarConsumerGroup(streamMensagens)
            .then(criarConsumerGroup(streamPresenca))
            .then(criarConsumerGroup(streamSalas));
    }

    // Métodos auxiliares

    private Mono<Void> publicarEvento(String stream, String tipo, Map<String, Object> dados) {
        Map<String, Object> evento = Map.of(
            "tipo", tipo,
            "timestamp", System.currentTimeMillis(),
            "dados", dados
        );

        return redisTemplate.opsForStream()
            .add(stream, evento)
            .then();
    }

    private Flux<Map<Object, Object>> lerStream(String stream) {
        StreamReadOptions options = StreamReadOptions.empty()
            .block(Duration.ofMillis(blockDuration))
            .count(10);

        return redisTemplate.opsForStream()
            .read(options, StreamOffset.latest(stream))
            .flatMapIterable(records -> records)
            .map(record -> record.getValue());
    }

    private ChatEventDto deserializarEventoMensagem(Map<Object, Object> dados) {
        try {
            String tipo = (String) dados.get("tipo");
            @SuppressWarnings("unchecked")
            Map<String, Object> dadosEvento = (Map<String, Object>) dados.get("dados");
            String mensagemJson = (String) dadosEvento.get("mensagem");
            
            MensagemDto mensagem = objectMapper.readValue(mensagemJson, MensagemDto.class);
            
            return switch (tipo) {
                case "NOVA_MENSAGEM" -> ChatEventDto.novaMensagem(mensagem.sala(), mensagem);
                case "MENSAGEM_EDITADA" -> new ChatEventDto(
                    ChatEventDto.TipoEvento.MENSAGEM_EDITADA, mensagem.sala(),
                    mensagem.dataEnvio(), mensagem, null, null, null
                );
                case "MENSAGEM_EXCLUIDA" -> new ChatEventDto(
                    ChatEventDto.TipoEvento.MENSAGEM_EXCLUIDA, mensagem.sala(),
                    mensagem.dataEnvio(), mensagem, null, null, null
                );
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private ChatEventDto deserializarEvento(Map<Object, Object> dados) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> dadosEvento = (Map<String, Object>) dados.get("dados");
            String eventoJson = (String) dadosEvento.get("evento");
            
            return objectMapper.readValue(eventoJson, ChatEventDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String serializar(Object objeto) {
        try {
            return objectMapper.writeValueAsString(objeto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar objeto", e);
        }
    }

    private Mono<Void> criarConsumerGroup(String stream) {
        return redisTemplate.opsForStream()
            .createGroup(stream, consumerGroup)
            .onErrorResume(error -> Mono.empty()) // Ignora se já existe
            .then();
    }

    private Flux<ChatEventDto> tratarErroStream(Throwable error) {
        // Log do erro e retorna flux vazio para manter stream ativo
        return Flux.empty();
    }
}