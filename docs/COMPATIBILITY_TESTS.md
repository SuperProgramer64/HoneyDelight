# Pruebas de portabilidad con APK anteriores

4 de noviembre de 2026. Se trabajó en el Mac, sin usar el teléfono y sin cambiar los binarios publicados. Este informe añade evidencia a la [investigación de compatibilidad](COMPATIBILITY_RESEARCH.md); no amplía el soporte del ZIP actual.

## Resultado práctico

**5.9.20.87 y 5.9.30.95 son candidatos concretos para portar Core.** Conservan las doce firmas de Fluency invocadas directamente por el puente. Todavía requieren localizar los hooks de Samsung, adaptar el acceso al texto en composición y admitir cuatro DEX.

**5.6.10.31 ofrece una ruta de port más antigua**, pero usa `com.touchtype_fluency` en lugar de `com.microsoft.fluency`. Al remapear simbólicamente ese namespace coinciden las doce firmas de Fluency examinadas. Esto permite estudiar un adaptador; no demuestra que baste renombrar clases para obtener un teclado funcional.

Ninguna de las tres APK antiguas admite el módulo actual sin cambios. La coincidencia de firmas de métodos no verifica su semántica, JNI, acceso, reflexión, carga de clases ni funcionamiento sobre una ROM antigua.

## APK reales examinadas

Se verificaron criptográficamente las cuatro firmas. Todas coinciden con el certificado de las dos bases Samsung locales:

```text
34df0e7a9f1cf1892e45c056b4973cd81ccf148a4050d11aea4ac5a65f900a42
```

Las versiones mínima y objetivo proceden del manifiesto binario, no de las etiquetas del sitio de descarga. En particular, la página de 5.6.10.31 indicaba un mínimo distinto al del APK; se utiliza el dato del archivo.

| Samsung Keyboard | SDK mínimo / objetivo | DEX | Clases DEX indexadas | Contextos del parche actual que coinciden, por clase |
| :--- | :--- | :--- | :--- | :--- |
| 5.9.30.97, referencia | 33 / 36 | 3 | 21.378 | 16/16 |
| 5.9.30.95 | 33 / 36 | 4 | 37.930 | 1/16 |
| 5.9.20.87 | 33 / 35 | 4 | 37.450 | 1/16 |
| 5.6.10.31 | 28 / 31 | 4 | 29.727 | 0/16 |

El preparador de CRC del módulo actual enumera tres DEX de forma fija. Las tres bases antiguas examinadas contienen cuatro: además de adaptar hooks, hay que revisar empaquetado, compilación y CRC sin omitir el cuarto archivo.

El número de clases indexadas coincide con la suma de `class_defs_size` de todos los DEX de cada APK. Esto comprueba que el análisis no omitió el cuarto DEX. Un contexto coincidente significa que el fragmento se acepta en `git apply --check`; no que deba aplicarse aisladamente o que el teclado ya sea compatible.

