package br.com.emitesimples.nfse.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.cadastros.domain.Pessoa;
import br.com.emitesimples.cadastros.domain.Servico;
import br.com.emitesimples.usuarios.domain.Usuario;
import br.com.emitesimples.usuarios.domain.ClienteAcesso;

/*
* NFS-e da empresa e o seu ciclo de vida. Há dois caminhos de entrada.
 * (A) Emissão DIRETA por uma pessoa no sistema: NÃO há rascunho; a nota nasce em 
* processamento, criada e enviada ao gateway numa só ação. (B) Assistente de IA: o 
* assistente só PREPARA a nota como rascunho (preparadoPorCliente) e uma PESSOA 
* aprova (o sistema envia e a nota vira PROCESSANDO) ou descarta (vira 
* DESCARTADA). Aprovar e descartar o rascunho são requisito AINDA NÃO IMPLEMENTADO 
* no código. O estado rascunho só existe no caminho B.
 * Depois do envio, o gateway devolve o código 5023 com a chave e só depois vem a 
* autorização. Falha de infraestrutura no envio direto (gateway indisponível, 
* token recusado, aceite sem chave, código 0) leva a FALHA_ENVIO, que pode ser 
* reenviada com o mesmo número, como a REJEITADA. As transições estão em 
* CicloNota.
 * Vive no contexto nfse (domain.nfse). Depende de configuracao (Empresa), de 
* cadastros (Pessoa e Servico) e de usuarios (Usuario e ClienteAcesso) para o 
* rastro de quem preparou, aprovou e descartou. Usuarios depende só de cadastros e 
* configuracao, então não há ciclo.
 * Guarda uma CÓPIA dos dados do SERVIÇO enviados (código, discriminação, valores, 
* tributos), porque alterar o cadastro de Servico depois não pode mudar uma nota 
* já emitida.
 * Da PESSOA guarda só o vínculo: o sistema monta o envio lendo a Pessoa no momento 
* do envio (aprovação). O registro do que de fato foi enviado e autorizado é o XML 
* (e o PDF) que a nota guarda; consultas por nome ou documento da pessoa usam a 
* relação. Custo: se a Pessoa for alterada depois, a nota passa a mostrar o dado 
* novo nas telas e nos relatórios, mas o XML e o PDF guardados preservam o que foi 
* emitido.
 * Chave única: a nota só tem a chave de 50 caracteres devolvida pelo gateway, 
* única por NFS-e (índice no banco). O campo identificacao do envio (1 a 40 
* caracteres) é preenchido com o id da nota (UUID de 36 caracteres, cabe nos 40 do 
* gateway, que não aceita mais que isso), então a nota não guarda atributo de 
* correlação à parte.
 * A numeração da DPS (numero e serie) é atributo da nota, não existe entidade RPS. 
* O prestador não vai no envio, vem do token da empresa (emitente no 
* IntegraNotas).
 * Regra de unicidade para a camada de banco: a combinação (empresa, ambiente, 
* serie, numero) deve ser única. O rascunho do assistente tem numero e serie 
* vazios e não conflita: no PostgreSQL valores nulos não colidem em índice único.
 * O mapa não expressa unicidade composta, ela precisa virar constraint no banco.
 * Fora do modelo por ora, a modelar sob demanda: obra, evento, locação, exterior, 
* intermediário, deduções, contingência, emissão pelo tomador e o bloco ibs_cbs.

*/
public class Nota {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @Size(max = 9, message = "Attribute numero must have 9 characters maximum.")
    private String numero;

    @Size(max = 5, message = "Attribute serie must have 5 characters maximum.")
    private String serie;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataEmissao;

    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate dataCompetencia;

    @Size(max = 2000, message = "Attribute informacoesComplementares must have 2000 characters maximum.")
    private String informacoesComplementares;

    private Integer regimeApuracao;

    private Integer regimeTributacao;

    @NotEmpty(message = "The attribute codigoServico can't be empty.")
    @Size(max = 6, message = "Attribute codigoServico must have 6 characters maximum.")
    private String codigoServico;

    @Size(max = 3, message = "Attribute codigoTributacaoMunicipio must have 3 characters maximum.")
    private String codigoTributacaoMunicipio;

