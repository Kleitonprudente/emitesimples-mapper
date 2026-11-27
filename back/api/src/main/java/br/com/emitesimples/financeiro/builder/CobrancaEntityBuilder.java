package br.com.emitesimples.financeiro.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.Cobranca;
import br.com.emitesimples.financeiro.entity.CobrancaEntity;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.math.BigDecimal;
import br.com.emitesimples.financeiro.entity.TituloEntity;
import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * Cobranca entity builder
*/
public class CobrancaEntityBuilder {
    public LocalDateTime version;
    public String provedor;
    public String tipo;
    public String estado;
    public BigDecimal valor;
    public LocalDate vencimento;
    public BigDecimal multaPercentual;
    public BigDecimal jurosMensalPercentual;
    public String idExterno;
    public String linhaDigitavel;
    public String codigoDeBarras;
    public String urlDoBoleto;
    public String pixCopiaECola;
    public String urlDoPix;
    public String urlDoLink;
    public LocalDateTime pagoEm;
    public BigDecimal valorPago;
    public String meioPago;
    public BigDecimal tarifa;
    public String idBaixaMovimento;
    public String idTarifaMovimento;
    public String motivo;
    public String respostaDoProvedor;
    public LocalDateTime ultimaConsultaEm;
    public String idCriadoPor;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public TituloEntity titulo;
    public ContaFinanceiraEntity conta;
    public EmpresaEntity empresa;

    public CobrancaEntityBuilder() {
    }
    public CobrancaEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public CobrancaEntityBuilder withProvedor(String provedor) {
        this.provedor = provedor;
        return this;
    }

    public CobrancaEntityBuilder withTipo(String tipo) {
        this.tipo = tipo;
        return this;
    }

    public CobrancaEntityBuilder withEstado(String estado) {
        this.estado = estado;
        return this;
    }
    public CobrancaEntityBuilder withValor(BigDecimal valor) {
        this.valor = valor;
        return this;
    }

    public CobrancaEntityBuilder withVencimento(LocalDate vencimento) {
        this.vencimento = vencimento;
        return this;
    }
    public CobrancaEntityBuilder withMultaPercentual(BigDecimal multaPercentual) {
        this.multaPercentual = multaPercentual;
        return this;
    }
    public CobrancaEntityBuilder withJurosMensalPercentual(BigDecimal jurosMensalPercentual) {
        this.jurosMensalPercentual = jurosMensalPercentual;
        return this;
    }

    public CobrancaEntityBuilder withIdExterno(String idExterno) {
        this.idExterno = idExterno;
        return this;
    }

    public CobrancaEntityBuilder withLinhaDigitavel(String linhaDigitavel) {
        this.linhaDigitavel = linhaDigitavel;
        return this;
    }

    public CobrancaEntityBuilder withCodigoDeBarras(String codigoDeBarras) {
        this.codigoDeBarras = codigoDeBarras;
        return this;
    }

    public CobrancaEntityBuilder withUrlDoBoleto(String urlDoBoleto) {
        this.urlDoBoleto = urlDoBoleto;
        return this;
    }

    public CobrancaEntityBuilder withPixCopiaECola(String pixCopiaECola) {
        this.pixCopiaECola = pixCopiaECola;
        return this;
    }

    public CobrancaEntityBuilder withUrlDoPix(String urlDoPix) {
        this.urlDoPix = urlDoPix;
        return this;
    }

    public CobrancaEntityBuilder withUrlDoLink(String urlDoLink) {
        this.urlDoLink = urlDoLink;
        return this;
    }

    public CobrancaEntityBuilder withPagoEm(LocalDateTime pagoEm) {
        this.pagoEm = pagoEm;
        return this;
    }
    public CobrancaEntityBuilder withValorPago(BigDecimal valorPago) {
        this.valorPago = valorPago;
        return this;
    }

    public CobrancaEntityBuilder withMeioPago(String meioPago) {
        this.meioPago = meioPago;
        return this;
    }
    public CobrancaEntityBuilder withTarifa(BigDecimal tarifa) {
        this.tarifa = tarifa;
        return this;
    }

