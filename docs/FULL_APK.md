# APK completo experimental

`HoneyDelight-Keyboard-0.1.0-alpha-experimental.apk` contiene el APK base exacto de Samsung Keyboard 5.9.30.97, los tres DEX con el mod, la biblioteca nativa Delight5 y los modelos base de español e inglés.

Es una variante para desarrollo. La construcción, la firma y la integridad se comprueban; **su instalación y funcionamiento completo no están validados**. No es la APK de HoneyDelight Updates.

## Firma y permisos

Al modificar el contenido del APK no se puede conservar la firma válida de Samsung. El paquete experimental está firmado con la clave propia de HoneyDelight. Mantiene el paquete y el `sharedUserId` originales; Android exige compatibilidad de certificados para actualizar ese paquete y participar en su UID compartido.

Por eso esta APK **no actualiza directamente el teclado oficial** y no se anuncia como instalable con un toque en un Samsung de fábrica. Tampoco se ha comprobado el acceso a permisos de firma ni a servicios Galaxy AI con ese certificado. No se incluye un procedimiento de desinstalación del teclado del sistema o de modificación de sus comprobaciones de firma.

Para el teléfono de desarrollo y la alpha, el **módulo ZIP** sigue siendo la vía principal: mantiene el APK original firmado por Samsung y cambia el código cargado mediante los montajes del mod.

## Modelos y datos

Los modelos base se empaquetan en `assets/honeydelight/`. La variante completa incluye una preparación asíncrona que los copia a la carpeta privada del perfil actual, comprueba SHA-256 y no reemplaza modelos ya existentes. No contiene historial entrenado, contactos, correos, instrucciones guardadas ni tokens del desarrollador.

Las clases de preparación del APK viven en `port/apk-src/`; no se añaden al puente del módulo. Los recursos y el manifiesto binario se conservan desde el APK original, evitando reconstruirlos o cambiar sus nombres con apktool.

## Construir

Primero genera el payload actual del módulo con `scripts/build-device-payload.sh`. Después:

```sh
export JAVA_HOME=/ruta/a/jdk-17
export ANDROID_HOME=/ruta/a/android-sdk
export FULL_APK_KEYSTORE=/ruta/privada/a/tu-clave.p12
export FULL_APK_STORE_PASS=tu-password-local
bash scripts/build-full-apk.sh /ruta/a/base-original.apk
```

El script comprueba el APK original y los componentes base por hash, compila la variante del puente, conserva los recursos originales, alinea y firma el resultado en `dist/`. No instala el APK ni toca el teclado activo.

```sh
bash port/tests/run-apk.sh
python3 scripts/tests/test_full_apk.py
```

Estas pruebas verifican preparación de modelos, integridad, conservación de recursos y exclusión de datos de entrada ajenos al paquete. No sustituyen una prueba de instalación en un entorno de desarrollo apropiado.

## Distribución

El paquete completo incluye código y recursos de Samsung y componentes base de Gboard. La licencia MIT de HoneyDelight cubre únicamente sus aportes originales. La alpha no relicencia esos componentes ni concede permisos adicionales sobre ellos. El código de preparación permite reconstruir el artefacto con una copia local compatible.
