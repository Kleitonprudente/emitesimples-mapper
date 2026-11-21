package br.com.emitesimples.cadastros.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
* Município brasileiro identificado pelo código IBGE de 7 dígitos, exceto o 
* registro especial Exterior (UF EX), que não tem IBGE e carrega o código 0 da 
* tabela oficial; esse 0 nunca é enviado ao IntegraNotas (para pessoa no exterior 
* o envio manda uf = EX e omite o código do município). Dado de referência 
* compartilhado:
 * endereço de Empresa aponta para ele, e a Pessoa o referencia por cidade e UF 
* (relação cidade): a tela mostra cidade e UF e esconde o IBGE, que está direto 
* aqui.
 * O par (nome, UF) é único, regra documentada e não expressa no mapa. O Municipio 
* é buscado por nome e UF. Todo registro tem código: o IBGE de 7 dígitos ou, no 
* Exterior, o 0.
 * Vive no contexto cadastros (domain.cadastros). Há dependência mútua entre os 
* contextos configuracao e cadastros:
 * a Empresa (configuracao) referencia o Municipio, e Pessoa e Servico (cadastros) 
* referenciam a Empresa.
 * Municipio e Empresa devem ser modelados juntos.

*/
public class Municipio {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute codigoIbge can't be empty.")
    @Size(max = 7, message = "Attribute codigoIbge must have 7 characters maximum.")
    private String codigoIbge;

    @NotEmpty(message = "The attribute nome can't be empty.")
    @Size(max = 100, message = "Attribute nome must have 100 characters maximum.")
    private String nome;

    @NotEmpty(message = "The attribute uf can't be empty.")
    @Size(max = 2, message = "Attribute uf must have 2 characters maximum.")
    private String uf;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    public Municipio() {
        this.version = null;
        this.codigoIbge = null;
        this.nome = null;
        this.uf = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
    }
    public Municipio(
        LocalDateTime version,
        String codigoIbge,
        String nome,
        String uf,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
    ) {
        this.version = version;
        this.codigoIbge = codigoIbge;
        this.nome = nome;
        this.uf = uf;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setCodigoIbge(String codigoIbge) {
        this.codigoIbge = codigoIbge;
    }

    public String getCodigoIbge() {
        return this.codigoIbge;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return this.nome;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getUf() {
        return this.uf;
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
            this.codigoIbge != null || 
            this.nome != null || 
            this.uf != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null;
    }

}
