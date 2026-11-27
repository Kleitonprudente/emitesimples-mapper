package br.com.emitesimples.financeiro.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
* Tabela GLOBAL (sem tenant) que acha a empresa quando o webhook do provedor chega 
* sem sessão (docs/financeiro/README.md, 9.5, Fase 7; referência §9.2).
 * Guarda só o hash SHA-256 do segredo do webhook, nunca o segredo. O webhook só 
* identifica a cobrança: quem muda o estado é o sincronizador, que sempre consulta 
* o provedor.
 * Não tem CRUD gerado: só a integração (financeiroop) grava.

*/
public class CobrancaDestino {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute idEmpresa can't be empty.")
    @Size(max = 36, message = "Attribute idEmpresa must have 36 characters maximum.")
    private String idEmpresa;

    @NotEmpty(message = "The attribute segredoHash can't be empty.")
    @Size(max = 64, message = "Attribute segredoHash must have 64 characters maximum.")
    private String segredoHash;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    @NotNull(message = "The attribute credencial can't be null.")
    private CredencialCobranca credencial;

    public CobrancaDestino() {
        this.version = null;
        this.idEmpresa = null;
        this.segredoHash = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.credencial = null;
    }
    public CobrancaDestino(
        LocalDateTime version,
        String idEmpresa,
        String segredoHash,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        CredencialCobranca credencial
    ) {
        this.version = version;
        this.idEmpresa = idEmpresa;
        this.segredoHash = segredoHash;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.credencial = credencial;
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setIdEmpresa(String idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public String getIdEmpresa() {
        return this.idEmpresa;
    }

    public void setSegredoHash(String segredoHash) {
        this.segredoHash = segredoHash;
    }

    public String getSegredoHash() {
        return this.segredoHash;
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

    public void setCredencial(CredencialCobranca credencial) {
        this.credencial = credencial;
    }

    public CredencialCobranca getCredencial() {
        return this.credencial;
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
            this.idEmpresa != null || 
            this.segredoHash != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.credencial != null;
    }

}
