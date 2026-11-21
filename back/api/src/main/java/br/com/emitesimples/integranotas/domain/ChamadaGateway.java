package br.com.emitesimples.integranotas.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.nfse.domain.Nota;

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
public class ChamadaGateway {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute operacao can't be empty.")
    @Size(max = 30, message = "Attribute operacao must have 30 characters maximum.")
    private String operacao;

    @NotEmpty(message = "The attribute metodoHttp can't be empty.")
    @Size(max = 7, message = "Attribute metodoHttp must have 7 characters maximum.")
    private String metodoHttp;

    @NotEmpty(message = "The attribute rota can't be empty.")
    @Size(max = 200, message = "Attribute rota must have 200 characters maximum.")
    private String rota;

    @NotEmpty(message = "The attribute versaoApi can't be empty.")
    @Size(max = 5, message = "Attribute versaoApi must have 5 characters maximum.")
    private String versaoApi;

    private Integer codigoHttp;

    private Integer codigoRetorno;

    @Size(max = 500, message = "Attribute mensagemRetorno must have 500 characters maximum.")
    private String mensagemRetorno;

    private Integer duracaoMs;

    @NotNull(message = "The attribute dataChamada can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataChamada;

    @Size(max = 50, message = "Attribute chave must have 50 characters maximum.")
    private String chave;

    @Size(max = 40, message = "Attribute identificacao must have 40 characters maximum.")
    private String identificacao;

    @Size(max = 500, message = "Attribute resumoEnvio must have 500 characters maximum.")
    private String resumoEnvio;

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

    private Nota nota;

    public ChamadaGateway() {
        this.version = null;
        this.operacao = null;
        this.metodoHttp = null;
        this.rota = null;
        this.versaoApi = null;
        this.codigoHttp = null;
        this.codigoRetorno = null;
        this.mensagemRetorno = null;
        this.duracaoMs = null;
        this.dataChamada = null;
        this.chave = null;
        this.identificacao = null;
        this.resumoEnvio = null;
        this.id = null;
        this.ambiente = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.empresa = null;
        this.nota = null;
    }
    public ChamadaGateway(
        LocalDateTime version,
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
        Empresa empresa,
        Nota nota
    ) {
        this.version = version;
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

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Empresa getEmpresa() {
        return this.empresa;
    }

    public void setNota(Nota nota) {
        this.nota = nota;
    }

    public Nota getNota() {
        return this.nota;
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
            this.operacao != null || 
            this.metodoHttp != null || 
            this.rota != null || 
            this.versaoApi != null || 
            this.codigoHttp != null || 
            this.codigoRetorno != null || 
            this.mensagemRetorno != null || 
            this.duracaoMs != null || 
            this.dataChamada != null || 
            this.chave != null || 
            this.identificacao != null || 
            this.resumoEnvio != null || 
            this.ambiente != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.empresa != null || 
            this.nota != null;
    }

}
