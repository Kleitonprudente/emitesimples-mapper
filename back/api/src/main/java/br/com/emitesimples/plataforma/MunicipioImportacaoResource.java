package br.com.emitesimples.plataforma;

import br.com.emitesimples.plataforma.PlataformaDtos.ImportacaoResumo;
import br.com.emitesimples.security.ApiException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/** Importacao da tabela de municipios por upload. So administrador da plataforma (matriz em application.properties). */
@Path("/admin/municipios")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("PLATAFORMA_ADMIN")
public class MunicipioImportacaoResource {

    @Inject
    ImportacaoMunicipioService service;

    @Inject
    JsonWebToken jwt;

    /** Limite do arquivo em bytes (o do IBGE tem uns 300 KB). */
    @ConfigProperty(name = "emitesimples.municipios.max-bytes", defaultValue = "2097152")
    long maxBytes;

    @POST
    @Path("/importacao")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public ImportacaoResumo importar(@RestForm("arquivo") FileUpload arquivo) {
        if (arquivo == null) {
            throw new ApiException(400, "arquivo_obrigatorio", "Envie o arquivo no campo \"arquivo\" (multipart/form-data).");
        }
        if (arquivo.size() > maxBytes) {
            throw new ApiException(413, "arquivo_grande_demais",
                    "O arquivo passa do limite de " + (maxBytes / 1024) + " KB.");
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(arquivo.uploadedFile());
        } catch (IOException e) {
            throw new ApiException(400, "arquivo_invalido", "Não foi possível ler o arquivo enviado.");
        }
        return service.importar(jwt.getSubject(), arquivo.fileName(), bytes);
    }

    @GET
    @Path("/importacoes")
    public List<ImportacaoResumo> ultimas(@QueryParam("limite") @DefaultValue("20") int limite) {
        if (limite < 1 || limite > 100) {
            throw new ApiException(400, "requisicao_invalida", "limite deve ficar entre 1 e 100.");
        }
        return service.ultimas(limite);
    }
}
