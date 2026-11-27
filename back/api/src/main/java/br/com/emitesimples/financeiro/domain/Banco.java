package br.com.emitesimples.financeiro.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
* Banco do cadastro público do Banco Central. Dado de referência GLOBAL, 
* compartilhado por todas as empresas: não tem empresa nem RLS.
 * A ContaFinanceira guarda só o código do banco (codigoBanco), que precisa existir 
* aqui.
 * Regras documentadas e não expressas no mapa (docs/financeiro/README.md, F1-H5): 
* o código é único, sem zeros à esquerda.
 * Mantido pelo administrador da plataforma; a carga pelo CSV do Banco Central (que 
* nunca apaga nem desmarca banco) está pendente.
 * Vive no contexto financeiro (domain.financeiro).

*/
public class Banco {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute codigo can't be empty.")
    @Size(max = 4, message = "Attribute codigo must have 4 characters maximum.")
    private String codigo;

    @Size(max = 8, message = "Attribute ispb must have 8 characters maximum.")
    private String ispb;

    @NotEmpty(message = "The attribute nome can't be empty.")
    @Size(max = 120, message = "Attribute nome must have 120 characters maximum.")
    private String nome;

    @Size(max = 200, message = "Attribute nomeExtenso must have 200 characters maximum.")
    private String nomeExtenso;

    @NotNull(message = "The attribute participaCompe can't be null.")
    private Boolean participaCompe;

    @NotNull(message = "The attribute integradoGateway can't be null.")
    private Boolean integradoGateway;

    @Size(max = 500, message = "Attribute urlPassoAPasso must have 500 characters maximum.")
    private String urlPassoAPasso;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    public Banco() {
        this.version = null;
        this.codigo = null;
        this.ispb = null;
        this.nome = null;
        this.nomeExtenso = null;
        this.participaCompe = null;
        this.integradoGateway = null;
        this.urlPassoAPasso = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
    }
    public Banco(
        LocalDateTime version,
        String codigo,
        String ispb,
        String nome,
        String nomeExtenso,
        Boolean participaCompe,
        Boolean integradoGateway,
        String urlPassoAPasso,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
    ) {
        this.version = version;
        this.codigo = codigo;
        this.ispb = ispb;
        this.nome = nome;
        this.nomeExtenso = nomeExtenso;
        this.participaCompe = participaCompe;
        this.integradoGateway = integradoGateway;
        this.urlPassoAPasso = urlPassoAPasso;
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

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return this.codigo;
    }

    public void setIspb(String ispb) {
        this.ispb = ispb;
    }

    public String getIspb() {
        return this.ispb;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return this.nome;
    }

    public void setNomeExtenso(String nomeExtenso) {
        this.nomeExtenso = nomeExtenso;
    }

    public String getNomeExtenso() {
        return this.nomeExtenso;
    }

    public void setParticipaCompe(Boolean participaCompe) {
        this.participaCompe = participaCompe;
    }

    public Boolean getParticipaCompe() {
        return this.participaCompe;
    }

    public void setIntegradoGateway(Boolean integradoGateway) {
        this.integradoGateway = integradoGateway;
    }

    public Boolean getIntegradoGateway() {
        return this.integradoGateway;
    }

    public void setUrlPassoAPasso(String urlPassoAPasso) {
        this.urlPassoAPasso = urlPassoAPasso;
    }

    public String getUrlPassoAPasso() {
        return this.urlPassoAPasso;
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
            this.codigo != null || 
            this.ispb != null || 
            this.nome != null || 
            this.nomeExtenso != null || 
            this.participaCompe != null || 
            this.integradoGateway != null || 
            this.urlPassoAPasso != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null;
    }

}
