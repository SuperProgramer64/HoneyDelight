<div align="center">

![HoneyDelight — Tu Samsung Keyboard, con un toque de Delight.](assets/banner.svg)

**Se ve como Samsung. Predice con Delight.**

![Estado: alpha experimental](https://img.shields.io/badge/estado-alpha_experimental-e9b45d?style=flat-square)
![Android probado: 16](https://img.shields.io/badge/Android_probado-16-b9e7dd?style=flat-square)
[![Licencia: MIT](https://img.shields.io/badge/licencia-MIT-blue?style=flat-square)](LICENSE)
![Acceso root necesario](https://img.shields.io/badge/root-necesario-d5dae0?style=flat-square)

[Qué hace](#qué-hace) · [Compatibilidad](#compatibilidad) · [Cómo lo probé](#cómo-lo-probé) · [Lo que viene](#lo-que-viene)

</div>

## Por qué existe

Me gusta Samsung Keyboard: su diseño, sus gestos, cómo encaja con el resto del teléfono. Lo que nunca me terminó de convencer era lo que pasaba por dentro: sugerencias que no entendían el contexto, correcciones que llegaban tarde o no llegaban, palabras pegadas que tocaba separar a mano.

HoneyDelight nace de esa espinita. Conecta **Delight5, el motor nativo de Gboard**, con la interfaz de Samsung Keyboard, para conservar el teclado que ya conoces y aprovechar su motor de predicción. El puente añade sus propias reglas; el resultado todavía tiene diferencias respecto a la app Gboard. Por el camino también le saqué más partido a Galaxy AI y fui arreglando los tropiezos que aparecían al juntar los dos mundos: palabras pegadas, sugerencias repetidas, campos que no corregían y esperas que volvían lento el editor de reescritura.

Está en alpha y lo uso en mi propio teléfono. Las pruebas y el uso diario van marcando qué mejorar. Cuando un ajuste empeora las sugerencias, vuelvo a la revisión anterior y guardo el caso para reproducirlo.

### ¿Por qué HoneyDelight?

**Honey** es por **HoneyBoard**, el nombre interno de Samsung Keyboard. **Delight**, por el motor que le da la predicción. El nombre reúne las dos piezas que quiero hacer trabajar juntas.

## Qué hace

| Área | Lo que aporta HoneyDelight |
| :--- | :--- |
| **Predicción** | Sugerencias de Delight5 que tienen en cuenta lo que escribiste antes, con reglas de corrección y la forma real del teclado. |
| **Palabras pegadas** | Combina el separador de Delight con una tabla local de 16 expresiones frecuentes. Filtra repeticiones y alternativas pegadas cuando acepta un corte. |
| **Toques y gestos** | El motor recibe dónde tocó tu dedo de verdad, y el deslizar usa el decodificador de gestos de Delight5. |
| **Aprendizaje** | Utiliza el historial local disponible y filtros que reducen el aprendizaje de erratas y texto pegado. No importa los datos aprendidos de quien desarrolla el mod. |
| **Campos difíciles** | Vuelve a corregir en los campos de Google que lo tenían desactivado, sin tocar las barras de direcciones de los navegadores. |
| **Galaxy AI** | Estilos nuevos y una cápsula en Editar para decirle a la IA cómo quieres el texto, con mejoras al confirmar el resultado. |
| **Fluidez** | Corrige las esperas reproducidas entre el editor y los hilos de predicción; las pruebas del usuario confirmaron menos lag. |
| **Conexión** | Espera el directorio del perfil actual antes de iniciar Delight y reintenta los fallos temporales, sin desactivarlo para todo el proceso. |

Comprobados **en la barra del teclado y al pulsar espacio en Google**, el 4 de noviembre de 2026:

| Entrada | Sugerencia y texto confirmado |
| :--- | :--- |
| `buenosdias` | buenos días |
| `buenastardes` | buenas tardes |
| `buenasnoches` | buenas noches |
| `hastamanana` | hasta mañana |
| `tequiero` | te quiero |
| `porfavor` | por favor |

Los 16 casos de la tabla local se resuelven sin gastar el presupuesto opcional de búsqueda del motor. El resto sigue pasando por el separador general, que aún necesita trabajo. `gustamucho → gusta mucho` también pasó el control en Google; la revisión más reciente añade corrección de una letra en la parte derecha y evita duplicar la composición al retroceder.

Las posiciones del dedo ya llegan al motor. La calibración personalizada existe, pero la dejé apagada: hasta que no demuestre que acierta más, no se activa.

## Compatibilidad

> **Esta primera alpha es para gente con root que sepa preparar su instalación y volver atrás si algo sale mal. Todavía no es un instalador para todo el mundo.**

| Componente | Entorno probado |
| :--- | :--- |
| Dispositivo | Samsung Galaxy S25+ · SM-S936B |
| Sistema | Android 16 · One UI 8.5 |
| Samsung Keyboard | 5.9.30.97 |
| Interfaz JNI de Gboard | 18.5.6 |
| Arquitectura | arm64-v8a |
| Root del entorno de desarrollo | KernelSU 3.3.0; activación como módulo aún sin validar |
| Compilación Samsung del ZIP | `S936BXXSBCZG3_OWOBCZG3` |
| Identificador compatible | `1a9a24823d75` |

HoneyDelight no toca el APK original firmado por Samsung. Durante el desarrollo, el código compilado y las librerías se montan encima de forma reversible. Eso sí: los artefactos tienen que corresponder exactamente al APK y al entorno ART de tu dispositivo. Que coincida el número de versión no basta.

[Compatibilidad teórica con Android 13–16, funcionamiento sin IA y propuesta de teclado montado →](docs/COMPATIBILITY_RESEARCH.md)

La investigación distingue candidatos de portabilidad del soporte actual: el ZIP publicado sigue exigiendo su combinación exacta. Las variantes Core y System Base todavía son propuestas. Ya se compararon cuatro APK reales y se ejecutaron regresiones en el Mac: [resultados de portabilidad](docs/COMPATIBILITY_TESTS.md).

### Qué necesitas y qué instala el módulo

La combinación recomendada para **probar esta alpha** es exactamente la de la tabla. No se ha comprobado otra versión de Android, One UI o Samsung Keyboard. El ZIP verifica el fingerprint completo y el hash del APK; tener solo el mismo modelo o número de versión no basta. Magisk es una vía de instalación prevista por el formato, pero no tiene una versión validada en este proyecto.

**El módulo trae el mod completo: puente, parches, biblioteca Delight5 y modelos base de español e inglés. No necesitas instalar Gboard, importar sus datos ni descargar esos modelos por separado.** Necesitas Samsung Keyboard compatible ya instalado y root; el ZIP no instala Samsung Keyboard desde cero ni añade Galaxy AI a dispositivos que no lo soporten. Las funciones de IA siguen dependiendo del servicio y los requisitos de Samsung.

Descarga el ZIP compatible en [Releases](https://github.com/SuperProgramer64/HoneyDelight/releases), instálalo desde el gestor root y conserva otro teclado disponible para recuperar la entrada. Una OTA o actualización del teclado puede exigir un paquete nuevo; no fuerces un ZIP rechazado. La guía detalla los métodos y el estado de validación.

### La forma principal: el módulo ZIP

La entrega principal es un **módulo root** en formato KernelSU/Magisk. La app es opcional. La activación desde el gestor y el primer arranque de esta alpha siguen pendientes de validación. Cada ZIP está hecho para una compilación concreta y, antes de instalarse, comprueba el APK, la versión de Android y que sus archivos estén íntegros.

Si quieres enterarte de las versiones nuevas sin estar pendiente, existe **HoneyDelight Updates**, un complemento opcional: te avisa y puede bajar el ZIP compatible por Wi-Fi o redes sin medición. Comprueba el SHA-256, lo guarda en `Descargas/HoneyDelight` y la instalación la decides tú. No pide root ni toca tus historiales ni modelos aprendidos.

| Método | Uso |
| :--- | :--- |
| **KernelSU / Magisk** | Descarga el ZIP y usa «Instalar desde almacenamiento» en el gestor. |
| **HoneyDelight Updates** | Consulta y descarga las releases; abre el ZIP con el gestor root. Opcional. |
| **Termux con root** | Ejecuta el instalador de módulos de tu gestor mediante `su`. |
| **ADB con root** | Copia el ZIP al teléfono y llama al mismo instalador desde ADB. |

La APK de Updates vive en `/data/app` y se actualiza como cualquier app con la misma firma. El teclado sigue siendo el original firmado por Samsung; nunca se reemplaza por uno refirmado.

> **Dónde estamos · 4 de noviembre de 2026:** la revisión del teclado está instalada mediante los montajes de desarrollo. El ZIP pasó compatibilidad, checksums y preparación local de ART en el teléfono; todavía faltan la activación como módulo, el primer arranque y la recuperación. Updates compila e instala; la descarga completa y los avisos desde una release pública aún necesitan prueba.

El repositorio es **público**, con una alpha experimental. El actualizador solo consulta releases públicas y no lleva credenciales de GitHub.

### Tres paquetes, tres funciones

| Archivo | Para qué sirve | Estado |
| :--- | :--- | :--- |
| **Módulo ZIP** | Aplicar HoneyDelight conservando el APK firmado por Samsung. | Entrega principal; preparación e integridad verificadas. Falta validar activación desde el gestor y primer arranque. |
| **HoneyDelight Updates APK** | Consultar y descargar módulos compatibles. Interfaz renovada, iconos adaptativos y avisos. | App opcional, sin root ni modelos aprendidos. `0.1.0-alpha.2`, con la misma firma de las versiones de prueba. |
| **Keyboard APK experimental** | APK completo con código modificado, biblioteca y modelos base incluidos. | Firmado con clave propia; no actualiza directamente el teclado oficial. Instalación y permisos/servicios de Samsung pendientes de validar. |

[Qué contiene el APK completo y por qué es experimental →](docs/FULL_APK.md)

[Instalación, comandos y recuperación →](docs/INSTALLATION.md)

## Cómo lo probé

La revisión actual tiene pruebas en tres niveles:

| Prueba | Resultado y alcance |
| :--- | :--- |
| **Regresiones JVM** | 19 grupos pasaron: arranque por perfil, privacidad, editor, candidatos, respuestas atrasadas y separación, entre otros. |
| **Ruta completa con Delight real** | Última repetición: **90/92**; una anterior obtuvo 92/92. Son 23 entradas, dos repeticiones y dos variantes de coordenadas. `holaxomo` y `holacomo` fallaron una vez cada una con coordenadas y presupuesto de 55 ms; la separación sigue siendo intermitente en ese recorrido. |
| **Barra y espacio en Google** | Las seis combinaciones de la tabla anterior mostraron separación en la barra y quedaron separadas al pulsar espacio. |
| **Controles adicionales en Google** | `gustamucho → gusta mucho`, `programacion → programación` y `cocodrilo → cocodrilo` pasaron. `holacomo` había quedado como `holacómo`. La revisión posterior mejoró esos casos; en Google ya se verificaron la sugerencia separada, la confirmación con espacio y el retroceso en ambos casos. |
| **Comparación ampliada del cálculo** | 82/84 decisiones esperadas. Los dos fallos fueron `erroes → errors` con coordenadas simuladas; la mezcla español/inglés sigue pendiente. |
| **Empaquetado y actualizador** | Pasaron las pruebas de exclusión de datos personales, compatibilidad, parche, CRC y selección/verificación de descargas. |

Estas cifras corresponden a una regresión concreta, no a un porcentaje de precisión global ni una comparación general con Gboard. La prueba nativa usa diccionarios base; los contenedores de prueba y los datos aprendidos no se empaquetan en el teclado.

La fluidez del editor tampoco se ajustó solo a ojo: las trazas mostraron una espera circular de Samsung de unos 500 ms antes del arreglo. Después, el usuario confirmó menos lag en ambos campos. No atribuyo a la revisión una latencia final exacta que no se haya medido.

En otra evaluación del toque reservé 100 palabras: centros de las teclas, coordenadas reales y centros personalizados obtuvieron 80 aciertos cada uno. La calibración personalizada sigue apagada porque todavía no mostró ventaja.

### Lo que todavía falla

- La separación general aún tiene fallos intermitentes con el presupuesto corto de 55 ms; la última repetición nativa obtuvo 90/92. La revisión del caso `holaxomo` también comprueba espacio y retroceso; una tabla local de expresiones frecuentes no resuelve cualquier combinación posible.
- La mezcla español/inglés puede elegir una corrección del otro idioma.
- La carga efectiva en el perfil de apps duplicadas aún necesita comprobación en uso, aunque la regresión de arranque por perfil pasó.
- Cuando queda una sola sugerencia, Samsung la coloca a la izquierda; el centrado no se ha modificado.
- Falta completar las pruebas de instalación, primer arranque y recuperación del módulo.

[Estado completo y límites conocidos →](docs/STATUS.md)

## Construir y probar

Necesitas **JDK 17**, Android SDK Platform 34 y Build Tools 34.0.0.

```sh
export ANDROID_HOME=/ruta/a/android-sdk
bash port/tests/run.sh
python3 scripts/tests/test_hooks.py
python3 scripts/tests/test_module.py
bash manager/tests/run.sh
python3 module-tools/tests/test_crc.py
```

La regresión de la ruta completa requiere un dispositivo seleccionado y los componentes base verificados:

```sh
export ANDROID_SERIAL=identificador-de-tu-dispositivo
export BASE_COMPONENTS_DIR=/ruta/a/componentes-base-locales
bash scripts/test-native-predictions.sh
```

Ese script usa una carpeta temporal de prueba: no instala el teclado, no abre apps ni consulta el aprendizaje del usuario.

Para preparar una copia local del APK compatible:

```sh
export APKTOOL_JAR=/ruta/a/apktool_2.12.1.jar
bash scripts/prepare-samsung.sh /ruta/a/base.apk
```

El script verifica el hash antes de aplicar los parches. No toca el APK instalado ni instala nada en el teléfono.

[Guía de construcción →](docs/BUILD.md)

## Lo que viene

- [x] Empaquetador de módulo que comprueba APK, compilación de Android, arquitectura e integridad.
- [x] App opcional que avisa y descarga releases compatibles.
- [x] Registro detallado opcional, apagado por defecto en la preparación de release.
- [x] Ruta local para 16 expresiones frecuentes y regresión nativa de la predicción completa.
- [ ] Ampliar la separación general sin depender de una lista de expresiones.
- [ ] Confirmar el arranque efectivo de Delight en apps duplicadas.
- [ ] Verificar de punta a punta la instalación, activación y primer arranque del módulo.
- [ ] Desactivación y recuperación probadas cuando el teclado se actualice.
- [ ] Pruebas de arranque y en otros dispositivos compatibles.
- [ ] Menos correcciones no deseadas al mezclar español e inglés.
- [ ] Aprender de las correcciones que deshaces.
- [ ] Calibración del dedo que demuestre una mejora real con datos reservados.

**Corregir desde el menú de selección** se quedó por el camino: no convenció en las pruebas, así que no viene incluido.

## Código, datos y componentes externos

Aquí está el código propio del proyecto: el puente, sus declaraciones JNI y stubs, los arneses de prueba, los parches de integración y la documentación. **El árbol de código no incluye APK de Samsung o Gboard, librerías nativas, modelos, historiales, contactos, conversaciones, claves ni tokens de mi teléfono.** El módulo local sí lleva los componentes base que necesita para funcionar, con hashes fijados, pero nada de lo que el teclado haya aprendido de quien lo usa.

El registro detallado viene apagado. Si lo enciendes, ten en cuenta que puede guardar texto personal: revísalo antes de compartirlo. Las funciones de Galaxy AI usan los servicios y ajustes de Samsung, así que algunas pueden necesitar conexión.

[Privacidad y diagnóstico →](docs/PRIVACY.md)

## Colaborar

¿Encontraste un fallo? Cuéntame en qué campo pasó, qué hiciste, qué esperabas y qué salió. Añade el modelo, la versión de Android/One UI y la de Samsung Keyboard. Si es de predicción, una frase inventada que lo reproduzca ayuda más que una conversación real, y de paso protege tu privacidad.

Si quieres proponer un cambio, pasa antes las regresiones y cuéntame qué caso mejora. Por favor, no adjuntes componentes propietarios ni datos personales.

---

Creado por **[SuperProgramer64](https://github.com/SuperProgramer64)**, frase de prueba a frase de prueba. HoneyDelight es un proyecto independiente, sin afiliación con Samsung, Google o Microsoft.

El código original de HoneyDelight se distribuye bajo la [licencia MIT](LICENSE), © 2026 SuperProgramer64. El alcance y las excepciones de componentes externos se detallan en [Avisos de terceros](THIRD_PARTY_NOTICES.md).
