package br.com.emitesimples.nfse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.nfse.domain.Cancelamento;
import br.com.emitesimples.nfse.builder.CancelamentoEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.usuarios.entity.ClienteAcessoEntity;

/*
* Pedido de cancelamento de uma Nota, com o evento e o retorno do gateway (XML e 
* PDF). Só se cancela nota autorizada,
 * caso contrário o gateway devolve 5013. Nota já cancelada devolve 5015, tratado 
* como sucesso idempotente.
 * Existe só dentro de uma nota e no mesmo contexto, por isso é detalhe da Nota 
* ([m]/[o]).
 * REGRA DO PRODUTO, mês vigente: só se cancela dentro do mês e ano da data de 
* emissão da nota (fuso America/Sao_Paulo). Prazos específicos de cada município 
* não são tratados.
 * Dois caminhos de entrada. (A) Cancelamento DIRETO por uma pessoa: o pedido nasce 
* em processando, criado e enviado ao gateway numa só ação. (B) Assistente de IA: 
* só PREPARA o pedido como rascunho (preparadoPorCliente); uma pessoa com 
* permissão de cancelar aprova (o sistema envia e o pedido vira processando) ou 
* descarta (vira descartado). Aprovar e descartar o rascunho são requisito AINDA 
* NÃO IMPLEMENTADO no código. O estado rascunho só existe no caminho B.
 * Quem cancela: o administrador sempre; o operador SÓ com permissão delegada 
* (VinculoUsuario.podeCancelarNfse). O código atual deixa o operador cancelar sem 
* delegação: divergência a corrigir no código.
 * Estados do pedido: rascunho, processando, concluido, recusado (o gateway ou o 
* sistema recusou), expirado (pendente por mais de 24 h sem confirmação) e 
* descartado. As transições estão em CicloCancelamento.
 * Vive no contexto nfse (domain.nfse). Depende do contexto usuarios (Usuario e 
* ClienteAcesso) para o rastro; usuarios depende só de cadastros e configuracao, 
* não há ciclo.

*/
@Entity
@Table(name = "CANCELAMENTO")
public class CancelamentoEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="CODIGO_MOTIVO", precision = 1)
    private Integer codigoMotivo;

    @Column(name="MOTIVO", length=255)
    private String motivo;

    @Column(name="DATA_HORA_APROVACAO")
    private LocalDateTime dataHoraAprovacao;

    @Column(name="DATA_HORA_DESCARTE")
    private LocalDateTime dataHoraDescarte;

    @Column(name="DATA_HORA_RECUSA")
    private LocalDateTime dataHoraRecusa;

    @Column(name="ESTADO", length=25)
    private String estado;

    @Column(name="CODIGO_RETORNO", precision = 4)
    private Integer codigoRetorno;

    @Column(name="MENSAGEM_RETORNO", length=500)
    private String mensagemRetorno;

    @Column(name="DATA_HORA_EVENTO")
    private LocalDateTime dataHoraEvento;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="NOTA_ID", nullable = false)
    private NotaEntity nota;

    public CancelamentoEntity() {
    }

    public CancelamentoEntity(
        Integer codigoMotivo,
        String motivo,
        LocalDateTime dataHoraAprovacao,
        LocalDateTime dataHoraDescarte,
        LocalDateTime dataHoraRecusa,
        String estado,
        Integer codigoRetorno,
        String mensagemRetorno,
        LocalDateTime dataHoraEvento,
        byte[] xml,
        byte[] pdf,
        String id,
        String ambiente,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        EmpresaEntity empresa,
        UsuarioEntity preparadoPorUsuario,
        ClienteAcessoEntity preparadoPorCliente,
        UsuarioEntity aprovadoPor,
        UsuarioEntity descartadoPor,
        NotaEntity nota
    ) {
        this.codigoMotivo = codigoMotivo;
        this.motivo = motivo;
        this.dataHoraAprovacao = dataHoraAprovacao;
        this.dataHoraDescarte = dataHoraDescarte;
        this.dataHoraRecusa = dataHoraRecusa;
        this.estado = estado;
        this.codigoRetorno = codigoRetorno;
        this.mensagemRetorno = mensagemRetorno;
        this.dataHoraEvento = dataHoraEvento;
        this.xml = xml;
        this.pdf = pdf;
        this.id = id;
        this.ambiente = ambiente;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
        this.preparadoPorUsuario = preparadoPorUsuario;
        this.preparadoPorCliente = preparadoPorCliente;
        this.aprovadoPor = aprovadoPor;
        this.descartadoPor = descartadoPor;
        this.nota = nota;
    }

    public CancelamentoEntity(CancelamentoEntityBuilder builder) {
        this.version = builder.version;
        this.codigoMotivo = builder.codigoMotivo;
        this.motivo = builder.motivo;
        this.dataHoraAprovacao = builder.dataHoraAprovacao;
        this.dataHoraDescarte = builder.dataHoraDescarte;
        this.dataHoraRecusa = builder.dataHoraRecusa;
        this.estado = builder.estado;
        this.codigoRetorno = builder.codigoRetorno;
        this.mensagemRetorno = builder.mensagemRetorno;
        this.dataHoraEvento = builder.dataHoraEvento;
        this.xml = builder.xml;
        this.pdf = builder.pdf;
        this.id = builder.id;
        this.ambiente = builder.ambiente;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.empresa = builder.empresa;
        this.preparadoPorUsuario = builder.preparadoPorUsuario;
        this.preparadoPorCliente = builder.preparadoPorCliente;
        this.aprovadoPor = builder.aprovadoPor;
        this.descartadoPor = builder.descartadoPor;
        this.nota = builder.nota;
    }

    public static CancelamentoEntityBuilder builderOf() {
        return new CancelamentoEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setCodigoMotivo(Integer codigoMotivo) {
        this.codigoMotivo = codigoMotivo;
    }

    public Integer getCodigoMotivo() {
        return this.codigoMotivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() {
        return this.motivo;
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

    public void setDataHoraRecusa(LocalDateTime dataHoraRecusa) {
        this.dataHoraRecusa = dataHoraRecusa;
    }

    public LocalDateTime getDataHoraRecusa() {
        return this.dataHoraRecusa;
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

    public void setDataHoraEvento(LocalDateTime dataHoraEvento) {
        this.dataHoraEvento = dataHoraEvento;
    }

    public LocalDateTime getDataHoraEvento() {
        return this.dataHoraEvento;
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

    public void setNota(NotaEntity nota) {
        this.nota = nota;
    }

    public NotaEntity getNota() {
        return this.nota;
    }

}