    @Size(max = 9, message = "Attribute codigoNbs must have 9 characters maximum.")
    private String codigoNbs;

    @NotEmpty(message = "The attribute discriminacao can't be empty.")
    @Size(max = 2000, message = "Attribute discriminacao must have 2000 characters maximum.")
    private String discriminacao;

    @Size(max = 200, message = "Attribute descricao must have 200 characters maximum.")
    private String descricao;

    @NotNull(message = "The attribute valorServicos can't be null.")
    private BigDecimal valorServicos;

    private BigDecimal valorRecebido;

    private BigDecimal valorDescontoCondicionado;

    private BigDecimal valorDescontoIncondicionado;

    @NotNull(message = "The attribute tipoOperacaoIss can't be null.")
    private Integer tipoOperacaoIss;

    private BigDecimal aliquotaIss;

    @NotNull(message = "The attribute issRetido can't be null.")
    private Boolean issRetido;

    private Integer responsavelRetencao;

    @NotEmpty(message = "The attribute cstPisCofins can't be empty.")
    @Size(max = 2, message = "Attribute cstPisCofins must have 2 characters maximum.")
    private String cstPisCofins;

    private Integer tipoRetencao;

    private BigDecimal valorBaseCalculo;

    private BigDecimal aliquotaPis;

    private BigDecimal valorPis;

    private BigDecimal aliquotaCofins;

    private BigDecimal valorCofins;

    private BigDecimal valorInss;

    private BigDecimal valorIr;

    private BigDecimal valorCsll;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataHoraAprovacao;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataHoraDescarte;

    @NotEmpty(message = "The attribute estado can't be empty.")
    @Size(max = 20, message = "Attribute estado must have 20 characters maximum.")
    private String estado;

    private Integer codigoRetorno;

    @Size(max = 500, message = "Attribute mensagemRetorno must have 500 characters maximum.")
    private String mensagemRetorno;

    @Size(max = 4000, message = "Attribute errosRetorno must have 4000 characters maximum.")
    private String errosRetorno;

    @Size(max = 50, message = "Attribute chave must have 50 characters maximum.")
    private String chave;

    @Size(max = 20, message = "Attribute numeroNfse must have 20 characters maximum.")
    private String numeroNfse;

    @Size(max = 9, message = "Attribute rpsNumero must have 9 characters maximum.")
    private String rpsNumero;

    @Size(max = 5, message = "Attribute rpsSerie must have 5 characters maximum.")
    private String rpsSerie;

    @Size(max = 40, message = "Attribute codigoVerificacao must have 40 characters maximum.")
    private String codigoVerificacao;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataHoraEvento;

    @Size(max = 500, message = "Attribute linkPdf must have 500 characters maximum.")
    private String linkPdf;

    private byte[] xml;

    private byte[] pdf;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotEmpty(message = "The attribute ambiente can't be empty.")
    @Size(max = 15, message = "Attribute ambiente must have 15 characters maximum.")
    private String ambiente;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    @NotNull(message = "The attribute empresa can't be null.")
    private Empresa empresa;

    @NotNull(message = "The attribute pessoa can't be null.")
    private Pessoa pessoa;

    @NotNull(message = "The attribute servico can't be null.")
    private Servico servico;

    private Usuario preparadoPorUsuario;

    private ClienteAcesso preparadoPorCliente;

    private Usuario aprovadoPor;

    private Usuario descartadoPor;

