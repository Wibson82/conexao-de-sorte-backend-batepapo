package br.tec.facilitaservicos.batepapo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(classes = {BatepapoSimplesApplication.class, TestConfig.class})
public class SimpleHealthTest {

    @Test
    void contextLoads() {
        // Test que garante que o contexto Spring carrega corretamente
        // Suficiente para validar que o microserviço está funcional
    }
}