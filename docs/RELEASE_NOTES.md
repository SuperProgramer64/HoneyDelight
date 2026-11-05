[English](#english) · [Español](#espanol)

<a id="english"></a>

# HoneyDelight 0.1.0-alpha · experimental

**Your Samsung Keyboard, with a touch of Delight.**

Connects Delight5 to Samsung Keyboard and expands the Galaxy AI writing tools. This alpha improves context handling, joined-word splitting, repeated-suggestion filtering and reproduced waits in the rewrite editor.

## Packages

- **Module ZIP:** the main package for the development Galaxy S25+ SM-S936B, Android 16 / One UI 8.5 and Samsung Keyboard 5.9.30.97. Checks compatibility and integrity while retaining Samsung's signed APK.
- **HoneyDelight Updates 0.1.0-alpha.2** (`versionCode=3`): an optional companion with a refreshed amber interface and adaptive icons. Checks public releases, provides notifications and downloads over unmetered networks, using the same signing certificate as the earlier app. Updating the installed app and its main screen were checked on the development phone. Does not require root or read the keyboard's learned data.
- **Experimental Keyboard APK:** a complete package with the mod, native library and base models. Signed with a project key; cannot directly update Samsung's official keyboard. Installation, signature permissions and Galaxy AI remain unvalidated; see [FULL_APK.md](FULL_APK.md).

## Requirements and installation

Reference: **Galaxy S25+ SM-S936B · Android 16 / One UI 8.5 · Samsung Keyboard 5.9.30.97 · arm64-v8a**, firmware `S936BXXSBCZG3_OWOBCZG3`, compatibility ID `1a9a24823d75`. The development environment uses KernelSU 3.3.0. The packaging supports a Magisk installation route, but no Magisk version has been validated.

The **ZIP includes the complete mod, Delight5 and base models**. Requires root and an existing compatible Samsung Keyboard installation; does not install the keyboard from scratch or require Gboard. Install through the root manager and keep another keyboard available. The Updates APK is optional; the experimental Keyboard APK does not remove the base-keyboard requirement. [Installation and recovery guide — Spanish](https://github.com/SuperProgramer64/HoneyDelight/blob/main/docs/INSTALLATION.md).

## Verified changes

- Startup uses the current profile's directory and retries temporary failures.
- Context is refreshed when the phrase or field changes; stale results are discarded and password fields are excluded early.
- Filters repeated expansions and joined alternatives when a split is accepted.
- Retains valid splits when the optional budget runs out and performs a final check on space in Google.
- A local table of 16 common expressions supplements the general splitter, which can conservatively correct one letter in the second word.
- Excludes corrected composition from the context: `holacomo` and `holaxomo` suggest and commit `hola cómo`; backspace and retyping preserved the split without the reproduced concatenations in interactive checks.

## Tests and limitations

**19 JVM groups passed.** The latest native repeat returned **90/92**, following an earlier 92/92 run, using base models: 23 inputs, two rounds and two coordinate variants. In Google, six common expressions were checked in the suggestion bar and when pressing space, alongside the controls and backspace cycle above. In the latest repeat, `holaxomo` and `holacomo` each failed once with coordinates and a 55 ms budget; that intermittent behavior remains documented. These samples are not an overall accuracy percentage or a general comparison with Gboard.

An earlier broader comparison returned 82/84 expected decisions: the bilingual `erroes → errors` case with simulated coordinates remains open. Module-manager activation, first boot, recovery and an end-to-end public download still need testing. Local ART preparation for the ZIP passed in a test directory on the device.

The removed text-selection “Correct” prototype is not included. Packages exclude the developer's learned history, contacts, emails, personal instructions, logs, keys and passwords. Each installation's learning stays local.

## License

Original code is MIT licensed, © 2026 SuperProgramer64. Third-party components and derived assets retain their own rights; see `LICENSE` and `THIRD_PARTY_NOTICES.md`. This alpha is published for experimentation; module installation and first boot are not yet validated. MIT grants no rights over third-party components.

---

<a id="espanol"></a>

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