    public Nota() {
        this.version = null;
        this.numero = null;
        this.serie = null;
        this.dataEmissao = null;
        this.dataCompetencia = null;
        this.informacoesComplementares = null;
        this.regimeApuracao = null;
        this.regimeTributacao = null;
        this.codigoServico = null;
        this.codigoTributacaoMunicipio = null;
        this.codigoNbs = null;
        this.discriminacao = null;
        this.descricao = null;
        this.valorServicos = null;
        this.valorRecebido = null;
        this.valorDescontoCondicionado = null;
        this.valorDescontoIncondicionado = null;
        this.tipoOperacaoIss = null;
        this.aliquotaIss = null;
        this.issRetido = null;
        this.responsavelRetencao = null;
        this.cstPisCofins = null;
        this.tipoRetencao = null;
        this.valorBaseCalculo = null;
        this.aliquotaPis = null;
        this.valorPis = null;
        this.aliquotaCofins = null;
        this.valorCofins = null;
        this.valorInss = null;
        this.valorIr = null;
        this.valorCsll = null;
        this.dataHoraAprovacao = null;
        this.dataHoraDescarte = null;
        this.estado = null;
        this.codigoRetorno = null;
        this.mensagemRetorno = null;
        this.errosRetorno = null;
        this.chave = null;
        this.numeroNfse = null;
        this.rpsNumero = null;
        this.rpsSerie = null;
        this.codigoVerificacao = null;
        this.dataHoraEvento = null;
        this.linkPdf = null;
        this.xml = null;
        this.pdf = null;
        this.id = null;
        this.ambiente = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.empresa = null;
        this.pessoa = null;
        this.servico = null;
        this.preparadoPorUsuario = null;
        this.preparadoPorCliente = null;
        this.aprovadoPor = null;
        this.descartadoPor = null;
    }
    public Nota(
        LocalDateTime version,
        String numero,
        String serie,
        LocalDateTime dataEmissao,
        LocalDate dataCompetencia,
        String informacoesComplementares,
        Integer regimeApuracao,
        Integer regimeTributacao,
        String codigoServico,
        String codigoTributacaoMunicipio,
        String codigoNbs,
        String discriminacao,
        String descricao,
        BigDecimal valorServicos,
        BigDecimal valorRecebido,
        BigDecimal valorDescontoCondicionado,
        BigDecimal valorDescontoIncondicionado,
        Integer tipoOperacaoIss,
        BigDecimal aliquotaIss,
        Boolean issRetido,
        Integer responsavelRetencao,
        String cstPisCofins,
        Integer tipoRetencao,
        BigDecimal valorBaseCalculo,
        BigDecimal aliquotaPis,
        BigDecimal valorPis,
        BigDecimal aliquotaCofins,
        BigDecimal valorCofins,
        BigDecimal valorInss,
        BigDecimal valorIr,
        BigDecimal valorCsll,
        LocalDateTime dataHoraAprovacao,
        LocalDateTime dataHoraDescarte,
        String estado,
        Integer codigoRetorno,
        String mensagemRetorno,
        String errosRetorno,
        String chave,
        String numeroNfse,
        String rpsNumero,
        String rpsSerie,
        String codigoVerificacao,
        LocalDateTime dataHoraEvento,
        String linkPdf,
        byte[] xml,
        byte[] pdf,
        String id,
        String ambiente,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Empresa empresa,
        Pessoa pessoa,
        Servico servico,
        Usuario preparadoPorUsuario,
        ClienteAcesso preparadoPorCliente,
        Usuario aprovadoPor,
        Usuario descartadoPor
    ) {
        this.version = version;
        this.numero = numero;
        this.serie = serie;
        this.dataEmissao = dataEmissao;
        this.dataCompetencia = dataCompetencia;
        this.informacoesComplementares = informacoesComplementares;
        this.regimeApuracao = regimeApuracao;
        this.regimeTributacao = regimeTributacao;
        this.codigoServico = codigoServico;
        this.codigoTributacaoMunicipio = codigoTributacaoMunicipio;
        this.codigoNbs = codigoNbs;
        this.discriminacao = discriminacao;
        this.descricao = descricao;
        this.valorServicos = valorServicos;
        this.valorRecebido = valorRecebido;
        this.valorDescontoCondicionado = valorDescontoCondicionado;
        this.valorDescontoIncondicionado = valorDescontoIncondicionado;
        this.tipoOperacaoIss = tipoOperacaoIss;
        this.aliquotaIss = aliquotaIss;
        this.issRetido = issRetido;
        this.responsavelRetencao = responsavelRetencao;
        this.cstPisCofins = cstPisCofins;
        this.tipoRetencao = tipoRetencao;
        this.valorBaseCalculo = valorBaseCalculo;
        this.aliquotaPis = aliquotaPis;
        this.valorPis = valorPis;
        this.aliquotaCofins = aliquotaCofins;
        this.valorCofins = valorCofins;
        this.valorInss = valorInss;
        this.valorIr = valorIr;
        this.valorCsll = valorCsll;
        this.dataHoraAprovacao = dataHoraAprovacao;
        this.dataHoraDescarte = dataHoraDescarte;
        this.estado = estado;
        this.codigoRetorno = codigoRetorno;
        this.mensagemRetorno = mensagemRetorno;
        this.errosRetorno = errosRetorno;
        this.chave = chave;
        this.numeroNfse = numeroNfse;
        this.rpsNumero = rpsNumero;
        this.rpsSerie = rpsSerie;
        this.codigoVerificacao = codigoVerificacao;
        this.dataHoraEvento = dataHoraEvento;
        this.linkPdf = linkPdf;
        this.xml = xml;
        this.pdf = pdf;
        this.id = id;
        this.ambiente = ambiente;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
        this.pessoa = pessoa;
        this.servico = servico;
        this.preparadoPorUsuario = preparadoPorUsuario;
        this.preparadoPorCliente = preparadoPorCliente;
        this.aprovadoPor = aprovadoPor;
        this.descartadoPor = descartadoPor;
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getNumero() {
        return this.numero;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getSerie() {
        return this.serie;
    }

    public void setDataEmissao(LocalDateTime dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public LocalDateTime getDataEmissao() {
        return this.dataEmissao;
    }

    public void setDataCompetencia(LocalDate dataCompetencia) {
        this.dataCompetencia = dataCompetencia;
    }

    public LocalDate getDataCompetencia() {
        return this.dataCompetencia;
    }

    public void setInformacoesComplementares(String informacoesComplementares) {
        this.informacoesComplementares = informacoesComplementares;
    }

    public String getInformacoesComplementares() {
        return this.informacoesComplementares;
    }

    public void setRegimeApuracao(Integer regimeApuracao) {
        this.regimeApuracao = regimeApuracao;
    }

    public Integer getRegimeApuracao() {
        return this.regimeApuracao;
    }

    public void setRegimeTributacao(Integer regimeTributacao) {
        this.regimeTributacao = regimeTributacao;
    }

    public Integer getRegimeTributacao() {
        return this.regimeTributacao;
    }

    public void setCodigoServico(String codigoServico) {
        this.codigoServico = codigoServico;
    }

    public String getCodigoServico() {
        return this.codigoServico;
    }

    public void setCodigoTributacaoMunicipio(String codigoTributacaoMunicipio) {
        this.codigoTributacaoMunicipio = codigoTributacaoMunicipio;
    }

    public String getCodigoTributacaoMunicipio() {
        return this.codigoTributacaoMunicipio;
    }

    public void setCodigoNbs(String codigoNbs) {
        this.codigoNbs = codigoNbs;
    }

    public String getCodigoNbs() {
        return this.codigoNbs;
    }

    public void setDiscriminacao(String discriminacao) {
        this.discriminacao = discriminacao;
    }

    public String getDiscriminacao() {
        return this.discriminacao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return this.descricao;
    }
    public void setValorServicos(BigDecimal valorServicos) {
        this.valorServicos = valorServicos;
    }
    public BigDecimal getValorServicos() {
        return this.valorServicos;
    }
    public void setValorRecebido(BigDecimal valorRecebido) {
        this.valorRecebido = valorRecebido;
    }
    public BigDecimal getValorRecebido() {
        return this.valorRecebido;
    }
    public void setValorDescontoCondicionado(BigDecimal valorDescontoCondicionado) {
        this.valorDescontoCondicionado = valorDescontoCondicionado;
    }
    public BigDecimal getValorDescontoCondicionado() {
        return this.valorDescontoCondicionado;
    }
    public void setValorDescontoIncondicionado(BigDecimal valorDescontoIncondicionado) {
        this.valorDescontoIncondicionado = valorDescontoIncondicionado;
    }
    public BigDecimal getValorDescontoIncondicionado() {
        return this.valorDescontoIncondicionado;
    }

    public void setTipoOperacaoIss(Integer tipoOperacaoIss) {
        this.tipoOperacaoIss = tipoOperacaoIss;
    }

    public Integer getTipoOperacaoIss() {
        return this.tipoOperacaoIss;
    }
    public void setAliquotaIss(BigDecimal aliquotaIss) {
        this.aliquotaIss = aliquotaIss;
    }
    public BigDecimal getAliquotaIss() {
        return this.aliquotaIss;
    }

    public void setIssRetido(Boolean issRetido) {
        this.issRetido = issRetido;
    }

    public Boolean getIssRetido() {
        return this.issRetido;
    }

    public void setResponsavelRetencao(Integer responsavelRetencao) {
        this.responsavelRetencao = responsavelRetencao;
    }

    public Integer getResponsavelRetencao() {
        return this.responsavelRetencao;
    }

    public void setCstPisCofins(String cstPisCofins) {
        this.cstPisCofins = cstPisCofins;
    }

    public String getCstPisCofins() {
        return this.cstPisCofins;
    }

    public void setTipoRetencao(Integer tipoRetencao) {
        this.tipoRetencao = tipoRetencao;
    }

    public Integer getTipoRetencao() {
        return this.tipoRetencao;
    }
    public void setValorBaseCalculo(BigDecimal valorBaseCalculo) {
        this.valorBaseCalculo = valorBaseCalculo;
    }
    public BigDecimal getValorBaseCalculo() {
        return this.valorBaseCalculo;
    }
    public void setAliquotaPis(BigDecimal aliquotaPis) {
        this.aliquotaPis = aliquotaPis;
    }
    public BigDecimal getAliquotaPis() {
        return this.aliquotaPis;
    }
    public void setValorPis(BigDecimal valorPis) {
        this.valorPis = valorPis;
    }
    public BigDecimal getValorPis() {
        return this.valorPis;
    }
    public void setAliquotaCofins(BigDecimal aliquotaCofins) {
        this.aliquotaCofins = aliquotaCofins;
    }
    public BigDecimal getAliquotaCofins() {
        return this.aliquotaCofins;
    }
    public void setValorCofins(BigDecimal valorCofins) {
        this.valorCofins = valorCofins;
    }
    public BigDecimal getValorCofins() {
        return this.valorCofins;
    }
    public void setValorInss(BigDecimal valorInss) {
        this.valorInss = valorInss;
    }
    public BigDecimal getValorInss() {
        return this.valorInss;
    }
    public void setValorIr(BigDecimal valorIr) {
        this.valorIr = valorIr;
    }
    public BigDecimal getValorIr() {
        return this.valorIr;
    }
    public void setValorCsll(BigDecimal valorCsll) {
        this.valorCsll = valorCsll;
    }
    public BigDecimal getValorCsll() {
        return this.valorCsll;
    }

    public void setDataHoraAprovacao(LocalDateTime dataHoraAprovacao) {
        this.dataHoraAprovacao = dataHoraAprovacao;
    }

    public LocalDateTime getDataHoraAprovacao() {
        return this.dataHoraAprovacao;
    }

    public void setDataHoraDescarte(LocalDateTime dataHoraDescarte) {
        this.dataHoraDescarte = dataHoraDescarte;
    }

    public LocalDateTime getDataHoraDescarte() {
        return this.dataHoraDescarte;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return this.estado;
    }

    public void setCodigoRetorno(Integer codigoRetorno) {
        this.codigoRetorno = codigoRetorno;
    }

    public Integer getCodigoRetorno() {
        return this.codigoRetorno;
    }

    public void setMensagemRetorno(String mensagemRetorno) {
        this.mensagemRetorno = mensagemRetorno;
    }

    public String getMensagemRetorno() {
        return this.mensagemRetorno;
    }

    public void setErrosRetorno(String errosRetorno) {
        this.errosRetorno = errosRetorno;
    }

    public String getErrosRetorno() {
        return this.errosRetorno;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public String getChave() {
        return this.chave;
    }

    public void setNumeroNfse(String numeroNfse) {
        this.numeroNfse = numeroNfse;
    }

    public String getNumeroNfse() {
        return this.numeroNfse;
    }

    public void setRpsNumero(String rpsNumero) {
        this.rpsNumero = rpsNumero;
    }

    public String getRpsNumero() {
        return this.rpsNumero;
    }

    public void setRpsSerie(String rpsSerie) {
        this.rpsSerie = rpsSerie;
    }

    public String getRpsSerie() {
        return this.rpsSerie;
    }

    public void setCodigoVerificacao(String codigoVerificacao) {
        this.codigoVerificacao = codigoVerificacao;
    }

    public String getCodigoVerificacao() {
        return this.codigoVerificacao;
    }

    public void setDataHoraEvento(LocalDateTime dataHoraEvento) {
        this.dataHoraEvento = dataHoraEvento;
    }

    public LocalDateTime getDataHoraEvento() {
        return this.dataHoraEvento;
    }

    public void setLinkPdf(String linkPdf) {
        this.linkPdf = linkPdf;
    }

    public String getLinkPdf() {
        return this.linkPdf;
    }

    public void setXml(byte[] xml) {
        this.xml = xml;
    }

    public byte[] getXml() {
        return this.xml;
    }

    public void setPdf(byte[] pdf) {
        this.pdf = pdf;
    }

    public byte[] getPdf() {
        return this.pdf;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    public String getAmbiente() {
        return this.ambiente;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataCriacao() {
        return this.dataCriacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return this.dataAtualizacao;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Empresa getEmpresa() {
        return this.empresa;
    }

    public void setPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }

    public Pessoa getPessoa() {
        return this.pessoa;
    }

    public void setServico(Servico servico) {
        this.servico = servico;
    }

    public Servico getServico() {
        return this.servico;
    }

    public void setPreparadoPorUsuario(Usuario preparadoPorUsuario) {
        this.preparadoPorUsuario = preparadoPorUsuario;
    }

    public Usuario getPreparadoPorUsuario() {
        return this.preparadoPorUsuario;
    }

    public void setPreparadoPorCliente(ClienteAcesso preparadoPorCliente) {
        this.preparadoPorCliente = preparadoPorCliente;
    }

    public ClienteAcesso getPreparadoPorCliente() {
        return this.preparadoPorCliente;
    }

    public void setAprovadoPor(Usuario aprovadoPor) {
        this.aprovadoPor = aprovadoPor;
    }

    public Usuario getAprovadoPor() {
        return this.aprovadoPor;
    }

    public void setDescartadoPor(Usuario descartadoPor) {
        this.descartadoPor = descartadoPor;
    }

    public Usuario getDescartadoPor() {
        return this.descartadoPor;
    }

    @JsonIgnore
    public boolean isWeakReference() {
      return !this.isAnyNotKeyFieldsDefined()
              && (
                      this.isIdDefined()
              );
    }
    @JsonIgnore
    public boolean isIdDefined() {
        return this.id != null;
    }

    @JsonIgnore
    public boolean isAnyNotKeyFieldsDefined() {
        return 
            this.numero != null || 
            this.serie != null || 
            this.dataEmissao != null || 
            this.dataCompetencia != null || 
            this.informacoesComplementares != null || 
            this.regimeApuracao != null || 
            this.regimeTributacao != null || 
            this.codigoServico != null || 
            this.codigoTributacaoMunicipio != null || 
            this.codigoNbs != null || 
            this.discriminacao != null || 
            this.descricao != null || 
            this.valorServicos != null || 
            this.valorRecebido != null || 
            this.valorDescontoCondicionado != null || 
            this.valorDescontoIncondicionado != null || 
            this.tipoOperacaoIss != null || 
            this.aliquotaIss != null || 
            this.issRetido != null || 
            this.responsavelRetencao != null || 
            this.cstPisCofins != null || 
            this.tipoRetencao != null || 
            this.valorBaseCalculo != null || 
            this.aliquotaPis != null || 
            this.valorPis != null || 
            this.aliquotaCofins != null || 
            this.valorCofins != null || 
            this.valorInss != null || 
            this.valorIr != null || 
            this.valorCsll != null || 
            this.dataHoraAprovacao != null || 
            this.dataHoraDescarte != null || 
            this.estado != null || 
            this.codigoRetorno != null || 
            this.mensagemRetorno != null || 
            this.errosRetorno != null || 
            this.chave != null || 
            this.numeroNfse != null || 
            this.rpsNumero != null || 
            this.rpsSerie != null || 
            this.codigoVerificacao != null || 
            this.dataHoraEvento != null || 
            this.linkPdf != null || 
            this.xml != null || 
            this.pdf != null || 
            this.ambiente != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.empresa != null || 
            this.pessoa != null || 
            this.servico != null || 
            this.preparadoPorUsuario != null || 
            this.preparadoPorCliente != null || 
            this.aprovadoPor != null || 
            this.descartadoPor != null;
    }

}
