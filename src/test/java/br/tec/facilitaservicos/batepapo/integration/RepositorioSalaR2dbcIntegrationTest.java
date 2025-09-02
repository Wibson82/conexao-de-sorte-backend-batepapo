package br.tec.facilitaservicos.batepapo.integration;

import br.tec.facilitaservicos.batepapo.BatepapoSimplesApplication;
import br.tec.facilitaservicos.batepapo.TestConfig;
import br.tec.facilitaservicos.batepapo.dominio.entidade.SalaR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.repositorio.RepositorioSalaR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusSala;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoSala;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

/**
 * Testes de integração para RepositorioSalaR2dbc
 * Valida operações CRUD e consultas específicas do repositório de salas
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(classes = {BatepapoSimplesApplication.class, TestConfig.class})
class RepositorioSalaR2dbcIntegrationTest {

    @Autowired
    private RepositorioSalaR2dbc repositorio;

    @BeforeEach
    void setUp() {
        // Limpa dados de teste anteriores
        repositorio.deleteAll().block();
    }

    @Test
    void deveSalvarERecuperarSala() {
        // Given
        SalaR2dbc sala = new SalaR2dbc();
        sala.setNome("Sala de Teste");
        sala.setDescricao("Descrição da sala de teste");
        sala.setTipo(TipoSala.PUBLICA);
        sala.setStatus(StatusSala.ATIVA);
        sala.setMaxUsuarios(50);
        sala.setCriadaPor(123L);

        // When & Then
        StepVerifier.create(
            repositorio.save(sala)
                .flatMap(salaSalva -> repositorio.findById(salaSalva.getId()))
        )
        .expectNextMatches(salaRecuperada -> 
            salaRecuperada.getNome().equals("Sala de Teste") &&
            salaRecuperada.getDescricao().equals("Descrição da sala de teste") &&
            salaRecuperada.isAtiva() &&
            salaRecuperada.isPublica() &&
            salaRecuperada.getMaxUsuarios() == 50 &&
            salaRecuperada.getCriadaPor().equals(123L)
        )
        .verifyComplete();
    }

    @Test
    void deveBuscarSalasPorNome() {
        // Given
        SalaR2dbc sala1 = criarSala("Sala Java", "Discussões sobre Java", StatusSala.ATIVA);
        SalaR2dbc sala2 = criarSala("Sala Python", "Discussões sobre Python", StatusSala.ATIVA);
        SalaR2dbc sala3 = criarSala("Java Avançado", "Tópicos avançados de Java", StatusSala.ATIVA);

        // When & Then
        StepVerifier.create(
            Flux.concat(
                repositorio.save(sala1),
                repositorio.save(sala2),
                repositorio.save(sala3)
            )
            .then()
            .thenMany(repositorio.buscarSalasPorNome("java", PageRequest.of(0, 10)))
        )
        .expectNextCount(2) // Deve encontrar "Sala Java" e "Java Avançado"
        .verifyComplete();
    }

    @Test
    void deveBuscarSalasAtivas() {
        // Given
        SalaR2dbc salaAtiva1 = criarSala("Sala Ativa 1", "Primeira sala ativa", StatusSala.ATIVA);
        SalaR2dbc salaAtiva2 = criarSala("Sala Ativa 2", "Segunda sala ativa", StatusSala.ATIVA);
        SalaR2dbc salaInativa = criarSala("Sala Inativa", "Sala desativada", StatusSala.INATIVA);

        // When & Then
        StepVerifier.create(
            Flux.concat(
                repositorio.save(salaAtiva1),
                repositorio.save(salaAtiva2),
                repositorio.save(salaInativa)
            )
            .then()
            .thenMany(repositorio.findSalasAtivas(PageRequest.of(0, 10)))
        )
        .expectNextCount(2) // Deve encontrar apenas as 2 salas ativas
        .verifyComplete();
    }

    @Test
    void deveBuscarSalasPublicasAtivas() {
        // Given
        SalaR2dbc salaPublica1 = criarSalaComTipo("Sala Pública 1", TipoSala.PUBLICA);
        SalaR2dbc salaPublica2 = criarSalaComTipo("Sala Pública 2", TipoSala.GERAL);
        SalaR2dbc salaPrivada = criarSalaComTipo("Sala Privada", TipoSala.PRIVADA);

        // When & Then
        StepVerifier.create(
            Flux.concat(
                repositorio.save(salaPublica1),
                repositorio.save(salaPublica2),
                repositorio.save(salaPrivada)
            )
            .then()
            .thenMany(repositorio.findSalasPublicasAtivas(PageRequest.of(0, 10)))
        )
        .expectNextCount(2) // Deve encontrar apenas as 2 salas públicas
        .verifyComplete();
    }

    @Test
    void deveBuscarSalasPorCriador() {
        // Given
        Long criadorId = 123L;
        SalaR2dbc sala1 = criarSalaPorCriador("Sala do Criador 1", criadorId);
        SalaR2dbc sala2 = criarSalaPorCriador("Sala do Criador 2", criadorId);
        SalaR2dbc salaOutroCriador = criarSalaPorCriador("Sala de Outro", 456L);

        // When & Then
        StepVerifier.create(
            Flux.concat(
                repositorio.save(sala1),
                repositorio.save(sala2),
                repositorio.save(salaOutroCriador)
            )
            .then()
            .thenMany(repositorio.findByCriadaPorOrderByDataCriacaoDesc(criadorId, PageRequest.of(0, 10)))
        )
        .expectNextCount(2) // Deve encontrar apenas as 2 salas do criador específico
        .verifyComplete();
    }

    @Test
    void deveAtualizarSala() {
        // Given
        SalaR2dbc sala = criarSala("Sala Original", "Descrição original", StatusSala.ATIVA);

        // When & Then
        StepVerifier.create(
            repositorio.save(sala)
                .flatMap(salaSalva -> {
                    salaSalva.setNome("Sala Atualizada");
                    salaSalva.setDescricao("Descrição atualizada");
                    salaSalva.setStatus(StatusSala.INATIVA);
                    return repositorio.save(salaSalva);
                })
        )
        .expectNextMatches(salaAtualizada -> 
            salaAtualizada.getNome().equals("Sala Atualizada") &&
            salaAtualizada.getDescricao().equals("Descrição atualizada") &&
            !salaAtualizada.isAtiva()
        )
        .verifyComplete();
    }

    @Test
    void deveDeletarSala() {
        // Given
        SalaR2dbc sala = criarSala("Sala para Deletar", "Será deletada", StatusSala.ATIVA);

        // When & Then
        StepVerifier.create(
            repositorio.save(sala)
                .flatMap(salaSalva -> 
                    repositorio.deleteById(salaSalva.getId())
                        .then(repositorio.findById(salaSalva.getId()))
                )
        )
        .expectNextCount(0) // Não deve encontrar a sala deletada
        .verifyComplete();
    }

    @Test
    void deveContarSalasAtivas() {
        // Given
        SalaR2dbc salaAtiva1 = criarSala("Ativa 1", "Primeira ativa", StatusSala.ATIVA);
        SalaR2dbc salaAtiva2 = criarSala("Ativa 2", "Segunda ativa", StatusSala.ATIVA);
        SalaR2dbc salaInativa = criarSala("Inativa", "Sala inativa", StatusSala.INATIVA);

        // When & Then
        StepVerifier.create(
            Flux.concat(
                repositorio.save(salaAtiva1),
                repositorio.save(salaAtiva2),
                repositorio.save(salaInativa)
            )
            .then(repositorio.countSalasAtivas())
        )
        .expectNext(2L) // Deve contar apenas as 2 salas ativas
        .verifyComplete();
    }

    @Test
    void deveValidarCamposObrigatorios() {
        // Given - Sala com nome nulo
        SalaR2dbc salaInvalida = new SalaR2dbc();
        salaInvalida.setNome(null); // Campo obrigatório nulo
        salaInvalida.setDescricao("Descrição válida");
        salaInvalida.setStatus(StatusSala.ATIVA);
        salaInvalida.setCriadaPor(123L);

        // When & Then - Deve falhar na validação
        StepVerifier.create(
            repositorio.save(salaInvalida)
        )
        .expectError()
        .verify();
    }

    @Test
    void devePermitirBuscaEmRepositorioVazio() {
        // Given - Nenhuma sala salva
        
        // When & Then
        StepVerifier.create(
            repositorio.findSalasAtivas(PageRequest.of(0, 10))
        )
        .expectNextCount(0)
        .verifyComplete();
    }

    @Test
    void deveManterTimestampsDeAuditoria() {
        // Given
        SalaR2dbc sala = criarSala("Sala com Auditoria", "Teste de timestamps", StatusSala.ATIVA);
        LocalDateTime antes = LocalDateTime.now();

        // When & Then
        StepVerifier.create(
            repositorio.save(sala)
        )
        .expectNextMatches(salaSalva -> {
            LocalDateTime depois = LocalDateTime.now();
            return salaSalva.getDataCriacao() != null &&
                   !salaSalva.getDataCriacao().isBefore(antes) &&
                   !salaSalva.getDataCriacao().isAfter(depois);
        })
        .verifyComplete();
    }

    @Test
    void deveBuscarSalasPorTipo() {
        // Given
        SalaR2dbc salaPublica = criarSalaComTipo("Sala Pública", TipoSala.PUBLICA);
        SalaR2dbc salaGeral = criarSalaComTipo("Sala Geral", TipoSala.GERAL);
        SalaR2dbc salaPrivada = criarSalaComTipo("Sala Privada", TipoSala.PRIVADA);

        // When & Then
        StepVerifier.create(
            Flux.concat(
                repositorio.save(salaPublica),
                repositorio.save(salaGeral),
                repositorio.save(salaPrivada)
            )
            .then()
            .thenMany(repositorio.findByTipoOrderByNome(TipoSala.PUBLICA, PageRequest.of(0, 10)))
        )
        .expectNextCount(1) // Deve encontrar apenas a sala pública
        .verifyComplete();
    }

    @Test
    void deveVerificarExistenciaPorNome() {
        // Given
        SalaR2dbc sala = criarSala("Sala Única", "Descrição única", StatusSala.ATIVA);

        // When & Then
        StepVerifier.create(
            repositorio.save(sala)
                .then(repositorio.existsByNome("Sala Única"))
        )
        .expectNext(true)
        .verifyComplete();

        // Verificar nome inexistente
        StepVerifier.create(
            repositorio.existsByNome("Sala Inexistente")
        )
        .expectNext(false)
        .verifyComplete();
    }

    // Métodos auxiliares para criação de salas de teste
    private SalaR2dbc criarSala(String nome, String descricao, StatusSala status) {
        SalaR2dbc sala = new SalaR2dbc();
        sala.setNome(nome);
        sala.setDescricao(descricao);
        sala.setTipo(TipoSala.PUBLICA);
        sala.setStatus(status);
        sala.setMaxUsuarios(50);
        sala.setCriadaPor(123L);
        return sala;
    }

    private SalaR2dbc criarSalaComTipo(String nome, TipoSala tipo) {
        SalaR2dbc sala = new SalaR2dbc();
        sala.setNome(nome);
        sala.setDescricao("Descrição de " + nome);
        sala.setTipo(tipo);
        sala.setStatus(StatusSala.ATIVA);
        sala.setMaxUsuarios(50);
        sala.setCriadaPor(123L);
        return sala;
    }

    private SalaR2dbc criarSalaPorCriador(String nome, Long criadaPor) {
        SalaR2dbc sala = new SalaR2dbc();
        sala.setNome(nome);
        sala.setDescricao("Descrição de " + nome);
        sala.setTipo(TipoSala.PUBLICA);
        sala.setStatus(StatusSala.ATIVA);
        sala.setMaxUsuarios(50);
        sala.setCriadaPor(criadaPor);
        return sala;
    }
}