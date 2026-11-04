# HoneyDelight 0.1.0-alpha · experimental

**Tu Samsung Keyboard, con un toque de Delight.**

Conecta Delight5 con Samsung Keyboard y amplía las herramientas de Galaxy AI. Esta alpha incluye mejoras de contexto, separación de palabras pegadas, filtros contra sugerencias repetidas y una reparación de las esperas del editor de reescritura.

## Paquetes

- **Módulo ZIP**: entrega principal para el Galaxy S25+ SM-S936B de desarrollo, Android 16 / One UI 8.5 y Samsung Keyboard 5.9.30.97. Valida compatibilidad e integridad y conserva el APK firmado por Samsung.
- **HoneyDelight Updates 0.1.0-alpha.2** (`versionCode=3`): complemento opcional, con interfaz ámbar renovada e iconos adaptativos. Consulta releases públicas, avisa y descarga por redes sin medición; mantiene la firma de la app anterior. La actualización sobre la app instalada y la pantalla principal se comprobaron en el teléfono de desarrollo. No requiere root ni lee aprendizaje del teclado.
- **Keyboard APK experimental**: paquete completo con el mod, biblioteca nativa y modelos base. Usa firma propia y no actualiza directamente el teclado oficial. Instalación, permisos de firma y Galaxy AI sin validar; véase [FULL_APK.md](FULL_APK.md).

## Requisitos e instalación

Referencia: **Galaxy S25+ SM-S936B · Android 16 / One UI 8.5 · Samsung Keyboard 5.9.30.97 · arm64-v8a**, compilación `S936BXXSBCZG3_OWOBCZG3`, compatibilidad `1a9a24823d75`. El entorno de desarrollo usa KernelSU 3.3.0. Magisk está contemplado por el formato, sin versión validada.

El **ZIP incluye el mod completo, Delight5 y los modelos base**. Requiere root y Samsung Keyboard compatible ya instalado; no instala ese teclado desde cero ni requiere Gboard. Instálalo desde el gestor root conservando otro teclado disponible. El APK Updates es opcional; el APK Keyboard experimental no sustituye este requisito. [Guía y recuperación](https://github.com/SuperProgramer64/HoneyDelight/blob/main/docs/INSTALLATION.md).

## Cambios comprobados

- Inicio con el directorio del perfil actual y reintentos tras fallos temporales.
- Contexto actualizado al cambiar de frase o campo; descarte de resultados atrasados y exclusión temprana de contraseñas.
- Filtros contra expansiones repetidas y alternativas pegadas cuando existe una separación aceptada.
- Conservación de cortes válidos al agotarse el presupuesto opcional y revisión final al pulsar espacio en Google.
- Tabla local de 16 expresiones frecuentes y separador general con corrección conservadora de una letra en la segunda palabra.
- Composición corregida excluida del contexto: `holacomo` y `holaxomo` muestran y confirman `hola cómo`; retroceso y reescritura conservan la separación sin las concatenaciones reproducidas.

## Pruebas y límites

Pasaron **19 grupos JVM**. La última repetición nativa obtuvo **90/92**, frente a 92/92 en una anterior, con modelos base: 23 entradas, dos repeticiones y dos variantes de coordenadas. En Google se comprobaron seis expresiones frecuentes en la barra y al pulsar espacio, además de los controles y el ciclo de retroceso anteriores. En la repetición más reciente, `holaxomo` y `holacomo` fallaron una vez cada una con coordenadas y presupuesto de 55 ms; queda documentada esa intermitencia. Estas muestras no son una cifra de precisión global ni una comparación general con Gboard.

La comparación ampliada anterior obtuvo 82/84 decisiones esperadas: sigue abierto el caso bilingüe `erroes → errors` con coordenadas simuladas. La activación del módulo desde el gestor, el primer arranque, la recuperación y una descarga pública de extremo a extremo aún requieren prueba. La preparación local de ART del ZIP pasó en una carpeta de prueba del dispositivo.

El prototipo retirado del menú Corregir no está incluido. Los paquetes excluyen historiales entrenados, contactos, correos, instrucciones personales, logs, claves y contraseñas del desarrollador. El aprendizaje de cada instalación se mantiene local.

## Licencia

Código original bajo MIT, © 2026 SuperProgramer64. Los componentes externos y elementos derivados conservan sus propios derechos; véanse `LICENSE` y `THIRD_PARTY_NOTICES.md`. La alpha se publica para experimentación; la instalación y el primer arranque del módulo aún no están validados. La MIT no concede derechos sobre los componentes externos.
