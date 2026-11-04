# Preparación de release · 0.1.0-alpha

**Alpha experimental pública, publicada por autorización del propietario.**

## Entregables

- Módulo ZIP específico de dispositivo/compilación: descarga principal.
- APK opcional HoneyDelight Updates: avisos y descargas, sin root.
- APK completo del teclado, experimental y con firma propia, sin validación de instalación/servicios Samsung.
- Código, parches, pruebas, banner y documentación.
- `LICENSE` y `THIRD_PARTY_NOTICES.md`: licencia MIT del código original y alcance de terceros.

El ZIP local contiene componentes necesarios para el mod y se conserva fuera del árbol de código. No incluye entrenamiento del usuario. Los componentes externos no adquieren una licencia abierta por aparecer en un paquete local; su distribución debe revisarse antes de publicar el binario.

## Comprobado

- Diecinueve grupos JVM del puente, incluidos privacidad, arranque por perfil y respuestas atrasadas.
- Resolución de parches por descriptor de clase.
- Exclusión de historial/token ficticios en el empaquetador.
- Rechazo de APK/fingerprint incorrectos en la validación.
- Reglas de variante compatible, nombre de archivo, URL GitHub y SHA-256 del actualizador.
- Compilación e instalación de la APK opcional en /data/app, con firma local conservada entre versiones.
- Reconstrucción completa del payload y del módulo a partir de este árbol de código y componentes base locales.
- Compatibilidad y checksums del ZIP local en el dispositivo de desarrollo.
- Preparación local de ART y de CRC ejecutada en una carpeta de prueba del teléfono, sin activar el módulo.

## Pendiente antes de publicar un binario para usuarios

- Instalación/activación del módulo, primer arranque y recuperación.
- Cambio/actualización del APK Samsung y rechazo seguro de artefactos antiguos.
- Descarga real y notificación desde una release, incluidas alphas marcadas como prerelease.
- Prueba de aprendizaje propio en instalación limpia y ampliación de apps/perfiles.
- Revisar distribución de componentes externos; el código original ya tiene licencia MIT.

## Sugerencias antes del siguiente corte

1. Medir correcciones rechazadas y palabras confirmadas con frases inventadas, no con conversaciones privadas.
2. Mejorar la mezcla español/inglés y las separaciones difíciles.
3. Añadir retroalimentación de deshacer antes de ampliar funciones de IA.

## Ideas para Galaxy AI

- Revisión conservadora de frase con diferencias visibles, aplicar y deshacer.
- Preservar nombres, persona gramatical, tono y hechos como reglas explícitas.
- Preferencias de estilo guardadas localmente y seleccionadas por el usuario.
- Respuesta asíncrona con cancelación y comprobación de que el texto no cambió.

La IA se invoca bajo demanda para la frase; Delight atiende el tecleo inmediato. El prototipo retirado del menú Corregir no forma parte de este corte.
