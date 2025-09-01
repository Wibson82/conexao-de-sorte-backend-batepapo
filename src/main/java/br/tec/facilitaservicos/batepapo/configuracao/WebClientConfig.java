package br.tec.facilitaservicos.batepapo.configuracao;

import br.tec.facilitaservicos.batepapo.infraestrutura.tracing.TracingWebClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuração consolidada do WebClient para comunicação entre microserviços
 */
@Configuration
public class WebClientConfig {

    private final TracingWebClientInterceptor tracingInterceptor;

    public WebClientConfig(TracingWebClientInterceptor tracingInterceptor) {
        this.tracingInterceptor = tracingInterceptor;
    }

    /**
     * WebClient Builder configurado para integração com outros microserviços
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(10))
            .doOnConnected(connection -> 
                connection.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                          .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
            );

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(tracingInterceptor.fullTracingFilter())
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(1024 * 1024) // 1MB
            );
    }

    /**
     * WebClient pré-configurado com tracing
     */
    @Bean
    public WebClient webClient() {
        return webClientBuilder().build();
    }
}