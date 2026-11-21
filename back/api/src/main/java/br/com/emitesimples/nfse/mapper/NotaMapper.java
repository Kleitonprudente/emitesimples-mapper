package br.com.emitesimples.nfse.mapper;

import br.com.emitesimples.nfse.domain.Nota;
import br.com.emitesimples.nfse.entity.NotaEntity;
import br.com.emitesimples.nfse.builder.NotaEntityBuilder;
import br.com.emitesimples.nfse.repository.NotaRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.cadastros.domain.Pessoa;
import br.com.emitesimples.cadastros.mapper.PessoaMapper;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.cadastros.domain.Servico;
import br.com.emitesimples.cadastros.mapper.ServicoMapper;
import br.com.emitesimples.cadastros.entity.ServicoEntity;
import br.com.emitesimples.usuarios.domain.Usuario;
import br.com.emitesimples.usuarios.mapper.UsuarioMapper;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.usuarios.domain.ClienteAcesso;
import br.com.emitesimples.usuarios.mapper.ClienteAcessoMapper;
import br.com.emitesimples.usuarios.entity.ClienteAcessoEntity;
import br.com.emitesimples.nfse.domain.Cancelamento;
import br.com.emitesimples.nfse.entity.CancelamentoEntity;

@ApplicationScoped
public class NotaMapper {

    @Inject NotaRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;
    @Inject 
    PessoaMapper pessoaMapper;
    @Inject 
    ServicoMapper servicoMapper;
    @Inject 
    UsuarioMapper usuarioMapper;
    @Inject 
    ClienteAcessoMapper clienteAcessoMapper;
    @Inject 
    CancelamentoMapper cancelamentoMapper;

