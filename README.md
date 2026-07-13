# AppSisbenValledupar Backend Actualizado

Backend modular para la nueva implementación web de **AppSisbenValledupar**, construido con Java 17, Spring Boot 3 y MySQL.

## 1. Stack

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- MySQL
- Lombok
- Swagger / OpenAPI

## 2. Base de datos esperada

```text
app_sisben_web
```

Primero debes haber ejecutado:

```text
01_base_datos_mysql_actualizada_app_sisben.sql
02_cargue_datos_mysql_desde_sqlite.sql
```

## 3. Configuración local

Editar:

```text
src/main/resources/application-dev.yml
```

Cambiar usuario y contraseña de MySQL:

```yaml
spring:
  datasource:
    username: root
    password: root
```

## 4. Ejecutar

```bash
mvn clean spring-boot:run
```

## 5. Endpoints iniciales

```text
GET  /api/health
POST /api/auth/login
GET  /api/auth/me
PUT  /api/auth/change-password
GET  /api/reports/migration/counts
GET  /api/territory/summary
GET  /api/catalogs/summary
GET  /api/users
GET  /api/roles
```

## 6. Swagger

```text
http://localhost:6095/swagger-ui.html
```

## 7. Nota sobre contraseñas migradas

El script de migración dejó un valor temporal en `usuario.password_hash`.

Antes de iniciar sesión debes actualizar el hash del usuario administrador con un BCrypt válido.

Ejemplo:

```sql
UPDATE usuario
SET password_hash = '$2a$10$REEMPLAZAR_POR_HASH_REAL'
WHERE username = 'ADMIN';
```

## 8. Estructura modular

```text
config
security
shared
modules
  auth
  roles
  users
  territory
  catalogs
  ventanilla
  dmc
  directory
  reports
  audit
```

## 9. Buenas prácticas aplicadas

- Separación por módulos funcionales.
- Entidades JPA separadas de DTOs.
- Respuesta estándar `ApiResponse`.
- Manejo global de excepciones.
- Seguridad preparada con JWT.
- Configuración por perfiles.
- Repositorios JPA por dominio.
- Endpoints iniciales para validar migración.
- Base lista para extender nuevos módulos.
