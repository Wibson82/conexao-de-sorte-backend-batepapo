package br.tec.facilitaservicos.batepapo.aplicacao.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDto;
import br.tec.facilitaservicos.batepapo.dominio.entidade.MensagemR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusMensagem;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoMensagem;

/**
 * ============================================================================
 * 💬 MAPPER DE MENSAGEM
 * ============================================================================
 * 
 * Mapper para conversão entre entidade R2DBC e DTO:
 * - MensagemR2dbc ↔ MensagemDto
 * - Validações e transformações
 * - Formatação de dados
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Component
public class MensagemMapper {

    /**
     * Converte entidade R2DBC para DTO
     */
    public MensagemDto paraDto(MensagemR2dbc entidade) {
        if (entidade == null) {
            return null;
        }
        
        return new MensagemDto(
            entidade.getId(),
            entidade.getConteudo(),
            entidade.getUsuarioId(),
            entidade.getUsuarioNome(),
            entidade.getSala(),
            MensagemDto.TipoMensagem.valueOf(entidade.getTipo().name()),
            MensagemDto.StatusMensagem.valueOf(entidade.getStatus().name()),
            entidade.getRespostaParaId(),
            entidade.getDataEnvio(),
            entidade.getDataEdicao(),
            entidade.getEditado(),
            entidade.getAnexos()
        );
    }

    /**
     * Converte DTO para entidade R2DBC
     */
    public MensagemR2dbc paraEntidade(MensagemDto dto) {
        if (dto == null) {
            return null;
        }
        
        return MensagemR2dbc.builder()
            .id(dto.id())
            .conteudo(dto.conteudo())
            .usuario(dto.usuarioId(), dto.usuarioNome())
            .sala(dto.sala())
            .tipo(TipoMensagem.valueOf(dto.tipo().name()))
            .respostaParaId(dto.respostaParaId())
            .anexos(dto.anexos())
            .build();
    }
    
    /**
     * Atualiza entidade com dados do DTO
     */
    public void atualizarEntidade(MensagemR2dbc entidade, MensagemDto dto) {
        if (entidade == null || dto == null) {
            return;
        }
        
        // Apenas alguns campos podem ser atualizados
        if (dto.conteudo() != null && !dto.conteudo().equals(entidade.getConteudo())) {
            entidade.setConteudo(dto.conteudo());
            entidade.marcarComoEditada();
        }
        
        if (dto.anexos() != null) {
            entidade.setAnexos(dto.anexos());
        }
    }
    
    /**
     * Cria DTO com dados mínimos para resposta rápida
     */
    public MensagemDto criarDtoMinimo(Long id, String conteudo, Long usuarioId, String usuarioNome, String sala) {
        return new MensagemDto(
            id,
            conteudo,
            usuarioId,
            usuarioNome,
            sala,
            MensagemDto.TipoMensagem.TEXTO,
            MensagemDto.StatusMensagem.ENVIADA,
            null,
            LocalDateTime.now(),
            null,
            false,
            null
        );
    }
    
    /**
     * Valida se DTO tem dados obrigatórios
     */
    public boolean isValido(MensagemDto dto) {
        return dto != null 
            && dto.conteudo() != null 
            && !dto.conteudo().trim().isEmpty()
            && dto.usuarioId() != null
            && dto.usuarioNome() != null
            && dto.sala() != null;
    }
}