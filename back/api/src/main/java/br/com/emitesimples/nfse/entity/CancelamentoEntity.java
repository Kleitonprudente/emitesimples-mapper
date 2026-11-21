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
 * Prazos e regras de cancelamento por município não estão documentados e não são 
* validados no modelo.
 * Cancelar exige confirmação humana: um agente só PEDE (aguardando_aprovacao) e 
* uma pessoa com o scope de cancelar (papel administrador) aprova, e só então o 
* sistema envia ao gateway.
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
    @JoinColumn(name="RECUSADO_POR_ID", nullable = true)
    private UsuarioEntity recusadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="NOTA_ID", nullable = false)
    private NotaEntity nota;

    public CancelamentoEntity() {
    }

    public CancelamentoEntity(
        Integer codigoMotivo,
        String motivo,
        LocalDateTime dataHoraAprovacao,
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
        UsuarioEntity recusadoPor,
        NotaEntity nota
    ) {
        this.codigoMotivo = codigoMotivo;
        this.motivo = motivo;
        this.dataHoraAprovacao = dataHoraAprovacao;
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
        this.recusadoPor = recusadoPor;
        this.nota = nota;
    }

    public CancelamentoEntity(CancelamentoEntityBuilder builder) {
        this.version = builder.version;
        this.codigoMotivo = builder.codigoMotivo;
        this.motivo = builder.motivo;
        this.dataHoraAprovacao = builder.dataHoraAprovacao;
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
        this.recusadoPor = builder.recusadoPor;
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

    public void setRecusadoPor(UsuarioEntity recusadoPor) {
        this.recusadoPor = recusadoPor;
    }

    public UsuarioEntity getRecusadoPor() {
        return this.recusadoPor;
    }

    public void setNota(NotaEntity nota) {
        this.nota = nota;
    }

    public NotaEntity getNota() {
        return this.nota;
    }

}