    public Nota entityToApi(NotaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Nota nota = new Nota();

        nota.setVersion( entity.getVersion() );
        nota.setNumero( entity.getNumero() );
        nota.setSerie( entity.getSerie() );
        nota.setDataEmissao( entity.getDataEmissao() );
        nota.setDataCompetencia( entity.getDataCompetencia() );
        nota.setInformacoesComplementares( entity.getInformacoesComplementares() );
        nota.setRegimeApuracao( entity.getRegimeApuracao() );
        nota.setRegimeTributacao( entity.getRegimeTributacao() );
        nota.setCodigoServico( entity.getCodigoServico() );
        nota.setCodigoTributacaoMunicipio( entity.getCodigoTributacaoMunicipio() );
        nota.setCodigoNbs( entity.getCodigoNbs() );
        nota.setDiscriminacao( entity.getDiscriminacao() );
        nota.setDescricao( entity.getDescricao() );
        nota.setValorServicos( entity.getValorServicos() );
        nota.setValorRecebido( entity.getValorRecebido() );
        nota.setValorDescontoCondicionado( entity.getValorDescontoCondicionado() );
        nota.setValorDescontoIncondicionado( entity.getValorDescontoIncondicionado() );
        nota.setTipoOperacaoIss( entity.getTipoOperacaoIss() );
        nota.setAliquotaIss( entity.getAliquotaIss() );
        nota.setIssRetido( entity.getIssRetido() );
        nota.setResponsavelRetencao( entity.getResponsavelRetencao() );
        nota.setCstPisCofins( entity.getCstPisCofins() );
        nota.setTipoRetencao( entity.getTipoRetencao() );
        nota.setValorBaseCalculo( entity.getValorBaseCalculo() );
        nota.setAliquotaPis( entity.getAliquotaPis() );
        nota.setValorPis( entity.getValorPis() );
        nota.setAliquotaCofins( entity.getAliquotaCofins() );
        nota.setValorCofins( entity.getValorCofins() );
        nota.setValorInss( entity.getValorInss() );
        nota.setValorIr( entity.getValorIr() );
        nota.setValorCsll( entity.getValorCsll() );
        nota.setDataHoraAprovacao( entity.getDataHoraAprovacao() );
        nota.setDataHoraDescarte( entity.getDataHoraDescarte() );
        nota.setEstado( entity.getEstado() );
        nota.setCodigoRetorno( entity.getCodigoRetorno() );
        nota.setMensagemRetorno( entity.getMensagemRetorno() );
        nota.setErrosRetorno( entity.getErrosRetorno() );
        nota.setChave( entity.getChave() );
        nota.setNumeroNfse( entity.getNumeroNfse() );
        nota.setRpsNumero( entity.getRpsNumero() );
        nota.setRpsSerie( entity.getRpsSerie() );
        nota.setCodigoVerificacao( entity.getCodigoVerificacao() );
        nota.setDataHoraEvento( entity.getDataHoraEvento() );
        nota.setLinkPdf( entity.getLinkPdf() );
        nota.setXml( entity.getXml() );
        nota.setPdf( entity.getPdf() );
        nota.setId( entity.getId() );
        nota.setAmbiente( entity.getAmbiente() );
        nota.setDataCriacao( entity.getDataCriacao() );
        nota.setDataAtualizacao( entity.getDataAtualizacao() );
        nota.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));
        nota.setPessoa(pessoaMapper.entityToApi(entity.getPessoa() ));
        nota.setServico(servicoMapper.entityToApi(entity.getServico() ));
        nota.setPreparadoPorUsuario(usuarioMapper.entityToApi(entity.getPreparadoPorUsuario() ));
        nota.setPreparadoPorCliente(clienteAcessoMapper.entityToApi(entity.getPreparadoPorCliente() ));
        nota.setAprovadoPor(usuarioMapper.entityToApi(entity.getAprovadoPor() ));
        nota.setDescartadoPor(usuarioMapper.entityToApi(entity.getDescartadoPor() ));

        this.setApiDefaultValues(nota);

        return nota;
    }

    public NotaEntity apiToEntity(Nota api) {
        if ( api == null ) {
            return null;
        }
        NotaEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Nota found for Id: " + api.getId()));
            }
        } else {
            NotaEntityBuilder notaBuilder = NotaEntity.builderOf();

            notaBuilder.version = api.getVersion();
            notaBuilder.numero = api.getNumero();
            notaBuilder.serie = api.getSerie();
            notaBuilder.dataEmissao = api.getDataEmissao();
            notaBuilder.dataCompetencia = api.getDataCompetencia();
            notaBuilder.informacoesComplementares = api.getInformacoesComplementares();
            notaBuilder.regimeApuracao = api.getRegimeApuracao();
            notaBuilder.regimeTributacao = api.getRegimeTributacao();
            notaBuilder.codigoServico = api.getCodigoServico();
            notaBuilder.codigoTributacaoMunicipio = api.getCodigoTributacaoMunicipio();
            notaBuilder.codigoNbs = api.getCodigoNbs();
            notaBuilder.discriminacao = api.getDiscriminacao();
            notaBuilder.descricao = api.getDescricao();
            notaBuilder.valorServicos = api.getValorServicos();
            notaBuilder.valorRecebido = api.getValorRecebido();
            notaBuilder.valorDescontoCondicionado = api.getValorDescontoCondicionado();
            notaBuilder.valorDescontoIncondicionado = api.getValorDescontoIncondicionado();
            notaBuilder.tipoOperacaoIss = api.getTipoOperacaoIss();
            notaBuilder.aliquotaIss = api.getAliquotaIss();
            notaBuilder.issRetido = api.getIssRetido();
            notaBuilder.responsavelRetencao = api.getResponsavelRetencao();
            notaBuilder.cstPisCofins = api.getCstPisCofins();
            notaBuilder.tipoRetencao = api.getTipoRetencao();
            notaBuilder.valorBaseCalculo = api.getValorBaseCalculo();
            notaBuilder.aliquotaPis = api.getAliquotaPis();
            notaBuilder.valorPis = api.getValorPis();
            notaBuilder.aliquotaCofins = api.getAliquotaCofins();
            notaBuilder.valorCofins = api.getValorCofins();
            notaBuilder.valorInss = api.getValorInss();
            notaBuilder.valorIr = api.getValorIr();
            notaBuilder.valorCsll = api.getValorCsll();
            notaBuilder.dataHoraAprovacao = api.getDataHoraAprovacao();
            notaBuilder.dataHoraDescarte = api.getDataHoraDescarte();
            notaBuilder.estado = api.getEstado();
            notaBuilder.codigoRetorno = api.getCodigoRetorno();
            notaBuilder.mensagemRetorno = api.getMensagemRetorno();
            notaBuilder.errosRetorno = api.getErrosRetorno();
            notaBuilder.chave = api.getChave();
            notaBuilder.numeroNfse = api.getNumeroNfse();
            notaBuilder.rpsNumero = api.getRpsNumero();
            notaBuilder.rpsSerie = api.getRpsSerie();
            notaBuilder.codigoVerificacao = api.getCodigoVerificacao();
            notaBuilder.dataHoraEvento = api.getDataHoraEvento();
            notaBuilder.linkPdf = api.getLinkPdf();
            notaBuilder.xml = api.getXml();
            notaBuilder.pdf = api.getPdf();
            notaBuilder.id = api.getId();
            notaBuilder.ambiente = api.getAmbiente();
            notaBuilder.dataCriacao = api.getDataCriacao();
            notaBuilder.dataAtualizacao = api.getDataAtualizacao();
            notaBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            notaBuilder.pessoa = pessoaMapper.apiToEntity(api.getPessoa());
            notaBuilder.servico = servicoMapper.apiToEntity(api.getServico());
            notaBuilder.preparadoPorUsuario = usuarioMapper.apiToEntity(api.getPreparadoPorUsuario());
            notaBuilder.preparadoPorCliente = clienteAcessoMapper.apiToEntity(api.getPreparadoPorCliente());
            notaBuilder.aprovadoPor = usuarioMapper.apiToEntity(api.getAprovadoPor());
            notaBuilder.descartadoPor = usuarioMapper.apiToEntity(api.getDescartadoPor());
            entity = notaBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<Nota> entityListToApiList(List<NotaEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Nota> list = new ArrayList<Nota>( entity.size() );
        for ( NotaEntity notaEntity : entity ) {
            list.add( entityToApi( notaEntity ) );
        }

        return list;
    }

    public List<NotaEntity> apiListToEntityList(List<Nota> api) {
        if ( api == null ) {
            return null;
        }

        List<NotaEntity> list = new ArrayList<NotaEntity>( api.size() );
        for ( Nota nota : api ) {
            list.add( apiToEntity( nota ) );
        }

        return list;
    }

    public Nota updateEntityToApi(NotaEntity entity, Nota api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setNumero( entity.getNumero() );
        api.setSerie( entity.getSerie() );
        api.setDataEmissao( entity.getDataEmissao() );
        api.setDataCompetencia( entity.getDataCompetencia() );
        api.setInformacoesComplementares( entity.getInformacoesComplementares() );
        api.setRegimeApuracao( entity.getRegimeApuracao() );
        api.setRegimeTributacao( entity.getRegimeTributacao() );
        api.setCodigoServico( entity.getCodigoServico() );
        api.setCodigoTributacaoMunicipio( entity.getCodigoTributacaoMunicipio() );
        api.setCodigoNbs( entity.getCodigoNbs() );
        api.setDiscriminacao( entity.getDiscriminacao() );
        api.setDescricao( entity.getDescricao() );
        api.setValorServicos( entity.getValorServicos() );
        api.setValorRecebido( entity.getValorRecebido() );
        api.setValorDescontoCondicionado( entity.getValorDescontoCondicionado() );
        api.setValorDescontoIncondicionado( entity.getValorDescontoIncondicionado() );
        api.setTipoOperacaoIss( entity.getTipoOperacaoIss() );
        api.setAliquotaIss( entity.getAliquotaIss() );
        api.setIssRetido( entity.getIssRetido() );
        api.setResponsavelRetencao( entity.getResponsavelRetencao() );
        api.setCstPisCofins( entity.getCstPisCofins() );
        api.setTipoRetencao( entity.getTipoRetencao() );
        api.setValorBaseCalculo( entity.getValorBaseCalculo() );
        api.setAliquotaPis( entity.getAliquotaPis() );
        api.setValorPis( entity.getValorPis() );
        api.setAliquotaCofins( entity.getAliquotaCofins() );
        api.setValorCofins( entity.getValorCofins() );
        api.setValorInss( entity.getValorInss() );
        api.setValorIr( entity.getValorIr() );
        api.setValorCsll( entity.getValorCsll() );
        api.setDataHoraAprovacao( entity.getDataHoraAprovacao() );
        api.setDataHoraDescarte( entity.getDataHoraDescarte() );
        api.setEstado( entity.getEstado() );
        api.setCodigoRetorno( entity.getCodigoRetorno() );
        api.setMensagemRetorno( entity.getMensagemRetorno() );
        api.setErrosRetorno( entity.getErrosRetorno() );
        api.setChave( entity.getChave() );
        api.setNumeroNfse( entity.getNumeroNfse() );
        api.setRpsNumero( entity.getRpsNumero() );
        api.setRpsSerie( entity.getRpsSerie() );
        api.setCodigoVerificacao( entity.getCodigoVerificacao() );
        api.setDataHoraEvento( entity.getDataHoraEvento() );
        api.setLinkPdf( entity.getLinkPdf() );
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

        if ( entity.getPessoa() != null ) {
            if ( api.getPessoa() == null ) {
                api.setPessoa( new Pessoa() );
            }
            pessoaMapper.updateEntityToApi(entity.getPessoa(), api.getPessoa());
        } else {
            api.setPessoa( null );
        }

        if ( entity.getServico() != null ) {
            if ( api.getServico() == null ) {
                api.setServico( new Servico() );
            }
            servicoMapper.updateEntityToApi(entity.getServico(), api.getServico());
        } else {
            api.setServico( null );
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

        this.setApiDefaultValues(api);

        return api;
    }

    public NotaEntity updateApiToEntity(Nota api, NotaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Nota found for Id: " + api.getId()));
            }
        } else {
            NotaEntityBuilder notaBuilder = NotaEntity.builderOf();

            notaBuilder.version = api.getVersion();
            entity.setNumero( api.getNumero() );
            entity.setSerie( api.getSerie() );
            entity.setDataEmissao( api.getDataEmissao() );
            entity.setDataCompetencia( api.getDataCompetencia() );
            entity.setInformacoesComplementares( api.getInformacoesComplementares() );
            entity.setRegimeApuracao( api.getRegimeApuracao() );
            entity.setRegimeTributacao( api.getRegimeTributacao() );
            entity.setCodigoServico( api.getCodigoServico() );
            entity.setCodigoTributacaoMunicipio( api.getCodigoTributacaoMunicipio() );
            entity.setCodigoNbs( api.getCodigoNbs() );
            entity.setDiscriminacao( api.getDiscriminacao() );
            entity.setDescricao( api.getDescricao() );
            entity.setValorServicos( api.getValorServicos() );
            entity.setValorRecebido( api.getValorRecebido() );
            entity.setValorDescontoCondicionado( api.getValorDescontoCondicionado() );
            entity.setValorDescontoIncondicionado( api.getValorDescontoIncondicionado() );
            entity.setTipoOperacaoIss( api.getTipoOperacaoIss() );
            entity.setAliquotaIss( api.getAliquotaIss() );
            entity.setIssRetido( api.getIssRetido() );
            entity.setResponsavelRetencao( api.getResponsavelRetencao() );
            entity.setCstPisCofins( api.getCstPisCofins() );
            entity.setTipoRetencao( api.getTipoRetencao() );
            entity.setValorBaseCalculo( api.getValorBaseCalculo() );
            entity.setAliquotaPis( api.getAliquotaPis() );
            entity.setValorPis( api.getValorPis() );
            entity.setAliquotaCofins( api.getAliquotaCofins() );
            entity.setValorCofins( api.getValorCofins() );
            entity.setValorInss( api.getValorInss() );
            entity.setValorIr( api.getValorIr() );
            entity.setValorCsll( api.getValorCsll() );
            entity.setDataHoraAprovacao( api.getDataHoraAprovacao() );
            entity.setDataHoraDescarte( api.getDataHoraDescarte() );
            entity.setEstado( api.getEstado() );
            entity.setCodigoRetorno( api.getCodigoRetorno() );
            entity.setMensagemRetorno( api.getMensagemRetorno() );
            entity.setErrosRetorno( api.getErrosRetorno() );
            entity.setChave( api.getChave() );
            entity.setNumeroNfse( api.getNumeroNfse() );
            entity.setRpsNumero( api.getRpsNumero() );
            entity.setRpsSerie( api.getRpsSerie() );
            entity.setCodigoVerificacao( api.getCodigoVerificacao() );
            entity.setDataHoraEvento( api.getDataHoraEvento() );
            entity.setLinkPdf( api.getLinkPdf() );
            entity.setXml( api.getXml() );
            entity.setPdf( api.getPdf() );
            entity.setId( api.getId() );
            entity.setAmbiente( api.getAmbiente() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            if ( api.getPessoa() != null ) {
                if ( entity.getPessoa() == null ) {
                    entity.setPessoa( new PessoaEntity() );
                }
                entity.setPessoa(pessoaMapper.updateApiToEntity( api.getPessoa(), entity.getPessoa()));
            } else {
                entity.setPessoa( null );
            }

            if ( api.getServico() != null ) {
                if ( entity.getServico() == null ) {
                    entity.setServico( new ServicoEntity() );
                }
                entity.setServico(servicoMapper.updateApiToEntity( api.getServico(), entity.getServico()));
            } else {
                entity.setServico( null );
            }

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

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public NotaEntity patchApiToEntity(Nota api, NotaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getNumero() != null) entity.setNumero( api.getNumero() ); 
        if (api.getSerie() != null) entity.setSerie( api.getSerie() ); 
        if (api.getDataEmissao() != null) entity.setDataEmissao( api.getDataEmissao() ); 
        if (api.getDataCompetencia() != null) entity.setDataCompetencia( api.getDataCompetencia() ); 
        if (api.getInformacoesComplementares() != null) entity.setInformacoesComplementares( api.getInformacoesComplementares() ); 
        if (api.getRegimeApuracao() != null) entity.setRegimeApuracao( api.getRegimeApuracao() ); 
        if (api.getRegimeTributacao() != null) entity.setRegimeTributacao( api.getRegimeTributacao() ); 
        if (api.getCodigoServico() != null) entity.setCodigoServico( api.getCodigoServico() ); 
        if (api.getCodigoTributacaoMunicipio() != null) entity.setCodigoTributacaoMunicipio( api.getCodigoTributacaoMunicipio() ); 
        if (api.getCodigoNbs() != null) entity.setCodigoNbs( api.getCodigoNbs() ); 
        if (api.getDiscriminacao() != null) entity.setDiscriminacao( api.getDiscriminacao() ); 
        if (api.getDescricao() != null) entity.setDescricao( api.getDescricao() ); 
        if (api.getValorServicos() != null) entity.setValorServicos( api.getValorServicos() ); 
        if (api.getValorRecebido() != null) entity.setValorRecebido( api.getValorRecebido() ); 
        if (api.getValorDescontoCondicionado() != null) entity.setValorDescontoCondicionado( api.getValorDescontoCondicionado() ); 
        if (api.getValorDescontoIncondicionado() != null) entity.setValorDescontoIncondicionado( api.getValorDescontoIncondicionado() ); 
        if (api.getTipoOperacaoIss() != null) entity.setTipoOperacaoIss( api.getTipoOperacaoIss() ); 
        if (api.getAliquotaIss() != null) entity.setAliquotaIss( api.getAliquotaIss() ); 
        if (api.getIssRetido() != null) entity.setIssRetido( api.getIssRetido() ); 
        if (api.getResponsavelRetencao() != null) entity.setResponsavelRetencao( api.getResponsavelRetencao() ); 
        if (api.getCstPisCofins() != null) entity.setCstPisCofins( api.getCstPisCofins() ); 
        if (api.getTipoRetencao() != null) entity.setTipoRetencao( api.getTipoRetencao() ); 
        if (api.getValorBaseCalculo() != null) entity.setValorBaseCalculo( api.getValorBaseCalculo() ); 
        if (api.getAliquotaPis() != null) entity.setAliquotaPis( api.getAliquotaPis() ); 
        if (api.getValorPis() != null) entity.setValorPis( api.getValorPis() ); 
        if (api.getAliquotaCofins() != null) entity.setAliquotaCofins( api.getAliquotaCofins() ); 
        if (api.getValorCofins() != null) entity.setValorCofins( api.getValorCofins() ); 
        if (api.getValorInss() != null) entity.setValorInss( api.getValorInss() ); 
        if (api.getValorIr() != null) entity.setValorIr( api.getValorIr() ); 
        if (api.getValorCsll() != null) entity.setValorCsll( api.getValorCsll() ); 
        if (api.getDataHoraAprovacao() != null) entity.setDataHoraAprovacao( api.getDataHoraAprovacao() ); 
        if (api.getDataHoraDescarte() != null) entity.setDataHoraDescarte( api.getDataHoraDescarte() ); 
        if (api.getEstado() != null) entity.setEstado( api.getEstado() ); 
        if (api.getCodigoRetorno() != null) entity.setCodigoRetorno( api.getCodigoRetorno() ); 
        if (api.getMensagemRetorno() != null) entity.setMensagemRetorno( api.getMensagemRetorno() ); 
        if (api.getErrosRetorno() != null) entity.setErrosRetorno( api.getErrosRetorno() ); 
        if (api.getChave() != null) entity.setChave( api.getChave() ); 
        if (api.getNumeroNfse() != null) entity.setNumeroNfse( api.getNumeroNfse() ); 
        if (api.getRpsNumero() != null) entity.setRpsNumero( api.getRpsNumero() ); 
        if (api.getRpsSerie() != null) entity.setRpsSerie( api.getRpsSerie() ); 
        if (api.getCodigoVerificacao() != null) entity.setCodigoVerificacao( api.getCodigoVerificacao() ); 
        if (api.getDataHoraEvento() != null) entity.setDataHoraEvento( api.getDataHoraEvento() ); 
        if (api.getLinkPdf() != null) entity.setLinkPdf( api.getLinkPdf() ); 
        if (api.getXml() != null) entity.setXml( api.getXml() ); 
        if (api.getPdf() != null) entity.setPdf( api.getPdf() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getAmbiente() != null) entity.setAmbiente( api.getAmbiente() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());
        if ( api.getPessoa() != null ) {
            if ( entity.getPessoa() == null ) {
                entity.setPessoa( new PessoaEntity() );
            }
            entity.setPessoa(pessoaMapper.updateApiToEntity( api.getPessoa(), entity.getPessoa()));
        }
        if ( api.getServico() != null ) {
            if ( entity.getServico() == null ) {
                entity.setServico( new ServicoEntity() );
            }
            entity.setServico(servicoMapper.updateApiToEntity( api.getServico(), entity.getServico()));
        }
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

        return entity;
    }

    public NotaEntity setEntityDefaultValues(NotaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getRegimeTributacao() == null) {
            entity.setRegimeTributacao(0);
        }
        if (entity.getTipoOperacaoIss() == null) {
            entity.setTipoOperacaoIss(1);
        }
        if (entity.getIssRetido() == null) {
            entity.setIssRetido(false);
        }
        if (entity.getTipoRetencao() == null) {
            entity.setTipoRetencao(2);
        }
        if (entity.getEstado() == null) {
            entity.setEstado("rascunho");
        }
        if (entity.getAmbiente() == null) {
            entity.setAmbiente("homologacao");
        }

        return entity;
    }

    public Nota setApiDefaultValues(Nota api) {
        if ( api == null ) {
            return null;
        }

        if (api.getRegimeTributacao() == null) {
            api.setRegimeTributacao(0);
        }
        if (api.getTipoOperacaoIss() == null) {
            api.setTipoOperacaoIss(1);
        }
        if (api.getIssRetido() == null) {
            api.setIssRetido(false);
        }
        if (api.getTipoRetencao() == null) {
            api.setTipoRetencao(2);
        }
        if (api.getEstado() == null) {
            api.setEstado("rascunho");
        }
        if (api.getAmbiente() == null) {
            api.setAmbiente("homologacao");
        }

        return api;
    }

}
