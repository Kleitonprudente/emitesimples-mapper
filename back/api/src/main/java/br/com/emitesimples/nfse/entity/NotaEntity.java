package br.com.emitesimples.nfse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.nfse.domain.Nota;
import br.com.emitesimples.nfse.builder.NotaEntityBuilder;
import jakarta.persistence.FetchType;
import java.util.List;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.math.BigDecimal;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.cadastros.entity.ServicoEntity;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.usuarios.entity.ClienteAcessoEntity;

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
 * Tributação: o grupo tributos_municipais SÓ vai no envio quando há dado de ISS 
* (ISS retido ligado, alíquota, base ou valor retido; iss_retido = false sozinho 
* não conta) E (regime de apuração diferente de 1 OU ISS retido); sem regime, vale 
* 1 (regra E0625). Quando vai, leva sempre aliquota_iss (obrigatória, nunca 
* deduzida) e valor_base_calculo_iss (em branco = valor dos serviços menos 
* desconto incondicionado, mínimo 0). Uma nota simples, sem dado de ISS, sai sem o 
* grupo tributos_municipais (a confirmar em homologação). O campo tipo_operacao 
* não é mais enviado (não existe no schema de 2026-11-24). O grupo 
* tributos_nacionais só vai com dado, e então exige o CST. Nenhum agente de IA 
* informa tributo: só a pessoa, pela tela. As validações de coerência estão nas 
* description dos atributos e são implementadas à mão.
 * Fora do modelo por ora, a modelar sob demanda: obra, evento, locação, exterior, 
* intermediário, deduções, contingência, emissão pelo tomador e o bloco ibs_cbs.

