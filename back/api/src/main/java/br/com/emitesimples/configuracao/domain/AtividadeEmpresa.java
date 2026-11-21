package br.com.emitesimples.configuracao.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
* Atividade econômica (CNAE) de uma Empresa, com tipo, código CNAE e descrição. 
* Uma empresa tem uma atividade principal e várias secundárias. A regra de no 
* máximo uma principal por empresa é da aplicação.
 * Existe só dentro da empresa e no mesmo contexto, por isso é detalhe da Empresa 
* ([m]/[o]). O tenant é a própria empresa. Não tem ambiente: herda o da Empresa.
 * Vive no contexto configuracao (domain.configuracao).

*/
public class AtividadeEmpresa {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute tipo can't be empty.")
    @Size(max = 10, message = "Attribute tipo must have 10 characters maximum.")
    private String tipo;

    @NotEmpty(message = "The attribute codigoCnae can't be empty.")
    @Size(max = 7, message = "Attribute codigoCnae must have 7 characters maximum.")
    private String codigoCnae;

    @NotEmpty(message = "The attribute descricao can't be empty.")
    @Size(max = 300, message = "Attribute descricao must have 300 characters maximum.")
    private String descricao;

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

    public AtividadeEmpresa() {
        this.version = null;
        this.tipo = null;
        this.codigoCnae = null;
        this.descricao = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.empresa = null;
    }
    public AtividadeEmpresa(
        LocalDateTime version,
        String tipo,
        String codigoCnae,
        String descricao,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Empresa empresa
    ) {
        this.version = version;
        this.tipo = tipo;
        this.codigoCnae = codigoCnae;
        this.descricao = descricao;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTipo() {
        return this.tipo;
    }

    public void setCodigoCnae(String codigoCnae) {
        this.codigoCnae = codigoCnae;
    }

    public String getCodigoCnae() {
        return this.codigoCnae;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return this.descricao;
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
            this.tipo != null || 
            this.codigoCnae != null || 
            this.descricao != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.empresa != null;
    }

}
