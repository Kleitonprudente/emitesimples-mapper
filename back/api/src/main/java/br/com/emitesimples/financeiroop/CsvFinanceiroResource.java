package br.com.emitesimples.financeiroop;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/**
 * CSV do financeiro (docs/financeiro/README.md, F4-C3 e F4-C4). Exportar: qualquer papel da empresa. Importar contas, categorias e condicoes:
 * so ADMINISTRADOR (critico). Bancos do Banco Central: so o administrador da plataforma. As regras de caminho do application.properties
 * valem primeiro; {@code @RolesAllowed} e o cinto extra.
 */
@Path("/financeiro/csv")
public class CsvFinanceiroResource {

    @Inject
    CsvFinanceiro csv;

    @POST
    @Path("/bancos")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("PLATAFORMA_ADMIN")
    public CsvFinanceiro.ResultadoDosBancos importarBancos(@RestForm("arquivo") FileUpload arquivo) {
        return csv.importarBancos(texto(arquivo));
    }

    @GET
    @Path("/{entidade}")
    @Produces("text/csv")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA"})
    public Response exportar(@PathParam("entidade") String entidade) {
        return Response.ok(csv.exportar(entidade), "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"" + entidade + ".csv\"").build();
    }

    @POST
    @Path("/{entidade}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public CsvFinanceiro.ResultadoDaImportacao importar(@PathParam("entidade") String entidade, @RestForm("arquivo") FileUpload arquivo) {
        return csv.importar(entidade, texto(arquivo));
    }

    /** O arquivo enviado (campo `arquivo` do multipart, como o certificado e a importacao de municipios), em UTF-8, ate 1 MB. */
    private static String texto(FileUpload arquivo) {
        if (arquivo == null || arquivo.uploadedFile() == null) {
            throw CsvFinanceiro.invalido("Envie o arquivo CSV.");
        }
        if (arquivo.size() > CsvFinanceiro.MAX_BYTES) {
            throw CsvFinanceiro.invalido("O arquivo passa de 1 MB.");
        }
        try {
            return Files.readString(arquivo.uploadedFile(), StandardCharsets.UTF_8);
        } catch (IOException | java.io.UncheckedIOException e) {
            throw CsvFinanceiro.invalido("Não foi possível ler o arquivo: salve-o como CSV em UTF-8.");
        }
    }
}
