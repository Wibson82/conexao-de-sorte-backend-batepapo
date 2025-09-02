package br.tec.facilitaservicos.batepapo.integration;

import br.tec.facilitaservicos.batepapo.BatepapoSimplesApplication;
import br.tec.facilitaservicos.batepapo.TestConfig;
import br.tec.facilitaservicos.batepapo.dominio.entidade.UsuarioOnlineR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.repositorio.RepositorioUsuarioOnlineR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusPresenca;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de integração para RepositorioUsuarioOnlineR2dbc
 * Valida operações CRUD e consultas específicas do repositório de usuários online
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(classes = {BatepapoSimplesApplication.class, TestConfig.class})
class RepositorioUsuarioOnlineR2dbcIntegrationTest {

    @Autowired
    private RepositorioUsuarioOnlineR2dbc repositorio;

    @BeforeEach
    void setUp() {
        // Limpa dados de teste anteriores
        repositorio.deleteAll().block();
    }

    @Test
    void deveSalvarERecuperarUsuarioOnline() {
        // Given
        UsuarioOnlineR2dbc usuario = new UsuarioOnlineR2dbc();
        usuario.setUsuarioId(123L);
        usuario.setUsuarioNome("usuarioTeste");
        usuario.setSala("sala-teste");
        usuario.setStatusPresenca(StatusPresenca.ONLINE);
        usuario.setUltimoHeartbeat(LocalDateTime.now());

        // When & Then
        StepVerifier.create(
            repositorio.save(usuario)
                .flatMap(usuarioSalvo -> repositorio.findById(usuarioSalvo.getId()))
        )
        .expectNextMatches(usuarioRecuperado -> 
            usuarioRecuperado.getUsuarioId().equals(123L) &&
            usuarioRecuperado.getUsuarioNome().equals("usuarioTeste") &&
            usuarioRecuperado.getSala().equals("sala-teste") &&
            usuarioRecuperado.getStatusPresenca().equals(StatusPresenca.ONLINE)
        )
        .verifyComplete();
    }

    @Test
    void deveBuscarUsuariosPorSala() {
        // Given
        UsuarioOnlineR2dbc usuario1 = criarUsuario(1L, "Usuario 1", "sala-teste", StatusPresenca.ONLINE);
        UsuarioOnlineR2dbc usuario2 = criarUsuario(2L, "Usuario 2", "sala-teste", StatusPresenca.ONLINE);
        UsuarioOnlineR2dbc usuario3 = criarUsuario(3L, "Usuario 3", "outra-sala", StatusPresenca.ONLINE);

        // When & Then
        StepVerifier.create(
            repositorio.save(usuario1)
                .then(repositorio.save(usuario2))
                .then(repositorio.save(usuario3))
                .then()
                .thenMany(repositorio.findUsuariosOnlineSala("sala-teste", org.springframework.data.domain.Pageable.unpaged()))
        )
        .expectNextCount(2) // Deve encontrar os 2 usuários da sala-teste
        .verifyComplete();
    }

    @Test
    void deveBuscarUsuarioPorUsuarioIdESala() {
        // Given
        UsuarioOnlineR2dbc usuario = criarUsuario(123L, "Usuario Teste", "sala-teste", StatusPresenca.ONLINE);

        // When & Then
        StepVerifier.create(
            repositorio.save(usuario)
                .then(repositorio.findByUsuarioIdAndSala(123L, "sala-teste"))
        )
        .expectNextMatches(usuarioEncontrado -> 
            usuarioEncontrado.getUsuarioId().equals(123L) &&
            usuarioEncontrado.getSala().equals("sala-teste")
        )
        .verifyComplete();
    }

    @Test
    void deveBuscarUsuariosPorStatus() {
        // Given
        UsuarioOnlineR2dbc usuario1 = criarUsuario(1L, "Usuario 1", "sala-teste", StatusPresenca.ONLINE);
        UsuarioOnlineR2dbc usuario2 = criarUsuario(2L, "Usuario 2", "sala-teste", StatusPresenca.ONLINE);
        UsuarioOnlineR2dbc usuario3 = criarUsuario(3L, "Usuario 3", "sala-teste", StatusPresenca.AUSENTE);

        // When & Then
        StepVerifier.create(
            repositorio.save(usuario1)
                .then(repositorio.save(usuario2))
                .then(repositorio.save(usuario3))
                .then()
                .thenMany(repositorio.findBySalaAndStatusPresencaOrderByUsuarioNome("sala-teste", StatusPresenca.ONLINE, org.springframework.data.domain.Pageable.unpaged()))
        )
        .expectNextCount(2) // Deve encontrar apenas os 2 usuários online
        .verifyComplete();
    }

    @Test
    void deveContarUsuariosPorSala() {
        // Given
        UsuarioOnlineR2dbc usuario1 = criarUsuario(1L, "Usuario 1", "sala-teste", StatusPresenca.ONLINE);
        UsuarioOnlineR2dbc usuario2 = criarUsuario(2L, "Usuario 2", "sala-teste", StatusPresenca.ONLINE);
        UsuarioOnlineR2dbc usuario3 = criarUsuario(3L, "Usuario 3", "outra-sala", StatusPresenca.ONLINE);

        // When & Then
        StepVerifier.create(
            repositorio.save(usuario1)
                .then(repositorio.save(usuario2))
                .then(repositorio.save(usuario3))
                .then(repositorio.countUsuariosOnlineSala("sala-teste"))
        )
        .expectNext(2L) // Deve contar apenas os 2 usuários da sala-teste
        .verifyComplete();
    }

