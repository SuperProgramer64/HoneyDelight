# Compatibilidad teórica y variante con teclado montado

Investigación del 4 de noviembre de 2026. **Este documento propone objetivos de portabilidad; no amplía la compatibilidad del ZIP publicado.** La única combinación de desarrollo comprobada sigue siendo SM-S936B, Android 16 / One UI 8.5, Samsung Keyboard 5.9.30.97 y el fingerprint/hash de la alpha. Su activación como módulo y primer arranque siguen pendientes.

[Pruebas posteriores con cuatro APK reales y simulación del namespace antiguo →](COMPATIBILITY_TESTS.md)

## Resultado

Es viable diseñar una edición **Core sin Galaxy AI** y otra opción que aporte una base compatible de Samsung Keyboard mediante un montaje de sistema. Son dos decisiones independientes: qué funciones se activan y cómo se proporciona el teclado base.

El montaje del teclado debe conservar el APK original firmado por Samsung. HoneyDelight cargaría el código modificado por separado, como hace el prototipo mediante ART. El APK experimental refirmado no adquiere la firma ni el UID compartido de Samsung al colocarlo en `system/app`.

## Lo que se comprobó localmente

| Evidencia | Resultado | Implicación |
| :--- | :--- | :--- |
| Manifiesto binario del APK base | `minSdkVersion=33`, `targetSdkVersion=36`, compilación SDK 36 | Android 13 es el suelo declarado, no una prueba de funcionamiento en ese sistema. |
| Metadatos Samsung | `oneUiVersion=80500` | Describe la base de One UI 8.5; no sustituye una prueba sobre frameworks anteriores. |
| Identidad | `com.samsung.android.honeyboard`, `sharedUserId=android.uid.honeyboard`, versión 5.9.30.97, código 593097300 | El certificado y la identidad del paquete forman parte de la compatibilidad. |
| Parche | 16 clases objetivo y fragmentos concretos de sus métodos | No es un parche genérico para toda la familia 5.x o 5.9.x. |
| Compilación del puente | D8 con `--min-api 33` | El puente actual tampoco se anuncia para Android 12 o anteriores. |
| Instalador publicado | Exige SDK 36, ARM64, fingerprint y SHA-256 exactos | Rechaza otros Android/firmware antes de activar el mod. |
| ART | Contexto fijo con SmpsManager y bibliotecas de ventanas; rutas `oat/arm64` y `lib/arm64` esperadas | Hay que adaptar y comprobar rutas/contextos en cada familia de ROM. |
| Referencias de API | 46 métodos distintos introducidos en API 34, comparando smali con `api-versions.xml` del SDK 34 | Existen rutas más nuevas que Android 13. Este recuento no analiza si las llamadas están protegidas por comprobaciones de SDK ni cubre APIs 35/36. |
| Bibliotecas ARM64 del APK experimental | 50 bibliotecas ELF con alineación de segmentos LOAD de al menos 16 KiB, incluida Delight | Resultado favorable de alineación; no certifica carga, RELRO, empaquetado o ejecución en un dispositivo de 16 KiB. El teléfono de desarrollo usa 4 KiB. |

