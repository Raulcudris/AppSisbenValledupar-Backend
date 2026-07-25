package com.appsisben.backend.security;

public final class AppRolePreAuthorize {

    public static final String AUTHENTICATED =
            "isAuthenticated()";

    public static final String ADMIN =
            "hasAnyAuthority('ADMIN', 'ROLE_ADMIN') or hasRole('ADMIN')";

    public static final String ADMIN_OR_SUPERVISOR =
            "hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'SUPERVISOR', 'ROLE_SUPERVISOR') " +
                    "or hasAnyRole('ADMIN', 'SUPERVISOR')";

    public static final String ADMIN_SUPERVISOR_OR_CONSULTA =
            "hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'SUPERVISOR', 'ROLE_SUPERVISOR', 'CONSULTA', 'ROLE_CONSULTA') " +
                    "or hasAnyRole('ADMIN', 'SUPERVISOR', 'CONSULTA')";

    public static final String CATALOG_READ =
            "hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'SUPERVISOR', 'ROLE_SUPERVISOR', " +
                    "'FUNCIONARIO_VENTANILLA', 'ROLE_FUNCIONARIO_VENTANILLA', " +
                    "'FUNCIONARIO_DMC', 'ROLE_FUNCIONARIO_DMC', " +
                    "'FUNCIONARIO_CALLCENTER', 'ROLE_FUNCIONARIO_CALLCENTER', " +
                    "'CONSULTA', 'ROLE_CONSULTA') " +
                    "or hasAnyRole('ADMIN', 'SUPERVISOR', 'FUNCIONARIO_VENTANILLA', 'FUNCIONARIO_DMC', 'FUNCIONARIO_CALLCENTER', 'CONSULTA')";

    public static final String CATALOG_WRITE =
            ADMIN_OR_SUPERVISOR;

    public static final String TERRITORY_READ =
            CATALOG_READ;

    public static final String TERRITORY_WRITE =
            CATALOG_WRITE;

    public static final String VENTANILLA_READ =
            "hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'SUPERVISOR', 'ROLE_SUPERVISOR', " +
                    "'FUNCIONARIO_VENTANILLA', 'ROLE_FUNCIONARIO_VENTANILLA', 'CONSULTA', 'ROLE_CONSULTA') " +
                    "or hasAnyRole('ADMIN', 'SUPERVISOR', 'FUNCIONARIO_VENTANILLA', 'CONSULTA')";

    public static final String VENTANILLA_WRITE =
            "hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'SUPERVISOR', 'ROLE_SUPERVISOR', " +
                    "'FUNCIONARIO_VENTANILLA', 'ROLE_FUNCIONARIO_VENTANILLA') " +
                    "or hasAnyRole('ADMIN', 'SUPERVISOR', 'FUNCIONARIO_VENTANILLA')";

    public static final String DMC_READ =
            "hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'SUPERVISOR', 'ROLE_SUPERVISOR', " +
                    "'FUNCIONARIO_DMC', 'ROLE_FUNCIONARIO_DMC', 'CONSULTA', 'ROLE_CONSULTA') " +
                    "or hasAnyRole('ADMIN', 'SUPERVISOR', 'FUNCIONARIO_DMC', 'CONSULTA')";

    public static final String DMC_WRITE =
            "hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'SUPERVISOR', 'ROLE_SUPERVISOR', " +
                    "'FUNCIONARIO_DMC', 'ROLE_FUNCIONARIO_DMC') " +
                    "or hasAnyRole('ADMIN', 'SUPERVISOR', 'FUNCIONARIO_DMC')";

    public static final String CALLCENTER_READ =
            "hasAnyAuthority(" +
                    "'ADMIN', 'ROLE_ADMIN', " +
                    "'SUPERVISOR', 'ROLE_SUPERVISOR', " +
                    "'COORDINADOR_CALLCENTER', 'ROLE_COORDINADOR_CALLCENTER', " +
                    "'FUNCIONARIO_CALLCENTER', 'ROLE_FUNCIONARIO_CALLCENTER', " +
                    "'FUNCIONARIO_ENCUESTADOR', 'ROLE_FUNCIONARIO_ENCUESTADOR', " +
                    "'CONSULTA', 'ROLE_CONSULTA'" +
                    ") or hasAnyRole('ADMIN', 'SUPERVISOR', 'COORDINADOR_CALLCENTER', 'FUNCIONARIO_CALLCENTER', 'FUNCIONARIO_ENCUESTADOR', 'CONSULTA')";

    public static final String CALLCENTER_WRITE =
            "hasAnyAuthority(" +
                    "'ADMIN', 'ROLE_ADMIN', " +
                    "'SUPERVISOR', 'ROLE_SUPERVISOR', " +
                    "'COORDINADOR_CALLCENTER', 'ROLE_COORDINADOR_CALLCENTER', " +
                    "'FUNCIONARIO_CALLCENTER', 'ROLE_FUNCIONARIO_CALLCENTER'" +
                    ") or hasAnyRole('ADMIN', 'SUPERVISOR', 'COORDINADOR_CALLCENTER', 'FUNCIONARIO_CALLCENTER')";

    public static final String CALLCENTER_ASSIGN_FUNCIONARIO =
            "hasAnyAuthority(" +
                    "'ADMIN', 'ROLE_ADMIN', " +
                    "'SUPERVISOR', 'ROLE_SUPERVISOR', " +
                    "'COORDINADOR_CALLCENTER', 'ROLE_COORDINADOR_CALLCENTER'" +
                    ") or hasAnyRole('ADMIN', 'SUPERVISOR', 'COORDINADOR_CALLCENTER')";

    public static final String CALLCENTER_ASSIGN_ENCUESTADOR =
            "hasAnyAuthority(" +
                    "'ADMIN', 'ROLE_ADMIN', " +
                    "'SUPERVISOR', 'ROLE_SUPERVISOR', " +
                    "'COORDINADOR_CALLCENTER', 'ROLE_COORDINADOR_CALLCENTER', " +
                    "'FUNCIONARIO_CALLCENTER', 'ROLE_FUNCIONARIO_CALLCENTER'" +
                    ") or hasAnyRole('ADMIN', 'SUPERVISOR', 'COORDINADOR_CALLCENTER', 'FUNCIONARIO_CALLCENTER')";

    public static final String REPORT_READ =
            ADMIN_SUPERVISOR_OR_CONSULTA;

    public static final String EXPORT =
            ADMIN_OR_SUPERVISOR;

    public static final String AUDIT_READ =
            ADMIN_OR_SUPERVISOR;

    private AppRolePreAuthorize() {
    }
}
