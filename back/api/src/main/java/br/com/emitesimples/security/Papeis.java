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
    /**
     * Grupo do token de AGENTE (cliente de acesso; docs/financeiro/README.md, 9.4, B3). Nunca e papel de vinculo: o agente so le
     * (escopo consultar) e prepara rascunhos (escopo preparar), e o security/AgenteFiltro barra todo o resto.
     */
    public static final String AGENTE = "AGENTE";

    /** Papeis validos de um vinculo usuario/empresa. */
    public static final Set<String> DE_VINCULO = Set.of(ADMINISTRADOR, OPERADOR, CONSULTA);

    private Papeis() {
    }
}