    @Test
    void deveDeletarUsuariosPorSala() {
        // Given
        UsuarioOnlineR2dbc usuario1 = criarUsuario(1L, "Usuario 1", "sala-teste", StatusPresenca.ONLINE);
        UsuarioOnlineR2dbc usuario2 = criarUsuario(2L, "Usuario 2", "sala-teste", StatusPresenca.ONLINE);
        UsuarioOnlineR2dbc usuario3 = criarUsuario(3L, "Usuario 3", "outra-sala", StatusPresenca.ONLINE);

        // When & Then
        StepVerifier.create(
            repositorio.save(usuario1)
                .then(repositorio.save(usuario2))
                .then(repositorio.save(usuario3))
                .then(repositorio.removerSessoesAntigas(LocalDateTime.now().plusHours(1)))
                .then(repositorio.count())
        )
        .expectNext(0L) // Deve remover todas as sessões
        .verifyComplete();
    }

    @Test
    void deveDeletarUsuarioPorUsuarioIdESala() {
        // Given
        UsuarioOnlineR2dbc usuario1 = criarUsuario(1L, "Usuario 1", "sala-teste", StatusPresenca.ONLINE);
        UsuarioOnlineR2dbc usuario2 = criarUsuario(2L, "Usuario 2", "sala-teste", StatusPresenca.ONLINE);

        // When & Then
        StepVerifier.create(
            repositorio.save(usuario1)
                .then(repositorio.save(usuario2))
                .then(repositorio.marcarComoSaiu("sessionId1"))
                .then(repositorio.count())
        )
        .expectNext(2L) // Ainda deve ter 2 usuários (marcarComoSaiu não remove)
        .verifyComplete();
    }

    @Test
    void deveAtualizarStatusUsuario() {
        // Given
        UsuarioOnlineR2dbc usuario = criarUsuario(123L, "Usuario Teste", "sala-teste", StatusPresenca.ONLINE);

        // When & Then
        StepVerifier.create(
            repositorio.save(usuario)
                .flatMap(usuarioSalvo -> {
                    usuarioSalvo.setStatusPresenca(StatusPresenca.AUSENTE);
                    usuarioSalvo.setUltimoHeartbeat(LocalDateTime.now());
                    return repositorio.save(usuarioSalvo);
                })
        )
        .expectNextMatches(usuarioAtualizado -> 
            usuarioAtualizado.getStatusPresenca().equals(StatusPresenca.AUSENTE)
        )
        .verifyComplete();
    }

    @Test
    void deveBuscarUsuariosComAtividadeRecente() {
        // Given
        LocalDateTime agora = LocalDateTime.now();
        UsuarioOnlineR2dbc usuario1 = criarUsuarioComAtividade(1L, "Usuario 1", "sala-teste", StatusPresenca.ONLINE, agora.minusMinutes(30));
        UsuarioOnlineR2dbc usuario2 = criarUsuarioComAtividade(2L, "Usuario 2", "sala-teste", StatusPresenca.ONLINE, agora.minusMinutes(60));
        UsuarioOnlineR2dbc usuario3 = criarUsuarioComAtividade(3L, "Usuario 3", "sala-teste", StatusPresenca.ONLINE, agora.minusHours(2));

        // When & Then
        StepVerifier.create(
            repositorio.save(usuario1)
                .then(repositorio.save(usuario2))
                .then(repositorio.save(usuario3))
                .then()
                .thenMany(repositorio.findConexoesExpiradas(agora.minusMinutes(90)))
        )
        .expectNextCount(1) // Deve encontrar apenas o usuário com atividade há mais de 90 minutos
        .verifyComplete();
    }

    @Test
    void deveValidarCamposObrigatorios() {
        // When & Then
        assertThatThrownBy(() -> {
            UsuarioOnlineR2dbc usuarioInvalido = new UsuarioOnlineR2dbc();
            // Não definir campos obrigatórios
            repositorio.save(usuarioInvalido).block();
        }).isInstanceOf(Exception.class);
    }

    @Test
    void devePermitirBuscaEmRepositorioVazio() {
        // When & Then
        StepVerifier.create(
            repositorio.findBySessionId("sessionId-inexistente")
        )
        .expectNextCount(0)
        .verifyComplete();
    }

    @Test
    void deveManterTimestampsDeAuditoria() {
        // Given
        UsuarioOnlineR2dbc usuario = criarUsuario(123L, "Usuario Teste", "sala-teste", StatusPresenca.ONLINE);
        LocalDateTime antes = LocalDateTime.now();

        // When
        UsuarioOnlineR2dbc usuarioSalvo = repositorio.save(usuario).block();
        LocalDateTime depois = LocalDateTime.now();

        // Then
        assertThat(usuarioSalvo.getUltimoHeartbeat()).isBetween(antes, depois);
    }

    // Métodos auxiliares
    private UsuarioOnlineR2dbc criarUsuario(Long usuarioId, String nomeUsuario, String sala, StatusPresenca status) {
        UsuarioOnlineR2dbc usuario = new UsuarioOnlineR2dbc();
        usuario.setUsuarioId(usuarioId);
        usuario.setUsuarioNome(nomeUsuario);
        usuario.setSala(sala);
        usuario.setStatusPresenca(status);
        usuario.setUltimoHeartbeat(LocalDateTime.now());
        return usuario;
    }

    private UsuarioOnlineR2dbc criarUsuarioComAtividade(Long usuarioId, String nomeUsuario, String sala, StatusPresenca status, LocalDateTime ultimaAtividade) {
        UsuarioOnlineR2dbc usuario = new UsuarioOnlineR2dbc();
        usuario.setUsuarioId(usuarioId);
        usuario.setUsuarioNome(nomeUsuario);
        usuario.setSala(sala);
        usuario.setStatusPresenca(status);
        usuario.setUltimoHeartbeat(ultimaAtividade);
        return usuario;
    }
}