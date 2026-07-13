USE app_sisben_web;

SELECT 'rol' AS tabla, COUNT(*) AS total FROM rol
UNION ALL SELECT 'usuario', COUNT(*) FROM usuario
UNION ALL SELECT 'comuna', COUNT(*) FROM comuna
UNION ALL SELECT 'barrio', COUNT(*) FROM barrio
UNION ALL SELECT 'categoria', COUNT(*) FROM categoria
UNION ALL SELECT 'solicitud', COUNT(*) FROM solicitud
UNION ALL SELECT 'estado_solicitud', COUNT(*) FROM estado_solicitud
UNION ALL SELECT 'encuestador', COUNT(*) FROM encuestador
UNION ALL SELECT 'tipo_dmc', COUNT(*) FROM tipo_dmc
UNION ALL SELECT 'directorio_contacto', COUNT(*) FROM directorio_contacto
UNION ALL SELECT 'ventanilla_registro', COUNT(*) FROM ventanilla_registro
UNION ALL SELECT 'dmc_registro', COUNT(*) FROM dmc_registro;