El APK declara opcionales SmpsManager, escritura manual y bibliotecas de ventanas/S Pen. Eso permite que PackageManager no las exija como bibliotecas obligatorias, pero el código debe tratar su ausencia. El contexto de compilación del módulo actual sí contiene rutas fijas. Android distingue estos dos aspectos: [bibliotecas opcionales](https://developer.android.com/guide/topics/manifest/uses-library-element) y [contexto de carga de ART](https://source.android.com/docs/core/runtime/art-class-loader-context).

La versión objetivo 36 no obliga por sí sola a Android 16: el límite de instalación declarado es la versión mínima. Las llamadas a APIs ausentes requieren tratamiento en tiempo de ejecución. [Documentación de uses-sdk](https://developer.android.com/guide/topics/manifest/uses-sdk-element).

## Matriz teórica

Todas las filas distintas de la referencia son **hipótesis de portabilidad**, no compatibilidad anunciada ni porcentajes de éxito. Las asociaciones Android/One UI se contrastaron con [Samsung](https://www.samsung.com/de/support/software-updates/).

| Entorno Samsung ARM64 | Valoración técnica | IA | Trabajo necesario |
| :--- | :--- | :--- | :--- |
| Android 16 / One UI 8.5, referencia exacta | Entorno del prototipo actual | Funciona en el teléfono de desarrollo | Validar activación del módulo, primer arranque y recuperación. |
| Android 16 / One UI 8.x, otro firmware o modelo | Primer objetivo razonable por proximidad al entorno actual | Depende del modelo y servicios disponibles | Verificar APK/certificado, bibliotecas, ART, rutas y funciones. Construir un ZIP específico; el actual no sirve por coincidencia de versión. |
| Android 15 / One UI 7 | Candidato de investigación para Core; trasplantar 5.9.30.97 no está demostrado | Opcional en dispositivos compatibles | Comprobar primero la base firmada y sus APIs Samsung; si falla, portar al teclado nativo de esa ROM. |
| Android 14 / One UI 6.1 o 6.1.1 | Candidato para Core con port específico; la base más nueva puede necesitar adaptaciones | Writing/Chat assist solo en modelos admitidos. Composer tiene diferencias de generación | Separar las funciones disponibles de los hooks nuevos de reescritura y validar el teclado base. |
| Android 14 / One UI 6.0 o modelo sin Writing assist | Core es un objetivo razonable de port, no una instalación lista | Desactivada si falta la capacidad | Usar una base nativa compatible o demostrar que la base aportada arranca; no mostrar modos de IA sin servicio. |
| Android 13 / One UI 5.x | Suelo declarado del APK; objetivo de mayor incertidumbre | Sin promesa de Galaxy AI | Preferir evaluar el teclado nativo de esa ROM y portar predicción; auditar las rutas de API 34+ antes de trasplantar 5.9.30.97. |
| Android 12 o anterior | Fuera de la base y del puente actuales | No aplicable a esta propuesta | Requiere otra base antigua, recompilación y auditoría de APIs; cambiar el número del manifiesto no es un port. |
| ROM AOSP/no Samsung, o ejecución de teclado solo de 32 bits | Fuera del alcance del paquete actual | No incluida | Faltan integración Samsung y/o la biblioteca Delight ARM64 del payload. El APK base contiene otras ABI, pero HoneyDelight no aporta Delight para ellas. |

No se propone un rango de versiones de Samsung Keyboard como «5.8–5.9 compatible»: solo se ha inspeccionado y parcheado la base **5.9.30.97 con hash exacto**. Para añadir otra versión hay que disponer de su APK original, localizar clases/métodos equivalentes y probarla. Una versión con el mismo número pero distinto hash también necesita revisión.

## Qué ocurre sin Galaxy AI

Delight carga sus modelos base y calcula predicciones localmente. Esa ruta no necesita el servicio de reescritura de Samsung. Por arquitectura, pueden conservarse predicción, separación, autocorrección, gestos y aprendizaje en una edición Core.

La integración de IA actual usa clases concretas del teclado (`Ja.i`, `Ia.b`, `Ha.c`, el escritor SCS y fragmentos WritingToolkit) y servicios externos. El APK contiene referencias a SCS, AICore e IntelliVoiceService. La presencia de esos nombres no demuestra que el teléfono tenga un servicio compatible o autorizado.

Samsung sitúa Writing/Chat assist en **dispositivos seleccionados con Android 14 / One UI 6.1 o superior**; no en todos los equipos que tengan esa versión. La introducción de Composer figura en la actualización One UI 6.1.1 de un modelo compatible. Por tanto, no se pueden equiparar corrección básica, estilos y composición libre. [Writing assist](https://www.samsung.com/us/support/answer/ANS10000943/), [nota de actualización de Composer](https://doc.samsungmobile.com/SM-F9460/027122230818/eng.html).

| Situación | Fallo posible | Tratamiento propuesto |
| :--- | :--- | :--- |
| La versión antigua del teclado no contiene clases/métodos de IA | El aplicador actual rechaza clases o fragmentos ausentes; si se forzase el código, pueden fallar referencias/reflexión | Perfil Core con hooks de IA excluidos y adaptador sin trabajo de IA. |
| El APK contiene interfaz de IA pero el teléfono no tiene el servicio | Modos visibles sin respuesta, errores de autenticación o tareas de generación inútiles | Comprobar capacidades reales; ocultar esos modos y no iniciar prefetcheo ni comandos de IA. |
| Hay Chat assist pero no el Composer que usa «A tu estilo» | Un modo básico puede funcionar mientras la composición libre falla | Capacidades separadas para corrección, estilos, traducción y Composer. |
| Servicio presente, pero versión/cuenta/red no disponibles | Error o resultado vacío | Error acotado, cancelar sin modificar el texto y conservar la predicción local. |

`Ai.addStyles()` ya captura fallos de reflexión, pero no es un detector de disponibilidad. La alpha no se ha probado en un teléfono sin esos servicios. Una edición Core debe excluir también el trabajo y las entradas de IA, no limitarse a esconder el icono.

## Variante que aporta el teclado compatible

### Diseño recomendado

1. **Comprobar el destino**: ROM Samsung, ABI, SDK, teclado instalado, versión/certificado compatible y ruta real. Verificar también que las dependencias del APK aportado existen o tienen una ruta opcional probada.
2. **Aportar una base original firmada** desde un APK compatible verificado por hash. Puede ser un paquete completo específico o una construcción local que reciba esa base. No utilizar el APK experimental refirmado como sustituto del firmado por Samsung.
3. **Montarla en la ubicación correcta** del teclado del sistema, conservando partición y tratamiento de permisos. Según la ROM será `system`, `product` o `system_ext`, y `app` o `priv-app`; no elegir una carpeta arbitraria ni crear un segundo paquete duplicado.
4. **Verificar qué APK eligió PackageManager**. Una actualización existente en `/data/app` puede seguir siendo la versión activa; no borrar esa actualización ni sus datos automáticamente para forzar el montaje.
5. **Preparar ART y librerías para esa ruta y ROM**, y aplicar el código modificado externamente al APK firmado. El control actual presupone una estructura que puede no existir en una app montada de sistema: necesita adaptación y prueba.
6. **Elegir Core o funciones de IA disponibles**. Las dos opciones comparten predicción local; la presencia del APK por sí sola no proporciona Galaxy AI.
7. **Comprobar arranque y reversión**. La desactivación quita los montajes; una actualización de Samsung puede migrar bases de datos, por lo que restaurar el APK anterior no garantiza que lea datos ya migrados. Hace falta una estrategia de copia local y recuperación antes de probar una base distinta.

Representación del contenido previsto, **no un instalador implementado**:

```text
HoneyDelight System Base
├── APK original Samsung, firmado y verificado
├── montaje de la ruta de sistema correspondiente a esa ROM
├── ART/librerías del mod preparados para la ruta activa
├── Delight + modelos base ES/EN
└── perfil Core o complemento Galaxy AI según capacidades
```

Los módulos de Magisk admiten un árbol `system/`, pero el archivo `skip_mount` lo desactiva. El módulo actual lo contiene porque realiza sus propios montajes; no se puede reutilizar ese empaquetado sin cambiar el diseño. La documentación actual de KernelSU delega el montaje de `system/` en un metamódulo, como `meta-overlayfs`; los scripts por sí solos no sustituyen ese requisito para la variante propuesta. [Magisk](https://topjohnwu.github.io/Magisk/guides.html), [KernelSU](https://kernelsu.org/guide/module.html).

El APK base debe estar montado antes de que Android examine las apps de sistema. La compilación pesada de ART debe prepararse fuera de una etapa bloqueante de arranque y comprobarse contra la ruta activa. El servicio actual espera a `sys.boot_completed`; sirve para sus montajes de desarrollo, pero no para presentar por primera vez un APK al escaneo inicial del sistema. [Etapas de KernelSU](https://kernelsu.org/guide/module.html#boot-scripts).

### Por qué `system/app` no basta

- `system/app` no concede automáticamente privilegios de `priv-app` ni permisos de firma.
- Una app privilegiada necesita las reglas apropiadas en la misma partición. Reglas incompletas pueden provocar fallos de arranque cuando la política exige su cumplimiento. No debe copiarse una lista universal de permisos de otra ROM. [AOSP: permisos privilegiados](https://source.android.com/docs/core/permissions/perms-allowlist).
- El UID compartido y las actualizaciones exigen certificados compatibles; conservar un nombre de paquete no resuelve una firma distinta. [Android: UID compartido](https://developer.android.com/guide/topics/manifest/manifest-element), [AOSP: firmas de actualización](https://source.android.com/docs/core/ota/sign_builds).
- La base requiere APIs, recursos y servicios del framework Samsung. El montaje no aporta un framework de One UI completo.
- Una actualización bajo `/data/app`, las diferencias de ART o los permisos SELinux pueden impedir que se cargue el código previsto incluso si el APK está presente.
- La migración de datos y los perfiles secundarios requieren pruebas propias; no se debe importar aprendizaje personal de otro teléfono.

## Prioridad propuesta

1. **Core sobre el teclado ya instalado**, primero en Android 16 y después en Android 15 / One UI 7. Separar los hooks de IA y detectar capacidades. Esto amplía usuarios sin exigir trasplantar un teclado más nuevo.
2. **System Base para una combinación concreta**, conservando el APK firmado, con selección de versión/ruta y recuperación comprobadas. No anunciarlo como universal.
3. **Port a Android 14 y 13** con bases nativas verificadas cuando el trasplante de 5.9.30.97 no sea adecuado.

Esta investigación no instala ni sustituye nada en el teléfono, no modifica los binarios de la release y no relaja los controles de compatibilidad actuales. No existe aún un ZIP Core o System Base listo para descargar.
