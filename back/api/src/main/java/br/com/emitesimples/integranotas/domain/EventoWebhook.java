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
* Cada notificação recebida do gateway no endpoint de webhook do sistema. Registro 
* de auditoria e de idempotência.
 * ATENÇÃO: o corpo exato do webhook de emissão de NFS-e AINDA NÃO FOI CONFIRMADO 
* (as páginas Notificação com sucesso e Notificação com falha não foram lidas). 
* Este modelo cobre só o envelope comum conhecido (origem, cnpj_cpf, tipo) e os 
* campos chave, sucesso, código, mensagem e identificação. Quando o corpo for 
* confirmado, o mapa será revisado.
 * O XML e o PDF do webhook NÃO são guardados aqui: a Nota guarda os documentos.
 * Idempotência: a mesma notificação pode chegar até 3 vezes. Chave de deduplicação 
* sugerida: (empresa, ambiente, chave, código). O mapa não expressa unicidade 
* composta, ela precisa virar constraint única no banco, e uma repetição deve ser 
* reconhecida e marcada como ignorada em vez de reprocessada.
 * A empresa (emitente no IntegraNotas) é resolvida pelo cnpj_cpf e pelo ambiente. 
* A validade da assinatura é registrada, mas não há validação por header e a 
* assinatura na v2 não está confirmada.

*/
public class EventoWebhook {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @Size(max = 30, message = "Attribute origem must have 30 characters maximum.")
    private String origem;

    @Size(max = 14, message = "Attribute cnpjCpf must have 14 characters maximum.")
    private String cnpjCpf;

    @Size(max = 30, message = "Attribute tipo must have 30 characters maximum.")
    private String tipo;

    @Size(max = 50, message = "Attribute chave must have 50 characters maximum.")
    private String chave;

    private Boolean sucesso;

    private Integer codigo;

    @Size(max = 500, message = "Attribute mensagem must have 500 characters maximum.")
    private String mensagem;

    @Size(max = 40, message = "Attribute identificacao must have 40 characters maximum.")
    private String identificacao;

    @NotNull(message = "The attribute dataRecebimento can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataRecebimento;

    @NotEmpty(message = "The attribute resultadoAssinatura can't be empty.")
    @Size(max = 15, message = "Attribute resultadoAssinatura must have 15 characters maximum.")
    private String resultadoAssinatura;

    @NotEmpty(message = "The attribute estadoProcessamento can't be empty.")
    @Size(max = 15, message = "Attribute estadoProcessamento must have 15 characters maximum.")
    private String estadoProcessamento;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataProcessamento;

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

    public EventoWebhook() {
        this.version = null;
        this.origem = null;
        this.cnpjCpf = null;
        this.tipo = null;
        this.chave = null;
        this.sucesso = null;
        this.codigo = null;
        this.mensagem = null;
        this.identificacao = null;
        this.dataRecebimento = null;
        this.resultadoAssinatura = null;
        this.estadoProcessamento = null;
        this.dataProcessamento = null;
        this.id = null;
        this.ambiente = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.empresa = null;
        this.nota = null;
    }
    public EventoWebhook(
        LocalDateTime version,
        String origem,
        String cnpjCpf,
        String tipo,
        String chave,
        Boolean sucesso,
        Integer codigo,
        String mensagem,
        String identificacao,
        LocalDateTime dataRecebimento,
        String resultadoAssinatura,
        String estadoProcessamento,
        LocalDateTime dataProcessamento,
        String id,
        String ambiente,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Empresa empresa,
        Nota nota
    ) {
        this.version = version;
        this.origem = origem;
        this.cnpjCpf = cnpjCpf;
        this.tipo = tipo;
        this.chave = chave;
        this.sucesso = sucesso;
        this.codigo = codigo;
        this.mensagem = mensagem;
        this.identificacao = identificacao;
        this.dataRecebimento = dataRecebimento;
        this.resultadoAssinatura = resultadoAssinatura;
        this.estadoProcessamento = estadoProcessamento;
        this.dataProcessamento = dataProcessamento;
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

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getOrigem() {
        return this.origem;
    }

    public void setCnpjCpf(String cnpjCpf) {
        this.cnpjCpf = cnpjCpf;
    }

    public String getCnpjCpf() {
        return this.cnpjCpf;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTipo() {
        return this.tipo;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public String getChave() {
        return this.chave;
    }

    public void setSucesso(Boolean sucesso) {
        this.sucesso = sucesso;
    }

    public Boolean getSucesso() {
        return this.sucesso;
    }

    public void setCodigo(Integer codigo) {
        this.codigo = codigo;
    }

    public Integer getCodigo() {
        return this.codigo;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return this.mensagem;
    }

    public void setIdentificacao(String identificacao) {
        this.identificacao = identificacao;
    }

    public String getIdentificacao() {
        return this.identificacao;
    }

    public void setDataRecebimento(LocalDateTime dataRecebimento) {
        this.dataRecebimento = dataRecebimento;
    }

    public LocalDateTime getDataRecebimento() {
        return this.dataRecebimento;
    }

    public void setResultadoAssinatura(String resultadoAssinatura) {
        this.resultadoAssinatura = resultadoAssinatura;
    }

    public String getResultadoAssinatura() {
        return this.resultadoAssinatura;
    }

    public void setEstadoProcessamento(String estadoProcessamento) {
        this.estadoProcessamento = estadoProcessamento;
    }

    public String getEstadoProcessamento() {
        return this.estadoProcessamento;
    }

    public void setDataProcessamento(LocalDateTime dataProcessamento) {
        this.dataProcessamento = dataProcessamento;
    }

    public LocalDateTime getDataProcessamento() {
        return this.dataProcessamento;
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
            this.origem != null || 
            this.cnpjCpf != null || 
            this.tipo != null || 
            this.chave != null || 
            this.sucesso != null || 
            this.codigo != null || 
            this.mensagem != null || 
            this.identificacao != null || 
            this.dataRecebimento != null || 
            this.resultadoAssinatura != null || 
            this.estadoProcessamento != null || 
            this.dataProcessamento != null || 
            this.ambiente != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.empresa != null || 
            this.nota != null;
    }

}
