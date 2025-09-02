package br.tec.facilitaservicos.batepapo;

import br.tec.facilitaservicos.batepapo.config.TestR2dbcConfig;
import br.tec.facilitaservicos.batepapo.config.TestSecurityConfig;
import br.tec.facilitaservicos.batepapo.infraestrutura.cliente.AuthServiceClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * Configuração de teste para mocks e beans simplificados
 */
@TestConfiguration
@Import({TestR2dbcConfig.class, TestSecurityConfig.class})
public class TestConfig {

    @Bean
    @Primary
    public AuthServiceClient mockAuthServiceClient() {
        AuthServiceClient mock = Mockito.mock(AuthServiceClient.class);
        
        // Configure mock behavior
        Mockito.when(mock.isUserOnline(Mockito.any()))
                .thenReturn(Mono.just(true));
                
        Mockito.when(mock.validateToken(Mockito.any()))
                .thenReturn(Mono.just(true));
                
        Mockito.when(mock.getUserInfo(Mockito.any()))
                .thenReturn(Mono.just(Map.of("id", 1L, "username", "testuser", "active", true)));
                
        Mockito.when(mock.isAuthServiceHealthy())
                .thenReturn(Mono.just(true));
                
        return mock;
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public ReactiveRedisTemplate<String, Object> mockReactiveRedisTemplate() {
        return Mockito.mock(ReactiveRedisTemplate.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public StreamReceiver<String, MapRecord<String, String, String>> mockStreamReceiver() {
        return Mockito.mock(StreamReceiver.class);
    }
}