package br.com.emitesimples.financeiro.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;

/*
* Classifica receitas e despesas para relatórios. Hierarquia de UM nível: uma 
* categoria de topo (sem pai) agrupa filhas; o pai é só agrupador.
 * Título, movimento avulso e categoria padrão da configuração só usam categoria 
* SEM filhas (folha). Nome exibido "Pai › Filha".
 * Regras documentadas e não expressas no mapa (docs/financeiro/README.md, F1-H2), 
* implementadas na migração (trigger) e em listener à mão:
 * nome único por empresa e tipo; o pai é da mesma empresa, do mesmo tipo, ativo e 
* de topo; ninguém é pai de si mesmo;
 * quem tem filhas não vira filha nem muda de tipo; só vira pai a categoria fora de 
* uso (não é padrão da configuração e, nas Fases 2 e 3,
 * não tem título nem movimento); inativar pai com filha ativa e ativar filha sob 
* pai inativo são recusados; as dicas perdem os sinais < e >.
 * Nunca se apaga: só se desativa. Escrita só do administrador da empresa.

*/
public class CategoriaFinanceira {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute nome can't be empty.")
    @Size(max = 100, message = "Attribute nome must have 100 characters maximum.")
    private String nome;

    @NotEmpty(message = "The attribute tipo can't be empty.")
    @Size(max = 10, message = "Attribute tipo must have 10 characters maximum.")
    private String tipo;

    @Size(max = 500, message = "Attribute dicas must have 500 characters maximum.")
    private String dicas;

    @NotNull(message = "The attribute ativo can't be null.")
    private Boolean ativo;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    private CategoriaFinanceira categoriaPai;

    @NotNull(message = "The attribute empresa can't be null.")
    private Empresa empresa;

    public CategoriaFinanceira() {
        this.version = null;
        this.nome = null;
        this.tipo = null;
        this.dicas = null;
        this.ativo = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.categoriaPai = null;
        this.empresa = null;
    }
    public CategoriaFinanceira(
        LocalDateTime version,
        String nome,
        String tipo,
        String dicas,
        Boolean ativo,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        CategoriaFinanceira categoriaPai,
        Empresa empresa
    ) {
        this.version = version;
        this.nome = nome;
        this.tipo = tipo;
        this.dicas = dicas;
        this.ativo = ativo;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.categoriaPai = categoriaPai;
        this.empresa = empresa;
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return this.nome;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTipo() {
        return this.tipo;
    }

    public void setDicas(String dicas) {
        this.dicas = dicas;
    }

    public String getDicas() {
        return this.dicas;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Boolean getAtivo() {
        return this.ativo;
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

    public void setCategoriaPai(CategoriaFinanceira categoriaPai) {
        this.categoriaPai = categoriaPai;
    }

    public CategoriaFinanceira getCategoriaPai() {
        return this.categoriaPai;
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
            this.nome != null || 
            this.tipo != null || 
            this.dicas != null || 
            this.ativo != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.categoriaPai != null || 
            this.empresa != null;
    }

}
