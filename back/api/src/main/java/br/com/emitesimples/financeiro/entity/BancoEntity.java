package br.com.emitesimples.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.Banco;
import br.com.emitesimples.financeiro.builder.BancoEntityBuilder;

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
@Entity
@Table(name = "BANCO")
public class BancoEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="CODIGO", length=4)
    private String codigo;

    @Column(name="ISPB", length=8)
    private String ispb;

    @Column(name="NOME", length=120)
    private String nome;

    @Column(name="NOME_EXTENSO", length=200)
    private String nomeExtenso;

    @Column(name="PARTICIPA_COMPE")
    private Boolean participaCompe;

    @Column(name="INTEGRADO_GATEWAY")
    private Boolean integradoGateway;

    @Column(name="URL_PASSO_A_PASSO", length=500)
    private String urlPassoAPasso;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    public BancoEntity() {
    }

    public BancoEntity(
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

    public BancoEntity(BancoEntityBuilder builder) {
        this.version = builder.version;
        this.codigo = builder.codigo;
        this.ispb = builder.ispb;
        this.nome = builder.nome;
        this.nomeExtenso = builder.nomeExtenso;
        this.participaCompe = builder.participaCompe;
        this.integradoGateway = builder.integradoGateway;
        this.urlPassoAPasso = builder.urlPassoAPasso;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
    }

    public static BancoEntityBuilder builderOf() {
        return new BancoEntityBuilder();
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

}
