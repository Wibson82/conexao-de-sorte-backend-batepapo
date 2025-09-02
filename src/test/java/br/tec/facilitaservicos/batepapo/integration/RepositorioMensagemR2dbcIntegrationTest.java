package br.tec.facilitaservicos.batepapo.integration;

import br.tec.facilitaservicos.batepapo.BatepapoSimplesApplication;
import br.tec.facilitaservicos.batepapo.TestConfig;
import br.tec.facilitaservicos.batepapo.dominio.entidade.MensagemR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.repositorio.RepositorioMensagemR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusMensagem;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoMensagem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

/**
 * Testes de integração para RepositorioMensagemR2dbc
 * Valida operações CRUD e consultas específicas do repositório
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(classes = {BatepapoSimplesApplication.class, TestConfig.class})
class RepositorioMensagemR2dbcIntegrationTest {

    @Autowired
    private RepositorioMensagemR2dbc repositorio;

    private MensagemR2dbc mensagemTeste;

    @BeforeEach
    void setUp() {
        // Limpa dados de teste anteriores
        repositorio.deleteAll().block();
        
        // Cria mensagem de teste
        mensagemTeste = new MensagemR2dbc(
            "Mensagem de teste para integração",
            999L,
            "usuarioTeste",
            "sala-teste"
        );
    }

    @Test
    void deveSalvarERecuperarMensagem() {
        // Given & When & Then
        StepVerifier.create(
            repositorio.save(mensagemTeste)
                .flatMap(mensagemSalva -> repositorio.findById(mensagemSalva.getId()))
        )
        .expectNextMatches(mensagem -> 
            mensagem.getConteudo().equals("Mensagem de teste para integração") &&
            mensagem.getUsuarioId().equals(999L) &&
            mensagem.getUsuarioNome().equals("usuarioTeste") &&
            mensagem.getSala().equals("sala-teste")
        )
        .verifyComplete();
    }

    @Test
    void deveBuscarMensagensPorSala() {
        // Given
        MensagemR2dbc mensagem1 = new MensagemR2dbc("Primeira mensagem", 1L, "user1", "sala-teste");
        MensagemR2dbc mensagem2 = new MensagemR2dbc("Segunda mensagem", 2L, "user2", "sala-teste");
        MensagemR2dbc mensagem3 = new MensagemR2dbc("Mensagem outra sala", 3L, "user3", "outra-sala");

        // When & Then
        StepVerifier.create(
            Flux.concat(
                repositorio.save(mensagem1),
                repositorio.save(mensagem2),
                repositorio.save(mensagem3)
            )
            .then()
            .thenMany(repositorio.findBySalaOrderByDataEnvioDesc("sala-teste", PageRequest.of(0, 10)))
        )
        .expectNextCount(2)
        .verifyComplete();
    }

    @Test
    void deveBuscarMensagensPorUsuarioESala() {
        // Given
        MensagemR2dbc mensagem1 = new MensagemR2dbc("Mensagem user1", 1L, "user1", "sala-teste");
        MensagemR2dbc mensagem2 = new MensagemR2dbc("Outra mensagem user1", 1L, "user1", "sala-teste");
        MensagemR2dbc mensagem3 = new MensagemR2dbc("Mensagem user2", 2L, "user2", "sala-teste");

        // When & Then
        StepVerifier.create(
            Flux.concat(
                repositorio.save(mensagem1),
                repositorio.save(mensagem2),
                repositorio.save(mensagem3)
            )
            .then()
            .thenMany(repositorio.findByUsuarioIdAndSalaOrderByDataEnvioDesc(1L, "sala-teste", PageRequest.of(0, 10)))
        )
        .expectNextCount(2)
        .verifyComplete();
    }

    @Test
    void deveBuscarMensagensPorTipo() {
        // Given
        MensagemR2dbc mensagemTexto = new MensagemR2dbc("Mensagem texto", 1L, "user1", "sala-teste");
        mensagemTexto.setTipo(TipoMensagem.TEXTO);
        
        MensagemR2dbc mensagemSistema = new MensagemR2dbc("Mensagem sistema", 1L, "system", "sala-teste");
        mensagemSistema.setTipo(TipoMensagem.SISTEMA);

        // When & Then
        StepVerifier.create(
            Flux.concat(
                repositorio.save(mensagemTexto),
                repositorio.save(mensagemSistema)
            )
            .then()
            .thenMany(repositorio.findBySalaAndTipoOrderByDataEnvioDesc("sala-teste", TipoMensagem.TEXTO, PageRequest.of(0, 10)))
        )
        .expectNextCount(1)
        .verifyComplete();
    }

    @Test
    void deveBuscarUltimaMensagemDaSala() {
        // Given
        MensagemR2dbc mensagem1 = new MensagemR2dbc("Primeira mensagem", 1L, "user1", "sala-teste");
        MensagemR2dbc mensagem2 = new MensagemR2dbc("Última mensagem", 2L, "user2", "sala-teste");

        // When & Then
        StepVerifier.create(
            repositorio.save(mensagem1)
                .delayElement(java.time.Duration.ofMillis(10)) // Garante ordem temporal
                .then(repositorio.save(mensagem2))
                .then(repositorio.findUltimaMensagemSala("sala-teste"))
        )
        .expectNextMatches(mensagem -> 
            mensagem.getConteudo().equals("Última mensagem")
        )
        .verifyComplete();
    }

    @Test
    void deveContarMensagensDaSala() {
        // Given
        MensagemR2dbc mensagem1 = new MensagemR2dbc("Mensagem 1", 1L, "user1", "sala-teste");
        MensagemR2dbc mensagem2 = new MensagemR2dbc("Mensagem 2", 2L, "user2", "sala-teste");
        MensagemR2dbc mensagem3 = new MensagemR2dbc("Mensagem outra sala", 3L, "user3", "outra-sala");

        // When & Then
        StepVerifier.create(
            Flux.concat(
                repositorio.save(mensagem1),
                repositorio.save(mensagem2),
                repositorio.save(mensagem3)
            )
            .then(repositorio.count())
        )
        .expectNext(3L)
        .verifyComplete();
    }

    @Test
    void deveAtualizarMensagem() {
        // Given
        String novoConteudo = "Conteúdo atualizado";

        // When & Then
        StepVerifier.create(
            repositorio.save(mensagemTeste)
                .flatMap(mensagemSalva -> {
                    mensagemSalva.setConteudo(novoConteudo);
                    mensagemSalva.setEditada(true);
                    mensagemSalva.setDataEdicao(LocalDateTime.now());
                    return repositorio.save(mensagemSalva);
                })
        )
        .expectNextMatches(mensagem -> 
            mensagem.getConteudo().equals(novoConteudo) &&
            mensagem.getEditada() &&
            mensagem.getDataEdicao() != null
        )
        .verifyComplete();
    }

    @Test
    void deveDeletarMensagem() {
        // Given & When & Then
        StepVerifier.create(
            repositorio.save(mensagemTeste)
                .flatMap(mensagemSalva -> 
                    repositorio.deleteById(mensagemSalva.getId())
                        .then(repositorio.findById(mensagemSalva.getId()))
                )
        )
        .verifyComplete();
    }
}