Procedencia: 5.9.30.95 y 5.9.30.97 son las bases locales del proyecto. Para las otras se usaron las descargas de [5.6.10.31](https://www.androidapksbox.com/apk/samsung-keyboard-5-6-10-31-561031300-old-apk/) y [5.9.20.87](https://d-01.aabstatic.com/1124/samsung_keyboard_5.9.20.87_androidapksbox.apk), comprobando después paquete, versión, hash y certificado. Los hashes exactos están en [apk-inventory.json](compatibility/apk-inventory.json). Los APK y el smali propietario no se incorporan al repositorio.

Se intentó obtener 5.8.20.12, pero la descarga de APKMirror no estuvo disponible mediante el acceso utilizado. **No figura como APK probada.**

## Contratos del puente

Se desensambló el DEX del puente actual y se buscaron sus referencias directas a Fluency y al adaptador `Qi.c`. El resultado fue:

| Base | Fluency: firmas declaradas | Adaptador actual de composición `Qi.c.g()` | Interpretación |
| :--- | :--- | :--- | :--- |
| 5.9.30.97 | 12/12 | Presente | Control del análisis. |
| 5.9.30.95 | 12/12 | Clase ausente con ese nombre | El núcleo conserva contratos; la integración obfuscada necesita adaptación. |
| 5.9.20.87 | 12/12 | Clase ausente con ese nombre | Candidato para estudiar el port hacia Android 15; el manifiesto objetivo no certifica compatibilidad con One UI 7. |
| 5.6.10.31 | 0/12 con el namespace actual; 12/12 tras remapeo simbólico | Clase ausente con ese nombre | Hace falta una variante del adaptador y de los hooks. |

Las doce referencias abarcan construcción/lectura de predicciones, la llamada al predictor, secuencias, términos, historial de toques, puntos y geometría de teclas. No constituyen toda la API del teclado ni un porcentaje de precisión de sugerencias.

En 5.9.30.95, ocho clases exactas del parche no aparecen, siete tienen contextos distintos y una coincide. En 5.9.20.87 son trece ausentes, dos contextos distintos y una coincidencia. En 5.6.10.31 son catorce ausentes y dos contextos distintos. Una clase obfuscada ausente no demuestra que la función haya desaparecido: requiere localizar su equivalente. El aplicador publicado busca en los dos primeros DEX; el auditor de investigación examina todos.

Se localizaron interfaces candidatas para el adaptador de composición mediante la firma de entrada de carácter/punto/estado de mayúscula: `r40.e` en 5.9.30.95, `g40.c` en 5.9.20.87 y `com.samsung.android.honeyboard.v.h.d.j.a.c` en 5.6.10.31. Sus métodos y nombres difieren; falta verificar cuál devuelve exactamente la composición. Son candidatos de análisis y no remapeos aplicados.

La interfaz `Qi.c` corresponde al acceso al texto en composición, no a una capacidad de Galaxy AI. No se debe inventar una clase vacía con ese nombre para silenciar el error: hay que enlazarla con la representación real del texto de la versión destino.

## Simulaciones y regresiones ejecutadas

- **19 grupos JVM del puente actual: pasaron.** Comprueban, entre otros, contexto, privacidad, separación, respuestas atrasadas e inicio por perfil. Emplean los dobles de prueba del proyecto.
- **19 grupos JVM en una copia con namespace antiguo: pasaron.** Se copió el código propio a una carpeta de laboratorio y se reemplazaron las referencias `com.microsoft.fluency` por `com.touchtype_fluency`, incluidos los stubs y las pruebas. Compilación Java/D8 y regresiones pasaron. Esta copia conserva otros dobles, incluido el adaptador de composición: no equivale a ejecutar las clases nativas de Samsung 5.6.10.31.
- **5 pruebas del auditor: pasaron.** Incluyen clases en el cuarto DEX, contextos modificados, descriptores de miembros distintos, clases duplicadas y constructores que no se pueden heredar. El auditor no modifica los destinos.
- **9 pruebas controladas del validador publicado: pasaron.** Solo acepta la referencia; rechaza las tres APK antiguas, los SDK 33/34/35, una ABI distinta y otro fingerprint. Los casos de SDK cambian una condición de entrada para aislar esa comprobación; no representan dispositivos reales. No se ejecutó instalación, activación ni compilación ART.

Los resultados del remapeo y del validador se conservan en [compatibility/](compatibility/). Todos los informes de comparación marcan `runtime_validated=false`.

## Alcance de la emulación

No se ejecutó un emulador ni una ROM One UI antigua. El entorno no tenía un AVD o emulador instalado y no ofrecía aceleración de hipervisor en la comprobación realizada. Las pruebas dinámicas de este informe son JVM en el Mac.

Además, un AVD con aspecto Galaxy seguiría ejecutando Android estándar: Samsung explica que sus skins no incorporan One UI. Por eso ejecutar allí una APK no demostraría compatibilidad con el framework ni los permisos de una ROM Samsung. [Documentación oficial de Samsung](https://developer.samsung.com/galaxy-emulator-skin/guide.html).

## Qué falta para declarar una versión compatible

Para 5.9.20.87 o 5.9.30.95: localizar hooks equivalentes y composición, adaptar el empaquetado a cuatro DEX, preparar ART contra las dependencias reales de la ROM y probar arranque, escritura, espacio, retroceso, aprendizaje y recuperación. La edición Core debe excluir trabajo de IA cuando no existe esa capacidad.

Para 5.6.10.31 se añade el port del namespace y la auditoría de APIs si se pretende bajar de Android 13. Su mínimo declarado API 28 no rebaja el mínimo del puente actual, compilado con `--min-api 33`. El SDK objetivo de cada APK tampoco identifica por sí solo la ROM donde fue extraído.

Prioridad sugerida: 5.9.30.95 para estudiar una base cercana; 5.9.20.87 para el objetivo Android 15; 5.6.10.31 para una rama Legacy Core separada. El módulo System Base necesitaría seleccionar la base firmada adecuada y aplicar el port correspondiente, no imponer 5.9.30.97 a todos los teléfonos.

## Repetir la comparación

Con JDK 17 y apktool 2.12.1, descompila cada APK original localmente:

```sh
java -jar "$APKTOOL_JAR" d -r /ruta/teclado-original.apk -o work/compatibility/teclado
python3 scripts/audit-port-target.py \
  --decoded work/compatibility/teclado \
  --output work/compatibility/teclado-audit.json
python3 scripts/tests/test_port_audit.py
```

Para comparar referencias directas, añade `--bridge-decoded` apuntando a una carpeta con el DEX del puente desensamblado. Sin esa opción solo se comprueban las clases/contextos del parche. El auditor genera evidencia estática y no instala, parchea ni certifica el APK.
