package br.com.emitesimples.admin.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.usuarios.domain.Usuario;

/*
* Vínculo de uma empresa a um plano, feito pelo suporte. Não tem ambiente: herda a 
* situação da empresa.
 * Regra da aplicação (o mapa não a expressa, e o banco a reforça com índice 
* parcial): no máximo UMA assinatura ativa por empresa. Trocar de plano encerra a 
* assinatura anterior (preenche dataFim) e cria outra; nada é apagado.
 * A cobrança é do CRM: a assinatura é enviada ao CRM, e a referenciaCrm guarda o 
* id dela lá.

*/
public class AssinaturaEmpresa {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute estado can't be empty.")
    @Size(max = 10, message = "Attribute estado must have 10 characters maximum.")
    private String estado;

    @NotNull(message = "The attribute dataInicio can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate dataInicio;

    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate dataFim;

    @Size(max = 50, message = "Attribute referenciaCrm must have 50 characters maximum.")
    private String referenciaCrm;

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

    @NotNull(message = "The attribute plano can't be null.")
    private Plano plano;

    private Usuario vinculadaPor;

    public AssinaturaEmpresa() {
        this.version = null;
        this.estado = null;
        this.dataInicio = null;
        this.dataFim = null;
        this.referenciaCrm = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.empresa = null;
        this.plano = null;
        this.vinculadaPor = null;
    }
    public AssinaturaEmpresa(
        LocalDateTime version,
        String estado,
        LocalDate dataInicio,
        LocalDate dataFim,
        String referenciaCrm,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Empresa empresa,
        Plano plano,
        Usuario vinculadaPor
    ) {
        this.version = version;
        this.estado = estado;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.referenciaCrm = referenciaCrm;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
        this.plano = plano;
        this.vinculadaPor = vinculadaPor;
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return this.estado;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataInicio() {
        return this.dataInicio;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public LocalDate getDataFim() {
        return this.dataFim;
    }

    public void setReferenciaCrm(String referenciaCrm) {
        this.referenciaCrm = referenciaCrm;
    }

    public String getReferenciaCrm() {
        return this.referenciaCrm;
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

    public void setPlano(Plano plano) {
        this.plano = plano;
    }

    public Plano getPlano() {
        return this.plano;
    }

    public void setVinculadaPor(Usuario vinculadaPor) {
        this.vinculadaPor = vinculadaPor;
    }

    public Usuario getVinculadaPor() {
        return this.vinculadaPor;
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
            this.estado != null || 
            this.dataInicio != null || 
            this.dataFim != null || 
            this.referenciaCrm != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.empresa != null || 
            this.plano != null || 
            this.vinculadaPor != null;
    }

}
