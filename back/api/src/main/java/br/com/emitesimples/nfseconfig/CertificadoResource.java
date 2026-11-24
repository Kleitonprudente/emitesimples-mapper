package br.com.emitesimples.nfseconfig;

import br.com.emitesimples.nfseconfig.CertificadoService.CertificadoResposta;
import br.com.emitesimples.security.ApiException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.Files;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/**
 * {@code POST /api/configuracao/empresa/certificado} (multipart: {@code arquivo} = .pfx/.p12, {@code senha}): so o administrador da
 * empresa. O corpo multipart nao e logado. O arquivo temporario do upload e apagado pelo framework ao fim da requisicao.
 */
@Path("/configuracao/empresa/certificado")
@Produces(MediaType.APPLICATION_JSON)
public class CertificadoResource {

    @Inject
    CertificadoService service;

    @Inject
    JsonWebToken jwt;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed("ADMINISTRADOR")
    public CertificadoResposta enviar(@RestForm("arquivo") FileUpload arquivo, @RestForm("senha") String senha) {
        if (arquivo == null) {
            return service.enviar(jwt.getSubject(), null, -1, null, senha);
        }
        // o arquivo so e lido para a memoria depois de o service conferir o tamanho
        return service.enviar(jwt.getSubject(), arquivo.fileName(), arquivo.size(), () -> {
            try {
                return Files.readAllBytes(arquivo.uploadedFile());
            } catch (IOException e) {
                throw new ApiException(400, "arquivo_invalido", "Não foi possível ler o arquivo enviado.");
            }
        }, senha);
    }
}
