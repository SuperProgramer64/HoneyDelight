# Publicaciones y app opcional

HoneyDelight Updates consulta la lista pública de releases de `SuperProgramer64/HoneyDelight`, incluidas prereleases. No usa autenticación ni contiene tokens. El repositorio está privado durante la preparación.

Los ZIP deben llevar un identificador de compatibilidad al final del nombre:

```text
HoneyDelight-0.1.0-alpha-SM-S936B-<compatibility-id>.zip
```

El identificador es los primeros 12 caracteres hex de SHA-256 de `fingerprint + "\n" + sha256(APK Samsung)`. La comparación se hace localmente; esos valores no se transmiten para consultar las releases. `scripts/build-module.py --output-dir dist` genera el nombre correspondiente.

La API de GitHub debe devolver el digest SHA-256 del asset. Si no existe un digest válido, la app rechaza la descarga automática. Solo acepta HTTPS hacia GitHub y su CDN de releases. El límite de descarga es 256 MiB. Primero descarga a caché privada, verifica y después guarda en Descargas/HoneyDelight.

La tarea periódica usa JobScheduler, cada seis horas aproximadamente y con red sin medición. Android puede diferirla por batería o restricciones. No se instala ni se reinicia automáticamente. Las notificaciones necesitan permiso del usuario.

No se ha validado aún una descarga de extremo a extremo desde una release real; hace falta la primera publicación de prueba.
