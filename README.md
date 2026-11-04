<div align="center">

![HoneyDelight — Tu Samsung Keyboard, con un toque de Delight.](assets/banner.svg)

**Predicción de Delight5 · Interfaz Samsung · Más posibilidades al escribir**

![Estado: alpha experimental](https://img.shields.io/badge/estado-alpha_experimental-e9b45d?style=flat-square)
![Android probado: 16](https://img.shields.io/badge/Android_probado-16-b9e7dd?style=flat-square)
[![Licencia: MIT](https://img.shields.io/badge/licencia-MIT-blue?style=flat-square)](LICENSE)
![Acceso root necesario](https://img.shields.io/badge/root-necesario-d5dae0?style=flat-square)

[Qué hace](#qué-hace) · [Compatibilidad](#compatibilidad) · [Pruebas](#pruebas-con-evidencia) · [Hoja de ruta](#lo-que-viene)

</div>

## Una escritura que se sienta mejor

Samsung Keyboard tiene una interfaz que quiero conservar. HoneyDelight busca mejorar lo que ocurre mientras escribo: las palabras que propone, las correcciones que elige y las herramientas que ayudan a revisar un mensaje.

El proyecto conecta **Delight5, el motor nativo de Gboard**, con Samsung Keyboard y amplía sus funciones de Galaxy AI. También corrige problemas de integración: palabras pegadas, sugerencias repetidas, campos que no corregían y esperas que hacían lento el editor de reescritura.

No es una recreación completa de Gboard ni una promesa de precisión perfecta. Es un mod en desarrollo, probado en uso real y acompañado de pruebas que permiten seguir mejorándolo.

### ¿Por qué HoneyDelight?

**Honey** viene de **HoneyBoard**, el nombre interno de Samsung Keyboard. **Delight** viene del motor que aporta la predicción. El nombre junta las dos partes que hacen posible el proyecto.

## Qué hace

| Área | Lo que aporta HoneyDelight |
| :--- | :--- |
| **Predicción** | Candidatos de Delight5 con contexto, reglas de corrección y geometría de las teclas. |
| **Palabras pegadas** | Recupera separaciones y filtra expansiones repetidas del decodificador. |
| **Toques y gestos** | Pasa posiciones del dedo al motor y conecta la decodificación de deslizamientos. |
| **Aprendizaje** | Usa historial local y filtros para evitar aprender algunas erratas y texto pegado como escritura propia. |
| **Campos difíciles** | Habilita corrección en campos de Google que la desactivaban, sin extender esa excepción a las barras de direcciones de navegadores. |
| **Galaxy AI** | Añade estilos y una cápsula de instrucciones en Editar, con mejoras en la confirmación del resultado. |
| **Fluidez** | Corrige esperas entre el editor local y los hilos de predicción. |

Algunos casos comprobados con el motor real:

```text
Ytú           → Y tú
buenosdias    → buenos días
buenasnoches  → buenas noches
gustamucho    → gusta mucho
```

Las posiciones del dedo ya se utilizan, pero la calibración personalizada experimental no se activa: todavía no mostró una mejora medible.

## Compatibilidad

> **Esta primera alpha está dirigida a desarrolladores y usuarios de root que puedan preparar y recuperar su instalación. Todavía no es un instalador universal.**

| Componente | Entorno probado |
| :--- | :--- |
| Dispositivo | Samsung Galaxy S25+ · SM-S936B |
| Sistema | Android 16 · One UI 8.5 |
| Samsung Keyboard | 5.9.30.97 |
| Interfaz JNI de Gboard | 18.5.6 |
| Arquitectura | arm64-v8a |

El mod mantiene el APK original firmado por Samsung. La instalación de desarrollo usa montajes reversibles de código compilado y librerías; los artefactos deben corresponder al APK y al entorno ART del dispositivo. Coincidir en el número de versión no basta.

### Descarga principal: el módulo ZIP

HoneyDelight se ofrece como **módulo root**, instalable directamente desde KernelSU o Magisk. La app no es un requisito para usarlo. Cada ZIP corresponde a una compilación concreta y comprueba el APK, Android y la integridad de sus archivos antes de aceptarse.

**HoneyDelight Updates** es un complemento opcional: avisa de nuevas versiones y puede descargar el ZIP compatible automáticamente, por Wi-Fi o redes sin medición. Comprueba SHA-256, guarda en `Descargas/HoneyDelight` y deja la instalación en manos del usuario. La app no solicita root ni accede a historiales o modelos aprendidos.

| Método | Uso |
| :--- | :--- |
| **KernelSU / Magisk** | Descarga el ZIP y usa «Instalar desde almacenamiento» en el gestor. |
| **HoneyDelight Updates** | Consulta y descarga las releases; abre el ZIP con el gestor root. Opcional. |
| **Termux con root** | Ejecuta el instalador de módulos de tu gestor mediante `su`. |
| **ADB con root** | Copia el ZIP al teléfono y llama al mismo instalador desde ADB. |

La APK de Updates se instala en `/data/app` y admite actualizaciones normales con la misma firma. Conserva el teclado firmado de Samsung; no intenta sustituirlo por un APK refirmado.

> El ZIP local pasó la comprobación de compatibilidad y checksums en el dispositivo de desarrollo. La activación del módulo y el primer arranque todavía no se han verificado. La app ya compila e instala, pero la descarga real de una release se probará cuando exista una publicación.

El repositorio permanece **privado durante la preparación**. El actualizador consulta releases públicas: no se incluyen credenciales de GitHub dentro de la APK.

[Instalación, comandos y recuperación →](docs/INSTALLATION.md)

## Pruebas con evidencia

La revisión estable pasó **ocho grupos de regresión local**. La preparación de release añade una novena prueba de privacidad, pruebas del empaquetador y pruebas de selección y verificación de actualizaciones. También se comprobaron ocho casos de separación en el motor nativo del teléfono y se conservaron los resultados de una referencia de 26 frases de contexto.

El ajuste del editor se comprobó con trazas de los hilos: una espera circular de Samsung podía detener una pulsación alrededor de 500 ms. Después del cambio, el usuario confirmó una escritura más fluida en ambos campos del editor.

Esto no equivale a una evaluación general frente a otros teclados. La comparación espacial reservó 100 palabras para evaluar: centros nominales, coordenadas reales y centros personalizados obtuvieron 80 aciertos cada uno. Esa medición guía el siguiente trabajo; no es una cifra global de precisión.

[Estado completo y límites conocidos →](docs/STATUS.md)

## Construir y probar

Requiere **JDK 17**, Android SDK Platform 34 y Build Tools 34.0.0.

```sh
export ANDROID_HOME=/ruta/a/android-sdk
bash port/tests/run.sh
```

Para preparar una copia local del APK compatible:

```sh
export APKTOOL_JAR=/ruta/a/apktool_2.12.1.jar
bash scripts/prepare-samsung.sh /ruta/a/base.apk
```

El script comprueba el hash antes de aplicar los parches. No modifica el APK instalado ni instala el mod.

[Guía de construcción →](docs/BUILD.md)

## Lo que viene

- [x] Empaquetador de módulo con comprobación del APK, compilación Android, arquitectura e integridad.
- [x] Aplicación opcional de avisos y descarga de releases compatibles.
- [ ] Verificación completa de instalación, activación y primer arranque del módulo.
- [ ] Desactivación y recuperación verificadas ante actualizaciones del teclado.
- [ ] Pruebas de arranque y de otros dispositivos compatibles.
- [ ] Menos correcciones indeseadas entre español e inglés.
- [ ] Aprender de las correcciones que el usuario deshace.
- [ ] Calibración del dedo con una mejora demostrada en datos reservados.
- [x] Registro detallado opcional y apagado por defecto en la preparación de release.

La propuesta de **Corregir desde el menú de selección** se retiró durante las pruebas. Su prototipo no está incluido ni se anuncia como una función terminada.

## Código, datos y componentes externos

Aquí se comparten el puente, sus declaraciones JNI y stubs, los arneses, los parches de integración y la documentación. **El árbol de código no incluye APK de Samsung o Gboard, librerías nativas, modelos, historiales, contactos, conversaciones, claves ni tokens del dispositivo de desarrollo.** El módulo local incluye los componentes base necesarios para funcionar, con hashes fijados; no incluye entrenamiento del usuario.

El registro detallado está apagado por defecto. Si lo activas, sus registros pueden contener texto personal. Revísalos antes de compartirlos. Las funciones Galaxy AI usan los servicios y la configuración de Samsung; no se promete que todas se ejecuten sin red.

[Privacidad y diagnóstico →](docs/PRIVACY.md)

## Colaborar

Si encuentras un error, describe el campo, los pasos, lo esperado y lo que ocurrió. Indica modelo, Android/One UI y versión de Samsung Keyboard. Para problemas de predicción, una frase de prueba inventada suele ser más útil que una conversación privada.

Antes de proponer cambios, ejecuta las regresiones y explica qué caso mejora el ajuste. Evita adjuntar componentes propietarios o datos personales.

---

Creado por **[SuperProgramer64](https://github.com/SuperProgramer64)**. HoneyDelight es un proyecto independiente, sin afiliación con Samsung, Google o Microsoft.

El código original de HoneyDelight se distribuye bajo la [licencia MIT](LICENSE), © 2026 SuperProgramer64. El alcance y las excepciones de componentes externos se detallan en [Avisos de terceros](THIRD_PARTY_NOTICES.md).
