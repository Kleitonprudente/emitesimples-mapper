package br.com.emitesimples.financeiro.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;

/*
* Registro de cada cobrança por e-mail (manual ou lembrete) mandada a um cliente 
* (docs/financeiro/README.md, 9.3, Fase 6; referência §9.1).
 * No máximo UMA por cliente por dia: o índice único (empresa, idPessoa, dia) 
* reserva o dia ANTES de mandar o e-mail; se o envio falha, a reserva é apagada.
 * Não tem CRUD gerado: só o serviço de cobrança (financeiroop) grava. A pessoa é 
* guardada como idPessoa (como no Título).

*/
public class CobrancaEnviada {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute idPessoa can't be empty.")
    @Size(max = 36, message = "Attribute idPessoa must have 36 characters maximum.")
    private String idPessoa;

    @NotEmpty(message = "The attribute origem can't be empty.")
    @Size(max = 10, message = "Attribute origem must have 10 characters maximum.")
    private String origem;

    @NotNull(message = "The attribute dia can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate dia;

    @NotEmpty(message = "The attribute email can't be empty.")
    @Size(max = 255, message = "Attribute email must have 255 characters maximum.")
    private String email;

    @NotNull(message = "The attribute enviadoEm can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime enviadoEm;

    @Size(max = 36, message = "Attribute idEnviadoPor must have 36 characters maximum.")
    private String idEnviadoPor;

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

    public CobrancaEnviada() {
        this.version = null;
        this.idPessoa = null;
        this.origem = null;
        this.dia = null;
        this.email = null;
        this.enviadoEm = null;
        this.idEnviadoPor = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.titulo = null;
        this.empresa = null;
    }
    public CobrancaEnviada(
        LocalDateTime version,
        String idPessoa,
        String origem,
        LocalDate dia,
        String email,
        LocalDateTime enviadoEm,
        String idEnviadoPor,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Titulo titulo,
        Empresa empresa
    ) {
        this.version = version;
        this.idPessoa = idPessoa;
        this.origem = origem;
        this.dia = dia;
        this.email = email;
        this.enviadoEm = enviadoEm;
        this.idEnviadoPor = idEnviadoPor;
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

    public void setIdPessoa(String idPessoa) {
        this.idPessoa = idPessoa;
    }

    public String getIdPessoa() {
        return this.idPessoa;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getOrigem() {
        return this.origem;
    }

    public void setDia(LocalDate dia) {
        this.dia = dia;
    }

    public LocalDate getDia() {
        return this.dia;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEnviadoEm(LocalDateTime enviadoEm) {
        this.enviadoEm = enviadoEm;
    }

    public LocalDateTime getEnviadoEm() {
        return this.enviadoEm;
    }

    public void setIdEnviadoPor(String idEnviadoPor) {
        this.idEnviadoPor = idEnviadoPor;
    }

    public String getIdEnviadoPor() {
        return this.idEnviadoPor;
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
            this.idPessoa != null || 
            this.origem != null || 
            this.dia != null || 
            this.email != null || 
            this.enviadoEm != null || 
            this.idEnviadoPor != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.titulo != null || 
            this.empresa != null;
    }

}
