# Licencia y componentes externos

El archivo [LICENSE](LICENSE) aplica la licencia MIT a los aportes originales de HoneyDelight, © 2026 SuperProgramer64: puente de integración, lógica propia de predicción y edición, aplicación de actualizaciones, herramientas, scripts, pruebas y documentación originales.

MIT permite usar, modificar y redistribuir esos aportes, incluso comercialmente, conservando el aviso de copyright y la licencia. Véase el [texto de MIT publicado por OSI](https://opensource.org/license/mit).

## Alcance de terceros

| Elemento | Alcance |
| --- | --- |
| Samsung Keyboard y Galaxy AI | Su APK, implementaciones, servicios y recursos conservan sus términos y derechos. MIT no concede permiso para redistribuirlos. El APK original queda fuera del árbol de código. |
| Motor Delight5 y componentes base de Gboard | La biblioteca nativa y los diccionarios base conservan sus términos y derechos. Están fuera del árbol de código; el ZIP experimental privado contiene los componentes indicados por `patches/base-components.json`. Su inclusión no los convierte en MIT. |
| Interfaces JNI y stubs de compatibilidad | Las declaraciones describen APIs externas; no incluyen sus implementaciones. MIT cubre únicamente los aportes originales de integración y no transfiere derechos sobre interfaces o implementaciones preexistentes. |
| Parche smali | MIT cubre los cambios originales de HoneyDelight. Las líneas preexistentes de Samsung usadas como contexto o eliminadas por el diff conservan sus derechos. |
| Identidad visual | `assets/spark-blue.svg`, `assets/banner.svg` y `assets/banner.png` contienen gráficos derivados del spark de Samsung y, en el banner, trazados de One UI Sans. Estos elementos de terceros quedan fuera de la concesión MIT. No se distribuye el archivo de fuente. |
| Herramientas de construcción | Android SDK, JDK, apktool, Magisk y KernelSU son dependencias externas con sus propias licencias. La licencia de HoneyDelight no modifica sus términos. |

Las marcas Samsung, Galaxy AI, Gboard y Microsoft pertenecen a sus respectivos titulares. El proyecto es independiente. La licencia MIT no concede derechos sobre marcas, fuentes o recursos de terceros.

## Distribución

Conserva `LICENSE` y este documento junto al código y los binarios originales de HoneyDelight. Antes de publicar paquetes que incluyan componentes externos, verifica por separado sus permisos de distribución y avisos aplicables. Este documento delimita la concesión de HoneyDelight; no sustituye los términos originales de esos componentes.
