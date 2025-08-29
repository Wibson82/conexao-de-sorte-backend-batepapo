package br.tec.facilitaservicos.batepapo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Aplicação simplificada do Microserviço de Bate-papo
 * 
 * Versão Mínima Viável focada no core:
 * - Enviar mensagens
 * - Listar mensagens  
 * - Integração básica com Auth Service
 */
@SpringBootApplication
@EnableR2dbcRepositories
public class BatepapoSimplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatepapoSimplesApplication.class, args);
    }
}