package br.com.emitesimples.financeiro.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;

/*
* Marca de que um lembrete automático de cobrança já saiu para um título 
* (docs/financeiro/README.md, 9.3, Fase 6; referência §9.1): cada aviso sai UMA 
* vez.
 * O job diário (9h, por empresa, só com lembretesAtivos na configuração) grava 
* aqui antes de enviar; o índice único (título, tipo, dias) impede repetir.
 * Não tem CRUD gerado: só o serviço de cobrança (financeiroop) grava. Vive no 
* contexto financeiro (domain.financeiro).

*/
public class LembreteCobranca {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute tipo can't be empty.")
    @Size(max = 10, message = "Attribute tipo must have 10 characters maximum.")
    private String tipo;

    @NotNull(message = "The attribute dias can't be null.")
    private Integer dias;

    @NotNull(message = "The attribute enviadoEm can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime enviadoEm;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    @NotNull(message = "The attribute titulo can't be null.")
    private Titulo titulo;

    @NotNull(message = "The attribute empresa can't be null.")
    private Empresa empresa;

    public LembreteCobranca() {
        this.version = null;
        this.tipo = null;
        this.dias = null;
        this.enviadoEm = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.titulo = null;
        this.empresa = null;
    }
    public LembreteCobranca(
        LocalDateTime version,
        String tipo,
        Integer dias,
        LocalDateTime enviadoEm,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Titulo titulo,
        Empresa empresa
    ) {
        this.version = version;
        this.tipo = tipo;
        this.dias = dias;
        this.enviadoEm = enviadoEm;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.titulo = titulo;
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

    public void setDias(Integer dias) {
        this.dias = dias;
    }

    public Integer getDias() {
        return this.dias;
    }

    public void setEnviadoEm(LocalDateTime enviadoEm) {
        this.enviadoEm = enviadoEm;
    }

    public LocalDateTime getEnviadoEm() {
        return this.enviadoEm;
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

    public void setTitulo(Titulo titulo) {
        this.titulo = titulo;
    }

    public Titulo getTitulo() {
        return this.titulo;
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
            this.dias != null || 
            this.enviadoEm != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.titulo != null || 
            this.empresa != null;
    }

}