*/
@Entity
@Table(name = "NOTA")
public class NotaEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="NUMERO", length=9)
    private String numero;

    @Column(name="SERIE", length=5)
    private String serie;

    @Column(name="DATA_EMISSAO")
    private LocalDateTime dataEmissao;

    @Column(name="DATA_COMPETENCIA")
    private LocalDate dataCompetencia;

    @Column(name="INFORMACOES_COMPLEMENTARES", length=2000)
    private String informacoesComplementares;

    @Column(name="REGIME_APURACAO", precision = 1)
    private Integer regimeApuracao;

    @Column(name="REGIME_TRIBUTACAO", precision = 1)
    private Integer regimeTributacao;

    @Column(name="ID_CONDICAO_PAGAMENTO", length=36)
    private String idCondicaoPagamento;

    @Column(name="CODIGO_SERVICO", length=6)
    private String codigoServico;

    @Column(name="CODIGO_TRIBUTACAO_MUNICIPIO", length=3)
    private String codigoTributacaoMunicipio;

    @Column(name="CODIGO_NBS", length=9)
    private String codigoNbs;

    @Column(name="DISCRIMINACAO", length=2000)
    private String discriminacao;

    @Column(name="DESCRICAO", length=200)
    private String descricao;

    @Column(name="VALOR_SERVICOS", precision = 15, scale = 2)
    private BigDecimal valorServicos;

    @Column(name="VALOR_RECEBIDO", precision = 15, scale = 2)
    private BigDecimal valorRecebido;

    @Column(name="VALOR_DESCONTO_CONDICIONADO", precision = 15, scale = 2)
    private BigDecimal valorDescontoCondicionado;

    @Column(name="VALOR_DESCONTO_INCONDICIONADO", precision = 15, scale = 2)
    private BigDecimal valorDescontoIncondicionado;

    @Column(name="TIPO_OPERACAO_ISS", precision = 1)
    private Integer tipoOperacaoIss;

    @Column(name="ALIQUOTA_ISS", precision = 5, scale = 2)
    private BigDecimal aliquotaIss;

    @Column(name="VALOR_BASE_CALCULO_ISS", precision = 15, scale = 2)
    private BigDecimal valorBaseCalculoIss;

    @Column(name="VALOR_ISS_RETIDO", precision = 15, scale = 2)
    private BigDecimal valorIssRetido;

    @Column(name="ISS_RETIDO")
    private Boolean issRetido;

    @Column(name="RESPONSAVEL_RETENCAO", precision = 1)
    private Integer responsavelRetencao;

    @Column(name="CST_PIS_COFINS", length=2)
    private String cstPisCofins;

    @Column(name="TIPO_RETENCAO", precision = 1)
    private Integer tipoRetencao;

    @Column(name="VALOR_BASE_CALCULO", precision = 15, scale = 2)
    private BigDecimal valorBaseCalculo;

    @Column(name="ALIQUOTA_PIS", precision = 5, scale = 2)
    private BigDecimal aliquotaPis;

    @Column(name="VALOR_PIS", precision = 15, scale = 2)
    private BigDecimal valorPis;

    @Column(name="ALIQUOTA_COFINS", precision = 5, scale = 2)
    private BigDecimal aliquotaCofins;

    @Column(name="VALOR_COFINS", precision = 15, scale = 2)
    private BigDecimal valorCofins;

    @Column(name="VALOR_INSS", precision = 15, scale = 2)
    private BigDecimal valorInss;

    @Column(name="VALOR_IR", precision = 15, scale = 2)
    private BigDecimal valorIr;

    @Column(name="VALOR_CSLL", precision = 15, scale = 2)
    private BigDecimal valorCsll;

    @Column(name="DATA_HORA_APROVACAO")
    private LocalDateTime dataHoraAprovacao;

    @Column(name="DATA_HORA_DESCARTE")
    private LocalDateTime dataHoraDescarte;

    @Column(name="ESTADO", length=20)
    private String estado;

    @Column(name="CODIGO_RETORNO", precision = 4)
    private Integer codigoRetorno;

    @Column(name="MENSAGEM_RETORNO", length=500)
    private String mensagemRetorno;

    @Column(name="ERROS_RETORNO", length=4000)
    private String errosRetorno;

    @Column(name="CHAVE", length=50)
    private String chave;

    @Column(name="NUMERO_NFSE", length=20)
    private String numeroNfse;

    @Column(name="RPS_NUMERO", length=9)
    private String rpsNumero;

    @Column(name="RPS_SERIE", length=5)
    private String rpsSerie;

    @Column(name="CODIGO_VERIFICACAO", length=40)
    private String codigoVerificacao;

    @Column(name="DATA_HORA_EVENTO")
    private LocalDateTime dataHoraEvento;

    @Column(name="LINK_PDF", length=500)
    private String linkPdf;

    // SEM @Lob, e isso e deliberado: em PostgreSQL, @Lob byte[] mapeia para OID
    // (large object), nao para BYTEA. Large object tem ciclo de vida PROPRIO --
    // apagar a linha NAO apaga o objeto, quem apaga e lo_unlink --, entao toda
    // entidade que zere um campo binario vazaria: objeto orfao em pg_largeobject,
    // sem dono, sem referencia e sem ninguem para notar.
    // Comportamento confirmado por teste de integracao em Postgres real.
    // Em H2 o defeito e INVISIVEL (o schema de teste sai da propria anotacao).
    // Quem precisar de large object de verdade tem caso diferente e merece
    // declaracao explicita, nao default.
    // O `length` NAO e decorativo: sem ele, o H2 cria BINARY VARYING(255) e um
    // upload de 300 bytes ja estoura. Postgres ignora tamanho em BYTEA, entao a
    // anotacao so tem efeito onde o schema sai da anotacao -- que e o perfil de
    // teste. Sem isto, o defeito e invisivel em producao e constante no teste.
    // Tamanho: o parametro do mapa quando existe; sem ele, o padrao explicito e
    // length = 1048576 (1 MiB). O @Column sempre sai com length, como o comentario exige.
    @Column(name="XML", length = 2097152)
    private byte[] xml;

    // SEM @Lob, e isso e deliberado: em PostgreSQL, @Lob byte[] mapeia para OID
    // (large object), nao para BYTEA. Large object tem ciclo de vida PROPRIO --
    // apagar a linha NAO apaga o objeto, quem apaga e lo_unlink --, entao toda
    // entidade que zere um campo binario vazaria: objeto orfao em pg_largeobject,
    // sem dono, sem referencia e sem ninguem para notar.
    // Comportamento confirmado por teste de integracao em Postgres real.
    // Em H2 o defeito e INVISIVEL (o schema de teste sai da propria anotacao).
    // Quem precisar de large object de verdade tem caso diferente e merece
    // declaracao explicita, nao default.
    // O `length` NAO e decorativo: sem ele, o H2 cria BINARY VARYING(255) e um
    // upload de 300 bytes ja estoura. Postgres ignora tamanho em BYTEA, entao a
    // anotacao so tem efeito onde o schema sai da anotacao -- que e o perfil de
    // teste. Sem isto, o defeito e invisivel em producao e constante no teste.
    // Tamanho: o parametro do mapa quando existe; sem ele, o padrao explicito e
    // length = 1048576 (1 MiB). O @Column sempre sai com length, como o comentario exige.
    @Column(name="PDF", length = 2097152)
    private byte[] pdf;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="AMBIENTE", length=15)
    private String ambiente;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="EMPRESA_ID", nullable = false)
    private EmpresaEntity empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="PESSOA_ID", nullable = false)
    private PessoaEntity pessoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="SERVICO_ID", nullable = false)
    private ServicoEntity servico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="PREPARADO_POR_USUARIO_ID", nullable = true)
    private UsuarioEntity preparadoPorUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="PREPARADO_POR_CLIENTE_ID", nullable = true)
    private ClienteAcessoEntity preparadoPorCliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="APROVADO_POR_ID", nullable = true)
    private UsuarioEntity aprovadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="DESCARTADO_POR_ID", nullable = true)
    private UsuarioEntity descartadoPor;

    @OneToMany(mappedBy="nota", fetch = FetchType.LAZY)
    private List<CancelamentoEntity> cancelamentos;

    public NotaEntity() {
    }

    public NotaEntity(
        String numero,
        String serie,
        LocalDateTime dataEmissao,
        LocalDate dataCompetencia,
        String informacoesComplementares,
        Integer regimeApuracao,
        Integer regimeTributacao,
        String idCondicaoPagamento,
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
        BigDecimal valorBaseCalculoIss,
        BigDecimal valorIssRetido,
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
        EmpresaEntity empresa,
        PessoaEntity pessoa,
        ServicoEntity servico,
        UsuarioEntity preparadoPorUsuario,
        ClienteAcessoEntity preparadoPorCliente,
        UsuarioEntity aprovadoPor,
        UsuarioEntity descartadoPor,
        List<CancelamentoEntity> cancelamentos
    ) {
        this.numero = numero;
        this.serie = serie;
        this.dataEmissao = dataEmissao;
        this.dataCompetencia = dataCompetencia;
        this.informacoesComplementares = informacoesComplementares;
        this.regimeApuracao = regimeApuracao;
        this.regimeTributacao = regimeTributacao;
        this.idCondicaoPagamento = idCondicaoPagamento;
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
        this.valorBaseCalculoIss = valorBaseCalculoIss;
        this.valorIssRetido = valorIssRetido;
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
        this.cancelamentos = cancelamentos;
    }

    public NotaEntity(NotaEntityBuilder builder) {
        this.version = builder.version;
        this.numero = builder.numero;
        this.serie = builder.serie;
        this.dataEmissao = builder.dataEmissao;
        this.dataCompetencia = builder.dataCompetencia;
        this.informacoesComplementares = builder.informacoesComplementares;
        this.regimeApuracao = builder.regimeApuracao;
        this.regimeTributacao = builder.regimeTributacao;
        this.idCondicaoPagamento = builder.idCondicaoPagamento;
        this.codigoServico = builder.codigoServico;
        this.codigoTributacaoMunicipio = builder.codigoTributacaoMunicipio;
        this.codigoNbs = builder.codigoNbs;
        this.discriminacao = builder.discriminacao;
        this.descricao = builder.descricao;
        this.valorServicos = builder.valorServicos;
        this.valorRecebido = builder.valorRecebido;
        this.valorDescontoCondicionado = builder.valorDescontoCondicionado;
        this.valorDescontoIncondicionado = builder.valorDescontoIncondicionado;
        this.tipoOperacaoIss = builder.tipoOperacaoIss;
        this.aliquotaIss = builder.aliquotaIss;
        this.valorBaseCalculoIss = builder.valorBaseCalculoIss;
        this.valorIssRetido = builder.valorIssRetido;
        this.issRetido = builder.issRetido;
        this.responsavelRetencao = builder.responsavelRetencao;
        this.cstPisCofins = builder.cstPisCofins;
        this.tipoRetencao = builder.tipoRetencao;
        this.valorBaseCalculo = builder.valorBaseCalculo;
        this.aliquotaPis = builder.aliquotaPis;
        this.valorPis = builder.valorPis;
        this.aliquotaCofins = builder.aliquotaCofins;
        this.valorCofins = builder.valorCofins;
        this.valorInss = builder.valorInss;
        this.valorIr = builder.valorIr;
        this.valorCsll = builder.valorCsll;
        this.dataHoraAprovacao = builder.dataHoraAprovacao;
        this.dataHoraDescarte = builder.dataHoraDescarte;
        this.estado = builder.estado;
        this.codigoRetorno = builder.codigoRetorno;
        this.mensagemRetorno = builder.mensagemRetorno;
        this.errosRetorno = builder.errosRetorno;
        this.chave = builder.chave;
        this.numeroNfse = builder.numeroNfse;
        this.rpsNumero = builder.rpsNumero;
        this.rpsSerie = builder.rpsSerie;
        this.codigoVerificacao = builder.codigoVerificacao;
        this.dataHoraEvento = builder.dataHoraEvento;
        this.linkPdf = builder.linkPdf;
        this.xml = builder.xml;
        this.pdf = builder.pdf;
        this.id = builder.id;
        this.ambiente = builder.ambiente;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.empresa = builder.empresa;
        this.pessoa = builder.pessoa;
        this.servico = builder.servico;
        this.preparadoPorUsuario = builder.preparadoPorUsuario;
        this.preparadoPorCliente = builder.preparadoPorCliente;
        this.aprovadoPor = builder.aprovadoPor;
        this.descartadoPor = builder.descartadoPor;
        this.cancelamentos = builder.cancelamentos;
    }

    public static NotaEntityBuilder builderOf() {
        return new NotaEntityBuilder();
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

    public void setIdCondicaoPagamento(String idCondicaoPagamento) {
        this.idCondicaoPagamento = idCondicaoPagamento;
    }

    public String getIdCondicaoPagamento() {
        return this.idCondicaoPagamento;
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
    public void setValorBaseCalculoIss(BigDecimal valorBaseCalculoIss) {
        this.valorBaseCalculoIss = valorBaseCalculoIss;
    }
    public BigDecimal getValorBaseCalculoIss() {
        return this.valorBaseCalculoIss;
    }
    public void setValorIssRetido(BigDecimal valorIssRetido) {
        this.valorIssRetido = valorIssRetido;
    }
    public BigDecimal getValorIssRetido() {
        return this.valorIssRetido;
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

    public void setEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
    }

    public EmpresaEntity getEmpresa() {
        return this.empresa;
    }

    public void setPessoa(PessoaEntity pessoa) {
        this.pessoa = pessoa;
    }

    public PessoaEntity getPessoa() {
        return this.pessoa;
    }

    public void setServico(ServicoEntity servico) {
        this.servico = servico;
    }

    public ServicoEntity getServico() {
        return this.servico;
    }

    public void setPreparadoPorUsuario(UsuarioEntity preparadoPorUsuario) {
        this.preparadoPorUsuario = preparadoPorUsuario;
    }

    public UsuarioEntity getPreparadoPorUsuario() {
        return this.preparadoPorUsuario;
    }

    public void setPreparadoPorCliente(ClienteAcessoEntity preparadoPorCliente) {
        this.preparadoPorCliente = preparadoPorCliente;
    }

    public ClienteAcessoEntity getPreparadoPorCliente() {
        return this.preparadoPorCliente;
    }

    public void setAprovadoPor(UsuarioEntity aprovadoPor) {
        this.aprovadoPor = aprovadoPor;
    }

    public UsuarioEntity getAprovadoPor() {
        return this.aprovadoPor;
    }

    public void setDescartadoPor(UsuarioEntity descartadoPor) {
        this.descartadoPor = descartadoPor;
    }

    public UsuarioEntity getDescartadoPor() {
        return this.descartadoPor;
    }

    public void setCancelamentos(List<CancelamentoEntity> cancelamentos) {
        this.cancelamentos = cancelamentos;
    }

    public List<CancelamentoEntity> getCancelamentos() {
        return this.cancelamentos;
    }

}
