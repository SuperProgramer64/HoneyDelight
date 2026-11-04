# HoneyDelight 0.1.0-alpha · borrador privado

**Tu Samsung Keyboard, con un toque de Delight.**

Primera preparación del mod que conecta Delight5 con la interfaz de Samsung Keyboard y amplía las herramientas de Galaxy AI.

Incluye mejoras de contexto, separación de palabras pegadas, filtros contra sugerencias repetidas, gestos, mayúsculas, aprendizaje local y una reparación de las esperas del editor de reescritura.

## Descargas

- **Módulo ZIP**: entrega principal. Este paquete corresponde al Galaxy S25+ SM-S936B de desarrollo, Android 16 / One UI 8.5 y Samsung Keyboard 5.9.30.97. La instalación valida APK, compilación y checksums.
- **HoneyDelight Updates APK**: complemento opcional para consultar releases públicas, avisar y descargar por redes sin medición. No requiere root ni lee modelos aprendidos. Las actualizaciones de la app conservan su firma.

## Estado de prueba

La construcción completa, la validación del paquete y la preparación local de ART en una carpeta de prueba del dispositivo pasaron. La activación del módulo, el primer arranque, la recuperación y una descarga de release de extremo a extremo aún necesitan prueba.

El ZIP no contiene historiales, contactos, correos, coordenadas, instrucciones personales, logs ni claves del usuario. Utiliza componentes base; el aprendizaje de cada instalación se mantiene local.

Este borrador es privado. No anuncia compatibilidad universal ni superioridad general frente a otros teclados. El prototipo retirado del menú Corregir no está incluido.

Antes de publicar: afinar sugerencias bilingües, probar instalación limpia/arranque y revisar la distribución de componentes externos.

## Licencia

Código original bajo MIT, © 2026 SuperProgramer64. Los componentes externos, el contexto preexistente de los parches y los elementos gráficos derivados de Samsung conservan sus propios derechos; véanse `LICENSE` y `THIRD_PARTY_NOTICES.md`, adjuntos al borrador.

## Revisión de sugerencias y conexión

- Inicio con el directorio del perfil actual y reintentos tras fallos temporales.
- Actualización del prefijo al cambiar contexto para evitar reutilizar la puntuación de otra frase.
- Filtro de sugerencias repetidas independiente de su puntuación y ordenación de alternativas.
- Descarte de respuestas atrasadas al cambiar de palabra/campo.
- Exclusión temprana de contraseñas y tolerancia a atajos opcionales de emoji dañados.
- Diecisiete regresiones JVM, ocho separaciones y seis cambios de contexto comprobados; comparación de 28 frases sin datos personales.

- Conservación de separaciones ya comprobadas al agotarse el presupuesto opcional.
- Revisión final de palabras desconocidas o cortes incompletos al pulsar espacio en Google.
- Filtro de alternativas pegadas cuando se acepta una separación.
- La revisión de estos tres ajustes está instalada; su comprobación interactiva final está pendiente.

## Expresiones frecuentes

Tabla local de 16 expresiones: saludos, despedidas y fórmulas frecuentes. Mantiene el separador general para los demás casos. La revisión de prioridad de cortes que empeoró la barra fue retirada.

Diecisiete grupos JVM y 80/80 casos en la nueva regresión nativa. La comparación ampliada de 84 casos deja dos errores bilingües documentados (`erroes → errors` con coordenadas simuladas). La comprobación en Google pasó seis combinaciones tanto en la barra como al pulsar espacio, incluida `hastamanana → hasta mañana`.

Los controles adicionales en Google confirmaron `gustamucho → gusta mucho`, `programacion → programación` y la conservación de `cocodrilo`. `holacomo` todavía puede quedar como `holacómo`; es un fallo abierto del separador general.
