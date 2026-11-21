package br.com.emitesimples.admin.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.usuarios.domain.Usuario;

/*
* Auditoria de cada mudança da situação da conta da empresa. É IMUTÁVEL: o 
* registro nunca é editado nem apagado (regra da aplicação). Responde quem mudou, 
* quando e por quê. Não tem ambiente: herda a situação da empresa.
 * Quando a mudança é automática, por exemplo a confirmação pelo link do e-mail, o 
* responsável fica vazio.

*/
public class HistoricoSituacaoEmpresa {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute situacaoAnterior can't be empty.")
    @Size(max = 20, message = "Attribute situacaoAnterior must have 20 characters maximum.")
    private String situacaoAnterior;

    @NotEmpty(message = "The attribute situacaoNova can't be empty.")
    @Size(max = 20, message = "Attribute situacaoNova must have 20 characters maximum.")
    private String situacaoNova;

    @Size(max = 500, message = "Attribute motivo must have 500 characters maximum.")
    private String motivo;

    @NotEmpty(message = "The attribute origem can't be empty.")
    @Size(max = 20, message = "Attribute origem must have 20 characters maximum.")
    private String origem;

    @NotNull(message = "The attribute dataHora can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataHora;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    @NotNull(message = "The attribute empresa can't be null.")
    private Empresa empresa;

    private Usuario responsavel;

    public HistoricoSituacaoEmpresa() {
        this.version = null;
        this.situacaoAnterior = null;
        this.situacaoNova = null;
        this.motivo = null;
        this.origem = null;
        this.dataHora = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.empresa = null;
        this.responsavel = null;
    }
    public HistoricoSituacaoEmpresa(
        LocalDateTime version,
        String situacaoAnterior,
        String situacaoNova,
        String motivo,
        String origem,
        LocalDateTime dataHora,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Empresa empresa,
        Usuario responsavel
    ) {
        this.version = version;
        this.situacaoAnterior = situacaoAnterior;
        this.situacaoNova = situacaoNova;
        this.motivo = motivo;
        this.origem = origem;
        this.dataHora = dataHora;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
        this.responsavel = responsavel;
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setSituacaoAnterior(String situacaoAnterior) {
        this.situacaoAnterior = situacaoAnterior;
    }

    public String getSituacaoAnterior() {
        return this.situacaoAnterior;
    }

    public void setSituacaoNova(String situacaoNova) {
        this.situacaoNova = situacaoNova;
    }

    public String getSituacaoNova() {
        return this.situacaoNova;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() {
        return this.motivo;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getOrigem() {
        return this.origem;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public LocalDateTime getDataHora() {
        return this.dataHora;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
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

    public void setResponsavel(Usuario responsavel) {
        this.responsavel = responsavel;
    }

    public Usuario getResponsavel() {
        return this.responsavel;
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
            this.situacaoAnterior != null || 
            this.situacaoNova != null || 
            this.motivo != null || 
            this.origem != null || 
            this.dataHora != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.empresa != null || 
            this.responsavel != null;
    }

}
