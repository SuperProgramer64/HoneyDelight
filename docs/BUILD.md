# Construcción reproducible de la alpha

## Requisitos

- JDK 17, Python 3, Git, adb y apktool 2.12.1.
- Android SDK Platform 34 y Build Tools 34.0.0.
- APK original de Samsung Keyboard 5.9.30.97 cuyo SHA-256 coincida con `patches/compatibility.json`.
- Dispositivo de destino con root, SDK 36 y arm64-v8a, conectado por ADB.
- Tres componentes base de Gboard aportados localmente: `libintegrated_shared_object.so`, `es_us.dict`, `en_us.dict`. Sus hashes deben coincidir con `patches/base-components.json`. No usar historiales ni diccionarios personales.

## Pruebas

```sh
export JAVA_HOME=/ruta/a/jdk-17
export ANDROID_HOME=/ruta/a/android-sdk
bash port/tests/run.sh
python3 scripts/tests/test_hooks.py
python3 scripts/tests/test_module.py
bash manager/tests/run.sh
```

Los nueve grupos JVM no cargan el motor nativo. Las pruebas del empaquetador trabajan con datos ficticios. La herramienta de instalación tiene además una prueba JVM de CRC en `module-tools/tests/test_crc.py`. La plantilla de CI está en `docs/ci-tests.yml.example` y puede activarse más adelante desde GitHub.

## Generar un módulo para el dispositivo

```sh
export APKTOOL_JAR=/ruta/a/apktool_2.12.1.jar
export ANDROID_SERIAL=identificador-de-tu-dispositivo
export BASE_COMPONENTS_DIR=/ruta/a/componentes-base-locales
bash scripts/build-device-payload.sh /ruta/a/base.apk
```

El script valida el APK y los componentes base, decodifica una copia local, aplica los hooks por descriptor de clase, compila el puente y la herramienta de instalación y genera en `dist/` un ZIP compatible con esa compilación. El módulo prepara los artefactos ART en el teléfono durante la instalación, mediante dex2oat y una herramienta propia, sin Termux ni JDK en el móvil. Esta construcción se ejecutó correctamente durante la preparación del 4 de noviembre de 2026.

No instala ni modifica el teclado activo. Los archivos propietarios, datos de compilación y paquetes generados quedan en `work/` o `dist/`, ignorados por Git.

El entorno de carga Samsung/ART sigue preparado para el modelo y la versión probados. El fingerprint no sustituye pruebas de cambios de ART, firmware, arranque o nuevos dispositivos.

Para generar únicamente las fuentes locales, usa `scripts/prepare-samsung.sh`; comprueba el hash y no instala el mod. Empieza con un checkout nuevo o mueve `work/samsung` si ya existe, para evitar superponer parches.

## Compilar la app opcional

```sh
export MANAGER_KEYSTORE=/ruta/privada/a/tu-clave.p12
export MANAGER_STORE_PASS=tu-password-local
bash manager/build.sh
```

La clave debe permanecer fuera del repositorio. Conserva la misma firma y aumenta versionCode para futuras actualizaciones. La APK se genera en `manager/build/` y no contiene componentes nativos del teclado ni modelos aprendidos.

La app declara Internet, estado de red, notificaciones y recepción de arranque para su tarea programada. No solicita root ni acceso a contactos o almacenamiento general.
