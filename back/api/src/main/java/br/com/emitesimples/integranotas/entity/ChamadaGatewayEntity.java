package br.com.emitesimples.integranotas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.integranotas.domain.ChamadaGateway;
import br.com.emitesimples.integranotas.builder.ChamadaGatewayEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.nfse.entity.NotaEntity;

/*
* Registro de auditoria de cada chamada que o sistema faz ao IntegraNotas, para 
* operar e depurar. Guarda operação, método, rota, versão da API, códigos de 
* retorno, mensagem, duração e a chave da nota.
 * A versão da API é por operação: o envio da nota usa v2, a consulta e o 
* cancelamento usam v1.
 * NUNCA guarda token, senha, corpo com segredos, XML ou PDF. Guarda só um resumo 
* seguro do que foi enviado (campo resumoEnvio, sem dado pessoal, sem segredo e 
* sem documento).

*/
@Entity
@Table(name = "CHAMADAGATEWAY")
public class ChamadaGatewayEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="OPERACAO", length=30)
    private String operacao;

    @Column(name="METODO_HTTP", length=7)
    private String metodoHttp;

    @Column(name="ROTA", length=200)
    private String rota;

    @Column(name="VERSAO_API", length=5)
    private String versaoApi;

    @Column(name="CODIGO_HTTP", precision = 3)
    private Integer codigoHttp;

    @Column(name="CODIGO_RETORNO", precision = 4)
    private Integer codigoRetorno;

    @Column(name="MENSAGEM_RETORNO", length=500)
    private String mensagemRetorno;

    @Column(name="DURACAO_MS", precision = 9)
    private Integer duracaoMs;

    @Column(name="DATA_CHAMADA")
    private LocalDateTime dataChamada;

    @Column(name="CHAVE", length=50)
    private String chave;

    @Column(name="IDENTIFICACAO", length=40)
    private String identificacao;

    @Column(name="RESUMO_ENVIO", length=500)
    private String resumoEnvio;
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
    @JoinColumn(name="NOTA_ID", nullable = true)
    private NotaEntity nota;

    public ChamadaGatewayEntity() {
    }

    public ChamadaGatewayEntity(
        String operacao,
        String metodoHttp,
        String rota,
        String versaoApi,
        Integer codigoHttp,
        Integer codigoRetorno,
        String mensagemRetorno,
        Integer duracaoMs,
        LocalDateTime dataChamada,
        String chave,
        String identificacao,
        String resumoEnvio,
        String id,
        String ambiente,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        EmpresaEntity empresa,
        NotaEntity nota
    ) {
        this.operacao = operacao;
        this.metodoHttp = metodoHttp;
        this.rota = rota;
        this.versaoApi = versaoApi;
        this.codigoHttp = codigoHttp;
        this.codigoRetorno = codigoRetorno;
        this.mensagemRetorno = mensagemRetorno;
        this.duracaoMs = duracaoMs;
        this.dataChamada = dataChamada;
        this.chave = chave;
        this.identificacao = identificacao;
        this.resumoEnvio = resumoEnvio;
        this.id = id;
        this.ambiente = ambiente;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
        this.nota = nota;
    }

    public ChamadaGatewayEntity(ChamadaGatewayEntityBuilder builder) {
        this.version = builder.version;
        this.operacao = builder.operacao;
        this.metodoHttp = builder.metodoHttp;
        this.rota = builder.rota;
        this.versaoApi = builder.versaoApi;
        this.codigoHttp = builder.codigoHttp;
        this.codigoRetorno = builder.codigoRetorno;
        this.mensagemRetorno = builder.mensagemRetorno;
        this.duracaoMs = builder.duracaoMs;
        this.dataChamada = builder.dataChamada;
        this.chave = builder.chave;
        this.identificacao = builder.identificacao;
        this.resumoEnvio = builder.resumoEnvio;
        this.id = builder.id;
        this.ambiente = builder.ambiente;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.empresa = builder.empresa;
        this.nota = builder.nota;
    }

    public static ChamadaGatewayEntityBuilder builderOf() {
        return new ChamadaGatewayEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setOperacao(String operacao) {
        this.operacao = operacao;
    }

    public String getOperacao() {
        return this.operacao;
    }

    public void setMetodoHttp(String metodoHttp) {
        this.metodoHttp = metodoHttp;
    }

    public String getMetodoHttp() {
        return this.metodoHttp;
    }

    public void setRota(String rota) {
        this.rota = rota;
    }

    public String getRota() {
        return this.rota;
    }

    public void setVersaoApi(String versaoApi) {
        this.versaoApi = versaoApi;
    }

    public String getVersaoApi() {
        return this.versaoApi;
    }

    public void setCodigoHttp(Integer codigoHttp) {
        this.codigoHttp = codigoHttp;
    }

    public Integer getCodigoHttp() {
        return this.codigoHttp;
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

    public void setDuracaoMs(Integer duracaoMs) {
        this.duracaoMs = duracaoMs;
    }

    public Integer getDuracaoMs() {
        return this.duracaoMs;
    }

    public void setDataChamada(LocalDateTime dataChamada) {
        this.dataChamada = dataChamada;
    }

    public LocalDateTime getDataChamada() {
        return this.dataChamada;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public String getChave() {
        return this.chave;
    }

    public void setIdentificacao(String identificacao) {
        this.identificacao = identificacao;
    }

    public String getIdentificacao() {
        return this.identificacao;
    }

    public void setResumoEnvio(String resumoEnvio) {
        this.resumoEnvio = resumoEnvio;
    }

    public String getResumoEnvio() {
        return this.resumoEnvio;
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

    public void setNota(NotaEntity nota) {
        this.nota = nota;
    }

    public NotaEntity getNota() {
        return this.nota;
    }

}
