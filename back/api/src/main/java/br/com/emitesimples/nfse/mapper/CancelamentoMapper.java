package br.com.emitesimples.nfse.mapper;

import br.com.emitesimples.nfse.domain.Cancelamento;
import br.com.emitesimples.nfse.entity.CancelamentoEntity;
import br.com.emitesimples.nfse.builder.CancelamentoEntityBuilder;
import br.com.emitesimples.nfse.repository.CancelamentoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.usuarios.domain.Usuario;
import br.com.emitesimples.usuarios.mapper.UsuarioMapper;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.usuarios.domain.ClienteAcesso;
import br.com.emitesimples.usuarios.mapper.ClienteAcessoMapper;
import br.com.emitesimples.usuarios.entity.ClienteAcessoEntity;
import br.com.emitesimples.nfse.domain.Nota;
import br.com.emitesimples.nfse.entity.NotaEntity;

@ApplicationScoped
public class CancelamentoMapper {

    @Inject CancelamentoRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;
    @Inject 
    UsuarioMapper usuarioMapper;
    @Inject 
    ClienteAcessoMapper clienteAcessoMapper;
    @Inject 
    NotaMapper notaMapper;

    public Cancelamento entityToApi(CancelamentoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Cancelamento cancelamento = new Cancelamento();

        cancelamento.setVersion( entity.getVersion() );
        cancelamento.setCodigoMotivo( entity.getCodigoMotivo() );
        cancelamento.setMotivo( entity.getMotivo() );
        cancelamento.setDataHoraAprovacao( entity.getDataHoraAprovacao() );
        cancelamento.setDataHoraDescarte( entity.getDataHoraDescarte() );
        cancelamento.setDataHoraRecusa( entity.getDataHoraRecusa() );
        cancelamento.setEstado( entity.getEstado() );
        cancelamento.setCodigoRetorno( entity.getCodigoRetorno() );
        cancelamento.setMensagemRetorno( entity.getMensagemRetorno() );
        cancelamento.setDataHoraEvento( entity.getDataHoraEvento() );
        cancelamento.setXml( entity.getXml() );
        cancelamento.setPdf( entity.getPdf() );
        cancelamento.setId( entity.getId() );
        cancelamento.setAmbiente( entity.getAmbiente() );
        cancelamento.setDataCriacao( entity.getDataCriacao() );
        cancelamento.setDataAtualizacao( entity.getDataAtualizacao() );
        cancelamento.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));
        cancelamento.setPreparadoPorUsuario(usuarioMapper.entityToApi(entity.getPreparadoPorUsuario() ));
        cancelamento.setPreparadoPorCliente(clienteAcessoMapper.entityToApi(entity.getPreparadoPorCliente() ));
        cancelamento.setAprovadoPor(usuarioMapper.entityToApi(entity.getAprovadoPor() ));
        cancelamento.setDescartadoPor(usuarioMapper.entityToApi(entity.getDescartadoPor() ));
        if ( entity.getNota() != null ) {
            Nota notaRef = new Nota();
            notaRef.setId( entity.getNota().getId() );
            cancelamento.setNota( notaRef );
        }

        this.setApiDefaultValues(cancelamento);

        return cancelamento;
    }

    public CancelamentoEntity apiToEntity(Cancelamento api) {
        if ( api == null ) {
            return null;
        }
        CancelamentoEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Cancelamento found for Id: " + api.getId()));
            }
        } else {
            CancelamentoEntityBuilder cancelamentoBuilder = CancelamentoEntity.builderOf();

            cancelamentoBuilder.version = api.getVersion();
            cancelamentoBuilder.codigoMotivo = api.getCodigoMotivo();
            cancelamentoBuilder.motivo = api.getMotivo();
            cancelamentoBuilder.dataHoraAprovacao = api.getDataHoraAprovacao();
            cancelamentoBuilder.dataHoraDescarte = api.getDataHoraDescarte();
            cancelamentoBuilder.dataHoraRecusa = api.getDataHoraRecusa();
            cancelamentoBuilder.estado = api.getEstado();
            cancelamentoBuilder.codigoRetorno = api.getCodigoRetorno();
            cancelamentoBuilder.mensagemRetorno = api.getMensagemRetorno();
            cancelamentoBuilder.dataHoraEvento = api.getDataHoraEvento();
            cancelamentoBuilder.xml = api.getXml();
            cancelamentoBuilder.pdf = api.getPdf();
            cancelamentoBuilder.id = api.getId();
            cancelamentoBuilder.ambiente = api.getAmbiente();
            cancelamentoBuilder.dataCriacao = api.getDataCriacao();
            cancelamentoBuilder.dataAtualizacao = api.getDataAtualizacao();
            cancelamentoBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            cancelamentoBuilder.preparadoPorUsuario = usuarioMapper.apiToEntity(api.getPreparadoPorUsuario());
            cancelamentoBuilder.preparadoPorCliente = clienteAcessoMapper.apiToEntity(api.getPreparadoPorCliente());
            cancelamentoBuilder.aprovadoPor = usuarioMapper.apiToEntity(api.getAprovadoPor());
            cancelamentoBuilder.descartadoPor = usuarioMapper.apiToEntity(api.getDescartadoPor());
            cancelamentoBuilder.nota = notaMapper.apiToEntity(api.getNota());
            entity = cancelamentoBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<Cancelamento> entityListToApiList(List<CancelamentoEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Cancelamento> list = new ArrayList<Cancelamento>( entity.size() );
        for ( CancelamentoEntity cancelamentoEntity : entity ) {
            list.add( entityToApi( cancelamentoEntity ) );
        }

        return list;
    }

    public List<CancelamentoEntity> apiListToEntityList(List<Cancelamento> api) {
        if ( api == null ) {
            return null;
        }

        List<CancelamentoEntity> list = new ArrayList<CancelamentoEntity>( api.size() );
        for ( Cancelamento cancelamento : api ) {
            list.add( apiToEntity( cancelamento ) );
        }

        return list;
    }

    public Cancelamento updateEntityToApi(CancelamentoEntity entity, Cancelamento api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setCodigoMotivo( entity.getCodigoMotivo() );
        api.setMotivo( entity.getMotivo() );
        api.setDataHoraAprovacao( entity.getDataHoraAprovacao() );
        api.setDataHoraDescarte( entity.getDataHoraDescarte() );
        api.setDataHoraRecusa( entity.getDataHoraRecusa() );
        api.setEstado( entity.getEstado() );
        api.setCodigoRetorno( entity.getCodigoRetorno() );
        api.setMensagemRetorno( entity.getMensagemRetorno() );
        api.setDataHoraEvento( entity.getDataHoraEvento() );
        api.setXml( entity.getXml() );
        api.setPdf( entity.getPdf() );
        api.setId( entity.getId() );
        api.setAmbiente( entity.getAmbiente() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

        if ( entity.getEmpresa() != null ) {
            if ( api.getEmpresa() == null ) {
                api.setEmpresa( new Empresa() );
            }
            empresaMapper.updateEntityToApi(entity.getEmpresa(), api.getEmpresa());
        } else {
            api.setEmpresa( null );
        }

        if ( entity.getPreparadoPorUsuario() != null ) {
            if ( api.getPreparadoPorUsuario() == null ) {
                api.setPreparadoPorUsuario( new Usuario() );
            }
            usuarioMapper.updateEntityToApi(entity.getPreparadoPorUsuario(), api.getPreparadoPorUsuario());
        } else {
            api.setPreparadoPorUsuario( null );
        }

        if ( entity.getPreparadoPorCliente() != null ) {
            if ( api.getPreparadoPorCliente() == null ) {
                api.setPreparadoPorCliente( new ClienteAcesso() );
            }
            clienteAcessoMapper.updateEntityToApi(entity.getPreparadoPorCliente(), api.getPreparadoPorCliente());
        } else {
            api.setPreparadoPorCliente( null );
        }

        if ( entity.getAprovadoPor() != null ) {
            if ( api.getAprovadoPor() == null ) {
                api.setAprovadoPor( new Usuario() );
            }
            usuarioMapper.updateEntityToApi(entity.getAprovadoPor(), api.getAprovadoPor());
        } else {
            api.setAprovadoPor( null );
        }

        if ( entity.getDescartadoPor() != null ) {
            if ( api.getDescartadoPor() == null ) {
                api.setDescartadoPor( new Usuario() );
            }
            usuarioMapper.updateEntityToApi(entity.getDescartadoPor(), api.getDescartadoPor());
        } else {
            api.setDescartadoPor( null );
        }
        if ( entity.getNota() != null ) {
            Nota notaRef = new Nota();
            notaRef.setId( entity.getNota().getId() );
            api.setNota( notaRef );
        } else {
            api.setNota( null );
        }

        this.setApiDefaultValues(api);

        return api;
    }

    public CancelamentoEntity updateApiToEntity(Cancelamento api, CancelamentoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Cancelamento found for Id: " + api.getId()));
            }
        } else {
            CancelamentoEntityBuilder cancelamentoBuilder = CancelamentoEntity.builderOf();

            cancelamentoBuilder.version = api.getVersion();
            entity.setCodigoMotivo( api.getCodigoMotivo() );
            entity.setMotivo( api.getMotivo() );
            entity.setDataHoraAprovacao( api.getDataHoraAprovacao() );
            entity.setDataHoraDescarte( api.getDataHoraDescarte() );
            entity.setDataHoraRecusa( api.getDataHoraRecusa() );
            entity.setEstado( api.getEstado() );
            entity.setCodigoRetorno( api.getCodigoRetorno() );
            entity.setMensagemRetorno( api.getMensagemRetorno() );
            entity.setDataHoraEvento( api.getDataHoraEvento() );
            entity.setXml( api.getXml() );
            entity.setPdf( api.getPdf() );
            entity.setId( api.getId() );
            entity.setAmbiente( api.getAmbiente() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            if ( api.getPreparadoPorUsuario() != null ) {
                if ( entity.getPreparadoPorUsuario() == null ) {
                    entity.setPreparadoPorUsuario( new UsuarioEntity() );
                }
                entity.setPreparadoPorUsuario(usuarioMapper.updateApiToEntity( api.getPreparadoPorUsuario(), entity.getPreparadoPorUsuario()));
            } else {
                entity.setPreparadoPorUsuario( null );
            }

            if ( api.getPreparadoPorCliente() != null ) {
                if ( entity.getPreparadoPorCliente() == null ) {
                    entity.setPreparadoPorCliente( new ClienteAcessoEntity() );
                }
                entity.setPreparadoPorCliente(clienteAcessoMapper.updateApiToEntity( api.getPreparadoPorCliente(), entity.getPreparadoPorCliente()));
            } else {
                entity.setPreparadoPorCliente( null );
            }

            if ( api.getAprovadoPor() != null ) {
                if ( entity.getAprovadoPor() == null ) {
                    entity.setAprovadoPor( new UsuarioEntity() );
                }
                entity.setAprovadoPor(usuarioMapper.updateApiToEntity( api.getAprovadoPor(), entity.getAprovadoPor()));
            } else {
                entity.setAprovadoPor( null );
            }

            if ( api.getDescartadoPor() != null ) {
                if ( entity.getDescartadoPor() == null ) {
                    entity.setDescartadoPor( new UsuarioEntity() );
                }
                entity.setDescartadoPor(usuarioMapper.updateApiToEntity( api.getDescartadoPor(), entity.getDescartadoPor()));
            } else {
                entity.setDescartadoPor( null );
            }

            if ( api.getNota() != null ) {
                if ( entity.getNota() == null ) {
                    entity.setNota( new NotaEntity() );
                }
                entity.setNota(notaMapper.updateApiToEntity( api.getNota(), entity.getNota()));
            } else {
                entity.setNota( null );
            }

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public CancelamentoEntity patchApiToEntity(Cancelamento api, CancelamentoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getCodigoMotivo() != null) entity.setCodigoMotivo( api.getCodigoMotivo() ); 
        if (api.getMotivo() != null) entity.setMotivo( api.getMotivo() ); 
        if (api.getDataHoraAprovacao() != null) entity.setDataHoraAprovacao( api.getDataHoraAprovacao() ); 
        if (api.getDataHoraDescarte() != null) entity.setDataHoraDescarte( api.getDataHoraDescarte() ); 
        if (api.getDataHoraRecusa() != null) entity.setDataHoraRecusa( api.getDataHoraRecusa() ); 
        if (api.getEstado() != null) entity.setEstado( api.getEstado() ); 
        if (api.getCodigoRetorno() != null) entity.setCodigoRetorno( api.getCodigoRetorno() ); 
        if (api.getMensagemRetorno() != null) entity.setMensagemRetorno( api.getMensagemRetorno() ); 
        if (api.getDataHoraEvento() != null) entity.setDataHoraEvento( api.getDataHoraEvento() ); 
        if (api.getXml() != null) entity.setXml( api.getXml() ); 
        if (api.getPdf() != null) entity.setPdf( api.getPdf() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getAmbiente() != null) entity.setAmbiente( api.getAmbiente() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());
        if ( api.getPreparadoPorUsuario() != null ) {
            if ( entity.getPreparadoPorUsuario() == null ) {
                entity.setPreparadoPorUsuario( new UsuarioEntity() );
            }
            entity.setPreparadoPorUsuario(usuarioMapper.updateApiToEntity( api.getPreparadoPorUsuario(), entity.getPreparadoPorUsuario()));
        }
        if ( api.getPreparadoPorCliente() != null ) {
            if ( entity.getPreparadoPorCliente() == null ) {
                entity.setPreparadoPorCliente( new ClienteAcessoEntity() );
            }
            entity.setPreparadoPorCliente(clienteAcessoMapper.updateApiToEntity( api.getPreparadoPorCliente(), entity.getPreparadoPorCliente()));
        }
        if ( api.getAprovadoPor() != null ) {
            if ( entity.getAprovadoPor() == null ) {
                entity.setAprovadoPor( new UsuarioEntity() );
            }
            entity.setAprovadoPor(usuarioMapper.updateApiToEntity( api.getAprovadoPor(), entity.getAprovadoPor()));
        }
        if ( api.getDescartadoPor() != null ) {
            if ( entity.getDescartadoPor() == null ) {
                entity.setDescartadoPor( new UsuarioEntity() );
            }
            entity.setDescartadoPor(usuarioMapper.updateApiToEntity( api.getDescartadoPor(), entity.getDescartadoPor()));
        }
        if ( api.getNota() != null ) {
            if ( entity.getNota() == null ) {
                entity.setNota( new NotaEntity() );
            }
            entity.setNota(notaMapper.updateApiToEntity( api.getNota(), entity.getNota()));
        }

        return entity;
    }

    public CancelamentoEntity setEntityDefaultValues(CancelamentoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getEstado() == null) {
            entity.setEstado("PROCESSANDO");
        }
        if (entity.getAmbiente() == null) {
            entity.setAmbiente("HOMOLOGACAO");
        }

        return entity;
    }

    public Cancelamento setApiDefaultValues(Cancelamento api) {
        if ( api == null ) {
            return null;
        }

        if (api.getEstado() == null) {
            api.setEstado("PROCESSANDO");
        }
        if (api.getAmbiente() == null) {
            api.setAmbiente("HOMOLOGACAO");
        }

        return api;
    }

}
