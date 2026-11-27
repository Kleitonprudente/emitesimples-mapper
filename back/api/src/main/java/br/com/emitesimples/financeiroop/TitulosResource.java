package br.com.emitesimples.financeiroop;

import br.com.emitesimples.financeiroop.TitulosService.AtualizarTitulo;
import br.com.emitesimples.financeiroop.TitulosService.CriarTitulo;
import br.com.emitesimples.financeiroop.TitulosService.Filtro;
import br.com.emitesimples.financeiroop.TitulosService.Pagina;
import br.com.emitesimples.financeiroop.TitulosService.Resumo;
import br.com.emitesimples.financeiroop.TitulosService.Sugestoes;
import br.com.emitesimples.financeiroop.TitulosService.TituloDto;
import br.com.emitesimples.security.ApiException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Titulos a receber e a pagar (docs/financeiro/README.md, secao 3; o Titulo NAO tem CRUD gerado). Papeis (decisao B2): ler, qualquer papel
 * da empresa; lancar e editar, ADMINISTRADOR ou OPERADOR; cancelar e aprovar, so ADMINISTRADOR. As regras de caminho do
 * application.properties valem primeiro; {@code @RolesAllowed} e o cinto extra.
 */
@Path("/financeiro/titulos")
@Produces(MediaType.APPLICATION_JSON)
public class TitulosResource {

    @Inject
    TitulosService titulos;

    @Inject
    CobrancaPorEmail cobrancaPorEmail;

    @Inject
    CobrancasService cobrancas;

    /** Fase 7 (README 9.5): gerar a cobranca no gateway (boleto, Pix ou link). Critico: so ADMINISTRADOR. */
    @POST
    @Path("/{id}/cobrancas")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public CobrancasService.CobrancaDto gerarCobranca(@PathParam("id") String id, CobrancasService.CriarCobranca corpo) {
        return cobrancas.criar(id, corpo);
    }

    @GET
    @Path("/{id}/cobrancas")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA"})
    public List<CobrancasService.CobrancaDto> cobrancasDoTitulo(@PathParam("id") String id) {
        return cobrancas.doTitulo(id);
    }

    /** Fase 6 (README 9.3): manda a cobranca por e-mail ao cliente. Critico (mensagem a terceiro): so ADMINISTRADOR. */
    @POST
    @Path("/{id}/enviar-cobranca")
    @RolesAllowed("ADMINISTRADOR")
    public CobrancaPorEmail.Enviada enviarCobranca(@PathParam("id") String id) {
        return cobrancaPorEmail.enviar(id);
    }

    /** Cobrancas por e-mail ja mandadas para o titulo (manuais e lembretes), da mais recente para a mais antiga. */
    @GET
    @Path("/{id}/cobrancas-enviadas")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA"})
    public List<CobrancaPorEmail.Enviada> cobrancasEnviadas(@PathParam("id") String id) {
        return cobrancaPorEmail.enviadas(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "AGENTE"})
    public List<TituloDto> criar(CriarTitulo corpo) {
        return titulos.criar(corpo);
    }

    @GET
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA", "AGENTE"})
    public Pagina listar(@QueryParam("tipo") String tipo, @QueryParam("filtro") String filtro, @QueryParam("de") String de,
            @QueryParam("ate") String ate, @QueryParam("idPessoa") String idPessoa, @QueryParam("idCategoria") String idCategoria,
            @QueryParam("idLancamento") String idLancamento, @QueryParam("busca") String busca, @QueryParam("pagina") Integer pagina,
            @QueryParam("tamanho") Integer tamanho) {
        return titulos.listar(new Filtro(tipo, filtro, data("de", de), data("ate", ate), idPessoa, idCategoria, idLancamento, busca, pagina,
                tamanho));
    }

    @GET
    @Path("/sugestoes")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA", "AGENTE"})
    public Sugestoes sugestoes(@QueryParam("tipo") String tipo) {
        return titulos.sugestoes(tipo);
    }

    @GET
    @Path("/resumo")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA"})
    public Resumo resumo() {
        return titulos.resumo();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA", "AGENTE"})
    public TituloDto buscar(@PathParam("id") String id) {
        return titulos.buscar(id);
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR"})
    public TituloDto atualizar(@PathParam("id") String id, AtualizarTitulo corpo) {
        return titulos.atualizar(id, corpo);
    }

    @POST
    @Path("/{id}/cancelar")
    @RolesAllowed("ADMINISTRADOR")
    public TituloDto cancelar(@PathParam("id") String id) {
        return titulos.cancelar(id);
    }

    @POST
    @Path("/{id}/aprovar")
    @RolesAllowed("ADMINISTRADOR")
    public TituloDto aprovar(@PathParam("id") String id) {
        return titulos.aprovar(id);
    }

    // ---------------------------------------------------------------------------------------------- Fase 3: baixa (pelo LivroDeMovimentos)

    @Inject
    LivroDeMovimentos livro;

    @POST
    @Path("/{id}/baixar")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR"})
    public LivroDeMovimentos.ResultadoDaBaixa baixar(@PathParam("id") String id, LivroDeMovimentos.Baixa corpo) {
        return livro.baixar(id, corpo);
    }

    @GET
    @Path("/{id}/sugestao-baixa")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA"})
    public LivroDeMovimentos.SugestaoDeBaixa sugestaoDeBaixa(@PathParam("id") String id, @QueryParam("data") String data,
            @QueryParam("quitado") java.math.BigDecimal quitado) {
        return livro.sugestao(id, data("data", data), quitado);
    }

    @GET
    @Path("/{id}/baixas")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA"})
    public List<LivroDeMovimentos.LinhaDoTitulo> baixas(@PathParam("id") String id) {
        return livro.baixasDoTitulo(id);
    }

    @POST
    @Path("/baixar-em-lote")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR"})
    public List<LivroDeMovimentos.ResultadoDaBaixa> baixarEmLote(LivroDeMovimentos.BaixaEmLote corpo) {
        return livro.baixarEmLote(corpo);
    }

    static LocalDate data(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            Map<String, String> m = Map.of("campo", campo, "mensagem", "Data inválida. Use o formato AAAA-MM-DD.");
            throw new ApiException(400, "requisicao_invalida", m.get("mensagem"), Map.of("campos", List.of(m)));
        }
    }
}
