package br.tec.facilitaservicos.batepapo.infraestrutura.configuracao;

import br.tec.facilitaservicos.batepapo.infraestrutura.tracing.TracingWebClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private final TracingWebClientInterceptor tracingInterceptor;

    public WebClientConfig(TracingWebClientInterceptor tracingInterceptor) {
        this.tracingInterceptor = tracingInterceptor;
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .filter(tracingInterceptor.fullTracingFilter())
            .build();
    }
}