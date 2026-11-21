package br.com.emitesimples.nfse.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.usuarios.domain.Usuario;
import br.com.emitesimples.usuarios.domain.ClienteAcesso;

/*
* Pedido de cancelamento de uma Nota, com o evento e o retorno do gateway (XML e 
* PDF). Só se cancela nota autorizada,
 * caso contrário o gateway devolve 5013. Nota já cancelada devolve 5015, tratado 
* como sucesso idempotente.
 * Existe só dentro de uma nota e no mesmo contexto, por isso é detalhe da Nota 
* ([m]/[o]).
 * Prazos e regras de cancelamento por município não estão documentados e não são 
* validados no modelo.
 * Cancelar exige confirmação humana: um agente só PEDE (aguardando_aprovacao) e 
* uma pessoa com o scope de cancelar (papel administrador) aprova, e só então o 
* sistema envia ao gateway.
 * Vive no contexto nfse (domain.nfse). Depende do contexto usuarios (Usuario e 
* ClienteAcesso) para o rastro; usuarios depende só de cadastros e configuracao, 
* não há ciclo.

*/
public class Cancelamento {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotNull(message = "The attribute codigoMotivo can't be null.")
    private Integer codigoMotivo;

    @NotEmpty(message = "The attribute motivo can't be empty.")
    @Size(max = 255, message = "Attribute motivo must have 255 characters maximum.")
    private String motivo;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataHoraAprovacao;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataHoraRecusa;

    @NotEmpty(message = "The attribute estado can't be empty.")
    @Size(max = 25, message = "Attribute estado must have 25 characters maximum.")
    private String estado;

    private Integer codigoRetorno;

    @Size(max = 500, message = "Attribute mensagemRetorno must have 500 characters maximum.")
    private String mensagemRetorno;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataHoraEvento;

    private byte[] xml;

    private byte[] pdf;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotEmpty(message = "The attribute ambiente can't be empty.")
    @Size(max = 15, message = "Attribute ambiente must have 15 characters maximum.")
    private String ambiente;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    @NotNull(message = "The attribute empresa can't be null.")
    private Empresa empresa;

    private Usuario preparadoPorUsuario;

    private ClienteAcesso preparadoPorCliente;

    private Usuario aprovadoPor;

    private Usuario recusadoPor;

    @NotNull(message = "The attribute nota can't be null.")
    private Nota nota;

