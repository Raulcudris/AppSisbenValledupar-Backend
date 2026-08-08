package com.appsisben.backend.security;

/**
 * Expresiones centralizadas de autorización utilizadas
 * por los controladores y servicios del sistema.
 */
public final class AppRolePreAuthorize {

        public static final String AUTHENTICATED =
                "isAuthenticated()";

        public static final String ADMIN =
                "hasRole('ADMIN')";

        public static final String ADMIN_OR_SUPERVISOR =
                "hasAnyRole('ADMIN', 'SUPERVISOR')";

        public static final String
                ADMIN_SUPERVISOR_OR_CONSULTA =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'SUPERVISOR', " +
                        "'CONSULTA'" +
                        ")";

        public static final String CATALOG_READ =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'SUPERVISOR', " +
                        "'FUNCIONARIO_VENTANILLA', " +
                        "'FUNCIONARIO_DMC', " +
                        "'FUNCIONARIO_CALLCENTER', " +
                        "'CONSULTA'" +
                        ")";

        /**
         * Lectura específica del catálogo de encuestadores.
         */
        public static final String
                ENCUESTADOR_CATALOG_READ =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'SUPERVISOR', " +
                        "'COORDINADOR_CALLCENTER', " +
                        "'FUNCIONARIO_VENTANILLA', " +
                        "'FUNCIONARIO_DMC', " +
                        "'FUNCIONARIO_CALLCENTER', " +
                        "'CONSULTA'" +
                        ")";

        public static final String CATALOG_WRITE =
                ADMIN_OR_SUPERVISOR;

        public static final String TERRITORY_READ =
                CATALOG_READ;

        public static final String TERRITORY_WRITE =
                CATALOG_WRITE;

        /**
         * Lectura general del módulo Ventanilla.
         *
         * FUNCIONARIO_CALLCENTER no se incluye aquí para
         * evitar otorgarle acceso general al módulo.
         */
        public static final String VENTANILLA_READ =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'SUPERVISOR', " +
                        "'FUNCIONARIO_VENTANILLA', " +
                        "'CONSULTA'" +
                        ")";

        /**
         * Consulta de registros de Ventanilla.
         *
         * Incluye FUNCIONARIO_CALLCENTER únicamente para
         * permitir la búsqueda necesaria durante la captura
         * y precarga de información en Call Center.
         */
        public static final String VENTANILLA_SEARCH_READ =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'SUPERVISOR', " +
                        "'FUNCIONARIO_VENTANILLA', " +
                        "'FUNCIONARIO_CALLCENTER', " +
                        "'CONSULTA'" +
                        ")";

        public static final String VENTANILLA_WRITE =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'SUPERVISOR', " +
                        "'FUNCIONARIO_VENTANILLA'" +
                        ")";

        public static final String DMC_READ =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'SUPERVISOR', " +
                        "'FUNCIONARIO_DMC', " +
                        "'CONSULTA'" +
                        ")";

        public static final String DMC_WRITE =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'SUPERVISOR', " +
                        "'FUNCIONARIO_DMC'" +
                        ")";

        /**
         * Lectura general de casos, catálogos,
         * llamadas y visitas de Call Center.
         */
        public static final String CALLCENTER_READ =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'SUPERVISOR', " +
                        "'COORDINADOR_CALLCENTER', " +
                        "'FUNCIONARIO_CALLCENTER', " +
                        "'FUNCIONARIO_ENCUESTADOR', " +
                        "'CONSULTA'" +
                        ")";

        /**
         * Lectura administrativa de agenda de visitas.
         */
        public static final String
                CALLCENTER_AGENDA_VISITAS_READ =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'COORDINADOR_CALLCENTER', " +
                        "'FUNCIONARIO_CALLCENTER'" +
                        ")";

        /**
         * Escritura general del módulo Call Center.
         */
        public static final String CALLCENTER_WRITE =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'SUPERVISOR', " +
                        "'COORDINADOR_CALLCENTER', " +
                        "'FUNCIONARIO_CALLCENTER'" +
                        ")";

        /**
         * Registro completo de un caso propio.
         */
        public static final String
                CALLCENTER_SELF_REGISTER =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'FUNCIONARIO_CALLCENTER'" +
                        ")";

        /**
         * Asignación administrativa de casos a funcionarios.
         */
        public static final String
                CALLCENTER_ASSIGN_FUNCIONARIO =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'SUPERVISOR', " +
                        "'COORDINADOR_CALLCENTER'" +
                        ")";

        /**
         * Asignación de encuestadores o visitas.
         */
        public static final String
                CALLCENTER_ASSIGN_ENCUESTADOR =
                CALLCENTER_WRITE;

        /**
         * Consulta de asignaciones personales.
         */
        public static final String
                CALLCENTER_ASSIGNMENTS_READ =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'SUPERVISOR', " +
                        "'FUNCIONARIO_CALLCENTER', " +
                        "'FUNCIONARIO_ENCUESTADOR'" +
                        ")";

        /**
         * Actualización formal del resultado de una visita.
         */
        public static final String
                CALLCENTER_VISIT_UPDATE =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'SUPERVISOR', " +
                        "'FUNCIONARIO_ENCUESTADOR'" +
                        ")";

        /**
         * Compatibilidad con el endpoint legacy que actualiza
         * directamente los campos de visita del caso maestro.
         */
        public static final String
                CALLCENTER_LEGACY_VISIT_UPDATE =
                "hasAnyRole(" +
                        "'ADMIN', " +
                        "'SUPERVISOR', " +
                        "'FUNCIONARIO_CALLCENTER', " +
                        "'FUNCIONARIO_ENCUESTADOR'" +
                        ")";

        public static final String REPORT_READ =
                ADMIN_SUPERVISOR_OR_CONSULTA;

        public static final String EXPORT =
                ADMIN_OR_SUPERVISOR;

        public static final String AUDIT_READ =
                ADMIN_OR_SUPERVISOR;

        private AppRolePreAuthorize() {
        }
}