    public CobrancaEntityBuilder withIdBaixaMovimento(String idBaixaMovimento) {
        this.idBaixaMovimento = idBaixaMovimento;
        return this;
    }

    public CobrancaEntityBuilder withIdTarifaMovimento(String idTarifaMovimento) {
        this.idTarifaMovimento = idTarifaMovimento;
        return this;
    }

    public CobrancaEntityBuilder withMotivo(String motivo) {
        this.motivo = motivo;
        return this;
    }

    public CobrancaEntityBuilder withRespostaDoProvedor(String respostaDoProvedor) {
        this.respostaDoProvedor = respostaDoProvedor;
        return this;
    }

    public CobrancaEntityBuilder withUltimaConsultaEm(LocalDateTime ultimaConsultaEm) {
        this.ultimaConsultaEm = ultimaConsultaEm;
        return this;
    }

    public CobrancaEntityBuilder withIdCriadoPor(String idCriadoPor) {
        this.idCriadoPor = idCriadoPor;
        return this;
    }

    public CobrancaEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public CobrancaEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public CobrancaEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public CobrancaEntityBuilder withTitulo(TituloEntity titulo) {
        this.titulo = titulo;
        return this;
    }

    public CobrancaEntityBuilder withConta(ContaFinanceiraEntity conta) {
        this.conta = conta;
        return this;
    }

    public CobrancaEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public CobrancaEntity build() {
        return new CobrancaEntity(this);
    }

    public CobrancaEntityBuilder withModel(Cobranca cobranca) {
        return this.withVersion(cobranca.getVersion())
                .withProvedor(cobranca.getProvedor())
                .withTipo(cobranca.getTipo())
                .withEstado(cobranca.getEstado())
                .withValor(cobranca.getValor())
                .withVencimento(cobranca.getVencimento())
                .withMultaPercentual(cobranca.getMultaPercentual())
                .withJurosMensalPercentual(cobranca.getJurosMensalPercentual())
                .withIdExterno(cobranca.getIdExterno())
                .withLinhaDigitavel(cobranca.getLinhaDigitavel())
                .withCodigoDeBarras(cobranca.getCodigoDeBarras())
                .withUrlDoBoleto(cobranca.getUrlDoBoleto())
                .withPixCopiaECola(cobranca.getPixCopiaECola())
                .withUrlDoPix(cobranca.getUrlDoPix())
                .withUrlDoLink(cobranca.getUrlDoLink())
                .withPagoEm(cobranca.getPagoEm())
                .withValorPago(cobranca.getValorPago())
                .withMeioPago(cobranca.getMeioPago())
                .withTarifa(cobranca.getTarifa())
                .withIdBaixaMovimento(cobranca.getIdBaixaMovimento())
                .withIdTarifaMovimento(cobranca.getIdTarifaMovimento())
                .withMotivo(cobranca.getMotivo())
                .withRespostaDoProvedor(cobranca.getRespostaDoProvedor())
                .withUltimaConsultaEm(cobranca.getUltimaConsultaEm())
                .withIdCriadoPor(cobranca.getIdCriadoPor())
                .withId(cobranca.getId())
                .withDataCriacao(cobranca.getDataCriacao())
                .withDataAtualizacao(cobranca.getDataAtualizacao())
                .withTitulo(TituloEntity.builderOf().withModel(cobranca.getTitulo()).build())
                .withConta(ContaFinanceiraEntity.builderOf().withModel(cobranca.getConta()).build())
                .withEmpresa(EmpresaEntity.builderOf().withModel(cobranca.getEmpresa()).build());
    }

    public List<CobrancaEntity> withModelList(List<Cobranca> cobrancaList) {
        if (cobrancaList == null) {
            return null;
        }

        List<CobrancaEntity> list = new ArrayList<CobrancaEntity>(cobrancaList.size());
        for (Cobranca cobranca: cobrancaList) {
           list.add(withModel(cobranca).build());
        }
        return list;
    }

}