    public Cancelamento() {
        this.version = null;
        this.codigoMotivo = null;
        this.motivo = null;
        this.dataHoraAprovacao = null;
        this.dataHoraRecusa = null;
        this.estado = null;
        this.codigoRetorno = null;
        this.mensagemRetorno = null;
        this.dataHoraEvento = null;
        this.xml = null;
        this.pdf = null;
        this.id = null;
        this.ambiente = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.empresa = null;
        this.preparadoPorUsuario = null;
        this.preparadoPorCliente = null;
        this.aprovadoPor = null;
        this.recusadoPor = null;
        this.nota = null;
    }
    public Cancelamento(
        LocalDateTime version,
        Integer codigoMotivo,
        String motivo,
        LocalDateTime dataHoraAprovacao,
        LocalDateTime dataHoraRecusa,
        String estado,
        Integer codigoRetorno,
        String mensagemRetorno,
        LocalDateTime dataHoraEvento,
        byte[] xml,
        byte[] pdf,
        String id,
        String ambiente,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Empresa empresa,
        Usuario preparadoPorUsuario,
        ClienteAcesso preparadoPorCliente,
        Usuario aprovadoPor,
        Usuario recusadoPor,
        Nota nota
    ) {
        this.version = version;
        this.codigoMotivo = codigoMotivo;
        this.motivo = motivo;
        this.dataHoraAprovacao = dataHoraAprovacao;
        this.dataHoraRecusa = dataHoraRecusa;
        this.estado = estado;
        this.codigoRetorno = codigoRetorno;
        this.mensagemRetorno = mensagemRetorno;
        this.dataHoraEvento = dataHoraEvento;
        this.xml = xml;
        this.pdf = pdf;
        this.id = id;
        this.ambiente = ambiente;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
        this.preparadoPorUsuario = preparadoPorUsuario;
        this.preparadoPorCliente = preparadoPorCliente;
        this.aprovadoPor = aprovadoPor;
        this.recusadoPor = recusadoPor;
        this.nota = nota;
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setCodigoMotivo(Integer codigoMotivo) {
        this.codigoMotivo = codigoMotivo;
    }

    public Integer getCodigoMotivo() {
        return this.codigoMotivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() {
        return this.motivo;
    }

    public void setDataHoraAprovacao(LocalDateTime dataHoraAprovacao) {
        this.dataHoraAprovacao = dataHoraAprovacao;
    }

    public LocalDateTime getDataHoraAprovacao() {
        return this.dataHoraAprovacao;
    }

    public void setDataHoraRecusa(LocalDateTime dataHoraRecusa) {
        this.dataHoraRecusa = dataHoraRecusa;
    }

    public LocalDateTime getDataHoraRecusa() {
        return this.dataHoraRecusa;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return this.estado;
    }

    public void setCodigoRetorno(Integer codigoRetorno) {
        this.codigoRetorno = codigoRetorno;
    }

    public Integer getCodigoRetorno() {
        return this.codigoRetorno;
    }

    public void setMensagemRetorno(String mensagemRetorno) {
        this.mensagemRetorno = mensagemRetorno;
    }

    public String getMensagemRetorno() {
        return this.mensagemRetorno;
    }

    public void setDataHoraEvento(LocalDateTime dataHoraEvento) {
        this.dataHoraEvento = dataHoraEvento;
    }

    public LocalDateTime getDataHoraEvento() {
        return this.dataHoraEvento;
    }

    public void setXml(byte[] xml) {
        this.xml = xml;
    }

    public byte[] getXml() {
        return this.xml;
    }

    public void setPdf(byte[] pdf) {
        this.pdf = pdf;
    }

    public byte[] getPdf() {
        return this.pdf;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    public String getAmbiente() {
        return this.ambiente;
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

    public void setPreparadoPorUsuario(Usuario preparadoPorUsuario) {
        this.preparadoPorUsuario = preparadoPorUsuario;
    }

    public Usuario getPreparadoPorUsuario() {
        return this.preparadoPorUsuario;
    }

    public void setPreparadoPorCliente(ClienteAcesso preparadoPorCliente) {
        this.preparadoPorCliente = preparadoPorCliente;
    }

    public ClienteAcesso getPreparadoPorCliente() {
        return this.preparadoPorCliente;
    }

    public void setAprovadoPor(Usuario aprovadoPor) {
        this.aprovadoPor = aprovadoPor;
    }

    public Usuario getAprovadoPor() {
        return this.aprovadoPor;
    }

    public void setRecusadoPor(Usuario recusadoPor) {
        this.recusadoPor = recusadoPor;
    }

    public Usuario getRecusadoPor() {
        return this.recusadoPor;
    }

    public void setNota(Nota nota) {
        this.nota = nota;
    }

    public Nota getNota() {
        return this.nota;
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
            this.codigoMotivo != null || 
            this.motivo != null || 
            this.dataHoraAprovacao != null || 
            this.dataHoraRecusa != null || 
            this.estado != null || 
            this.codigoRetorno != null || 
            this.mensagemRetorno != null || 
            this.dataHoraEvento != null || 
            this.xml != null || 
            this.pdf != null || 
            this.ambiente != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.empresa != null || 
            this.preparadoPorUsuario != null || 
            this.preparadoPorCliente != null || 
            this.aprovadoPor != null || 
            this.recusadoPor != null || 
            this.nota != null;
    }

}
