# Instalación y actualización

## Estado

El **módulo ZIP** es el producto principal. HoneyDelight Updates es opcional. La comprobación de compatibilidad, los checksums y la preparación local de ART del ZIP pasaron en una carpeta de prueba del Galaxy S25+ de desarrollo. La instalación/activación del módulo y el primer arranque siguen pendientes de validación; no se anuncia como instalación universal.

Cada paquete exige el APK exacto de Samsung, la compilación de Android, SDK 36 y arm64-v8a. El APK original de Samsung conserva su firma. El módulo compila localmente durante la instalación para usar la ruta y el ART del teléfono; puede tardar unos segundos. Repite la preparación si cambia la ruta del APK o los binarios de ART. El núcleo utiliza montajes reversibles de odex/vdex y librerías y mantiene los datos privados del teclado.

## 1. Directamente desde KernelSU o Magisk

1. Descarga el ZIP correspondiente a tu compilación cuando esté disponible en Releases.
2. Abre el gestor root → Módulos → Instalar desde almacenamiento.
3. Selecciona el ZIP y revisa el resultado. Un paquete incompatible debe rechazarse antes de activar el mod.
4. Sigue las indicaciones de tu gestor. La activación en el primer arranque de esta alpha aún requiere prueba.

Antes de migrar desde el instalador manual de desarrollo, hay que retirar sus montajes. El módulo rechaza rutas ya montadas para evitar superponer instalaciones. Los datos aprendidos se conservan.

## 2. Con HoneyDelight Updates

Instala la APK opcional, abre la app y activa los avisos si los quieres. Puede comprobar aproximadamente cada seis horas y descargar por redes sin medición. También permite buscar y descargar manualmente.

Guarda los módulos verificados en `Descargas/HoneyDelight`. La instalación se realiza desde el gestor root; la app no instala, no reinicia y no pide root. Puede funcionar sin el módulo instalado.

Mientras el repositorio sea privado y no haya releases públicas, no habrá descargas disponibles. Nunca se distribuye un token del propietario en la APK.

Para actualizar la app, instala una APK con la misma firma y un versionCode adecuado. La clave local de firma se mantiene fuera del repositorio.

## 3. Termux con root

Ejemplo para KernelSU; cambia el nombre del archivo por el ZIP compatible:

```sh
su -c '/data/adb/ksud module install /sdcard/Download/HoneyDelight-modulo-compatible.zip'
```

En Magisk:

```sh
su -c 'magisk --install-module /sdcard/Download/HoneyDelight-modulo-compatible.zip'
```

La ubicación del binario de KernelSU puede variar. Termux no es obligatorio.

## 4. ADB con root

```sh
adb push HoneyDelight-modulo-compatible.zip /data/local/tmp/honeydelight-module.zip
adb shell "su -c '/data/adb/ksud module install /data/local/tmp/honeydelight-module.zip'"
```

Usa la selección de dispositivo de ADB correspondiente a tu equipo si hay más de uno conectado.

## Recuperación

Desactiva o elimina `honeydelight` desde el gestor root. El módulo incluye un desmontaje limitado a sus propias entradas; no borra los datos aprendidos del teclado. Confirma la recuperación y el comportamiento tras reiniciar antes de distribuirlo ampliamente.

## Referencias

Formato y scripts del módulo: [Magisk](https://topjohnwu.github.io/Magisk/guides.html), [KernelSU](https://kernelsu.org/guide/module.html). Verificación de descargas: [digests de assets de GitHub](https://docs.github.com/en/rest/releases/assets).
