package br.com.emitesimples.security;

import java.util.Set;

/**
 * Papeis do sistema (escrito a mao). Contrato do mapa (VinculoUsuario.papel, secao 9.7 do guia de modelagem): valor tecnico
 * em MAIUSCULAS, o mesmo que vai na claim {@code groups} do JWT e nos {@code @RolesAllowed}/roles-allowed do
 * {@code application.properties}. {@link #PLATAFORMA_ADMIN} e o grupo do administrador da plataforma (nao e papel de vinculo);
 * a claim booleana {@code plataforma_admin} do JWT (lida pelo EscopoTenant/RLS) e outra coisa e continua com esse nome.
 */
public final class Papeis {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String OPERADOR = "OPERADOR";
    public static final String CONSULTA = "CONSULTA";
    public static final String PLATAFORMA_ADMIN = "PLATAFORMA_ADMIN";

    /** Papeis validos de um vinculo usuario/empresa. */
    public static final Set<String> DE_VINCULO = Set.of(ADMINISTRADOR, OPERADOR, CONSULTA);

    private Papeis() {
    }
}
