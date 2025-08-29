package br.tec.facilitaservicos.batepapo.infraestrutura.tracing;

import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

/**
 * Interceptor para adicionar tracing aos WebClient calls
 */
@Component
public class TracingWebClientInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TracingWebClientInterceptor.class);
    private final Tracer tracer;

    public TracingWebClientInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Filter function para WebClient que adiciona tracing headers
     */
    public ExchangeFilterFunction tracingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            TraceContext traceContext = tracer.currentTraceContext().context();
            
            if (traceContext != null) {
                String traceId = traceContext.traceId();
                String spanId = traceContext.spanId();
                
                logger.debug("🌐 Outbound request to {}: [traceId={}, spanId={}]", 
                           request.url(), traceId, spanId);
                
                // Adicionar headers de tracing
                ClientRequest tracedRequest = ClientRequest.from(request)
                    .header("X-Trace-Id", traceId)
                    .header("X-Span-Id", spanId)
                    .build();
                    
                return Mono.just(tracedRequest);
            }
            
            logger.debug("🌐 Outbound request to {} without trace context", request.url());
            return Mono.just(request);
        });
    }

    /**
     * Filter function completo com request/response tracing
     */
    public ExchangeFilterFunction fullTracingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            long startTime = System.currentTimeMillis();
            TraceContext traceContext = tracer.currentTraceContext().context();
            
            if (traceContext != null) {
                String traceId = traceContext.traceId();
                String spanId = traceContext.spanId();
                
                logger.debug("🚀 HTTP {} {} [trace={}]", 
                           request.method(), request.url(), traceId);
                
                ClientRequest tracedRequest = ClientRequest.from(request)
                    .header("X-Trace-Id", traceId)
                    .header("X-Span-Id", spanId)
                    .header("X-Request-Start", String.valueOf(startTime))
                    .build();
                    
                return Mono.just(tracedRequest);
            }
            
            return Mono.just(request);
        }).andThen(ExchangeFilterFunction.ofResponseProcessor(response -> {
            var request = response.request();
            String requestStart = request instanceof ClientRequest clientRequest ? 
                clientRequest.headers().getFirst("X-Request-Start") : null;
            if (requestStart != null) {
                long duration = System.currentTimeMillis() - Long.parseLong(requestStart);
                logger.debug("✅ HTTP {} {} - {} ({}ms)", 
                           request.getMethod(), 
                           request.getURI(),
                           response.statusCode().value(),
                           duration);
            }
            return Mono.just(response);
        }));
    }
}