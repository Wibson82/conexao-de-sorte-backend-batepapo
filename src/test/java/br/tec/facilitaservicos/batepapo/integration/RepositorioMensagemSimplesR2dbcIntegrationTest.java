package br.tec.facilitaservicos.batepapo.integration;

import br.tec.facilitaservicos.batepapo.BatepapoSimplesApplication;
import br.tec.facilitaservicos.batepapo.TestConfig;
import br.tec.facilitaservicos.batepapo.dominio.entidade.MensagemSimplesR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.repositorio.RepositorioMensagemSimplesR2dbc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Testes de integração para RepositorioMensagemSimplesR2dbc
 * Valida operações simplificadas do repositório de mensagens
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(classes = {BatepapoSimplesApplication.class, TestConfig.class})
class RepositorioMensagemSimplesR2dbcIntegrationTest {

    @Autowired
    private RepositorioMensagemSimplesR2dbc repositorio;

    @BeforeEach
    void setUp() {
        // Limpa dados de teste anteriores
        repositorio.deleteAll().block();
    }

    @Test
    void deveSalvarERecuperarMensagemSimples() {
        // Given
        MensagemSimplesR2dbc mensagem = MensagemSimplesR2dbc.nova(
            "Mensagem simples de teste",
            123L,
            "usuarioTeste",
            "sala-teste"
        );

        // When & Then
        StepVerifier.create(
            repositorio.save(mensagem)
                .flatMap(mensagemSalva -> repositorio.findById(mensagemSalva.getId()))
        )
        .expectNextMatches(mensagemRecuperada -> 
            mensagemRecuperada.getConteudo().equals("Mensagem simples de teste") &&
            mensagemRecuperada.getUsuarioId().equals(123L) &&
            mensagemRecuperada.getUsuarioNome().equals("usuarioTeste") &&
            mensagemRecuperada.getSala().equals("sala-teste")
        )
        .verifyComplete();
    }

    @Test
    void deveBuscarMensagensRecentesDaSala() {
        // Given
        MensagemSimplesR2dbc mensagem1 = MensagemSimplesR2dbc.nova("Primeira mensagem", 1L, "user1", "sala-teste");
        MensagemSimplesR2dbc mensagem2 = MensagemSimplesR2dbc.nova("Segunda mensagem", 2L, "user2", "sala-teste");
        MensagemSimplesR2dbc mensagem3 = MensagemSimplesR2dbc.nova("Terceira mensagem", 3L, "user3", "sala-teste");
        MensagemSimplesR2dbc mensagemOutraSala = MensagemSimplesR2dbc.nova("Mensagem outra sala", 4L, "user4", "outra-sala");

        // When & Then
        StepVerifier.create(
            Flux.concat(
                repositorio.save(mensagem1),
                repositorio.save(mensagem2),
                repositorio.save(mensagem3),
                repositorio.save(mensagemOutraSala)
            )
            .then()
            .thenMany(repositorio.findMensagensRecentes("sala-teste"))
        )
        .expectNextCount(3) // Deve retornar apenas as 3 mensagens da sala-teste
        .verifyComplete();
    }

    @Test
    void deveBuscarMensagensPorSalaComLimite() {
        // Given - Criar mais de 10 mensagens para testar o limite
        Flux<MensagemSimplesR2dbc> mensagens = Flux.range(1, 15)
            .map(i -> MensagemSimplesR2dbc.nova(
                "Mensagem " + i,
                (long) i,
                "user" + i,
                "sala-teste"
            ))
            .flatMap(repositorio::save);

        // When & Then
        StepVerifier.create(
            mensagens
                .then()
                .thenMany(repositorio.findBySalaOrderByDataEnvioDesc("sala-teste", 10))
        )
        .expectNextCount(10) // Deve retornar apenas 10 mensagens devido ao limite
        .verifyComplete();
    }

    @Test
    void deveContarMensagensPorSala() {
        // Given
        MensagemSimplesR2dbc mensagem1 = MensagemSimplesR2dbc.nova("Mensagem 1", 1L, "user1", "sala-teste");
        MensagemSimplesR2dbc mensagem2 = MensagemSimplesR2dbc.nova("Mensagem 2", 2L, "user2", "sala-teste");
        MensagemSimplesR2dbc mensagem3 = MensagemSimplesR2dbc.nova("Mensagem 3", 3L, "user3", "outra-sala");

        // When & Then
        StepVerifier.create(
            Flux.concat(
                repositorio.save(mensagem1),
                repositorio.save(mensagem2),
                repositorio.save(mensagem3)
            )
            .then(repositorio.countBySala("sala-teste"))
        )
        .expectNext(2L) // Deve contar apenas as 2 mensagens da sala-teste
        .verifyComplete();
    }

    @Test
    void deveCriarMensagemDoSistema() {
        // Given
        MensagemSimplesR2dbc mensagemSistema = MensagemSimplesR2dbc.sistema(
            "Usuário entrou na sala",
            "sala-teste"
        );

        // When & Then
        StepVerifier.create(
            repositorio.save(mensagemSistema)
        )
        .expectNextMatches(mensagem -> 
            mensagem.getConteudo().equals("Usuário entrou na sala") &&
            mensagem.getUsuarioNome().equals("Sistema") &&
            mensagem.getSala().equals("sala-teste") &&
            mensagem.getUsuarioId() == null // Mensagem do sistema não tem usuário
        )
        .verifyComplete();
    }

    @Test
    void deveValidarCamposObrigatorios() {
        // Given - Mensagem com campos nulos
        MensagemSimplesR2dbc mensagemInvalida = new MensagemSimplesR2dbc();
        mensagemInvalida.setConteudo(null); // Campo obrigatório nulo
        mensagemInvalida.setSala("sala-teste");
        mensagemInvalida.setUsuarioId(123L);
        mensagemInvalida.setUsuarioNome("user");

        // When & Then - Deve falhar na validação
        StepVerifier.create(
            repositorio.save(mensagemInvalida)
        )
        .expectError()
        .verify();
    }

    @Test
    void devePermitirBuscaEmSalaVazia() {
        // Given - Nenhuma mensagem salva
        
        // When & Then
        StepVerifier.create(
            repositorio.findMensagensRecentes("sala-inexistente")
        )
        .expectNextCount(0)
        .verifyComplete();
    }

    @Test
    void deveManterOrdemCronologicaDecrescente() {
        // Given
        MensagemSimplesR2dbc mensagem1 = MensagemSimplesR2dbc.nova("Primeira", 1L, "user1", "sala-teste");
        MensagemSimplesR2dbc mensagem2 = MensagemSimplesR2dbc.nova("Segunda", 2L, "user2", "sala-teste");
        MensagemSimplesR2dbc mensagem3 = MensagemSimplesR2dbc.nova("Terceira", 3L, "user3", "sala-teste");

        // When & Then
        StepVerifier.create(
            repositorio.save(mensagem1)
                .delayElement(java.time.Duration.ofMillis(10))
                .then(repositorio.save(mensagem2))
                .delayElement(java.time.Duration.ofMillis(10))
                .then(repositorio.save(mensagem3))
                .thenMany(repositorio.findMensagensRecentes("sala-teste"))
                .take(1) // Pega apenas a primeira (mais recente)
        )
        .expectNextMatches(mensagem -> 
            mensagem.getConteudo().equals("Terceira") // A mais recente deve vir primeiro
        )
        .verifyComplete();
    }
}