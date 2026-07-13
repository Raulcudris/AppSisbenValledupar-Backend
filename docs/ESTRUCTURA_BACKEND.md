# Estructura del backend

## Paquetes principales

```text
com.appsisben.backend
├── config
├── security
├── shared
│   ├── api
│   ├── exception
│   └── persistence
└── modules
    ├── auth
    ├── roles
    ├── users
    ├── territory
    ├── catalogs
    ├── ventanilla
    ├── dmc
    ├── directory
    ├── reports
    └── audit
```

## Regla de crecimiento

Cada nuevo módulo debe tener:

```text
api
application
domain
dto
repository
validation
```

## Ejemplo

Para agregar PQRS:

```text
modules/pqrs
├── api
├── application
├── domain
├── dto
├── repository
└── validation
```
