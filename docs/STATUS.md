# Estado de la alpha

Verificado el 4 de noviembre de 2026.

- Ocho grupos de pruebas locales pasaron en la revisión estable.
- Ocho casos de separación se verificaron con el motor real en el dispositivo.
- Veintiséis frases de contexto conservaron la referencia de la revisión anterior.
- El usuario confirmó menor lag en los campos del editor tras retirar una espera circular de Samsung de aproximadamente 500 ms.
- La última revisión estable fue reinstalada después de retirar el experimento de menú.

## Límites conocidos

- Validación en un solo modelo y una versión de Samsung Keyboard.
- `quet` todavía puede sugerir `quiet` por mezcla bilingüe.
- La decisión final puede ir una letra por detrás al escribir rápido.
- No hay validación completa de sustitución/selección en todas las apps.
- Los modelos del perfil secundario están preparados; falta ampliar la prueba efectiva por perfil.
- Existe un cierre histórico por agotamiento de hilos cuyo origen no quedó resuelto.
- El arnés nativo emite avisos `Invalid frame index`; no se consideran solucionados.
- No se ha comprobado la persistencia de esta instalación tras reiniciar.
- No se acredita superioridad general frente a Gboard ni una cifra global de precisión.

La muestra espacial temporal usó 100 palabras de entrenamiento y 100 de evaluación: centros nominales, toques reales y centros personalizados obtuvieron 80 aciertos cada uno. La adaptación personalizada permanece fuera del teclado activo.

## Preparación de distribución

Se añadió una novena regresión de privacidad, un módulo ZIP con validación y un actualizador opcional. El registro detallado está apagado por defecto y el instalador preparado no importa datos personales de Gboard. El módulo local pasó la validación de compatibilidad e integridad en el dispositivo; la activación y el primer arranque quedan pendientes.

## Revisión de conexión y sugerencias · 4 de noviembre de 2026

Se reprodujo un arranque fallido del perfil duplicado: el proceso buscaba los modelos en el usuario principal antes de recibir el contexto del servicio. Ahora el puente espera ese contexto, usa su directorio y reintenta fallos de inicialización con una pausa de cinco segundos.

Las dieciséis regresiones JVM pasaron. En el dispositivo, seis casos de cambio de contexto, ocho casos de separación y una comparación de 28 frases con diccionarios base pasaron la revisión. La comparación corrigió el cambio indebido de `yo supero` a `yo superó` y recuperó `cómo` tras `¿`; los otros 26 resultados de decisión se conservaron. Esta muestra no mide precisión global.

También se filtran expansiones repetidas aunque su puntuación sea alta, se ordenan alternativas por puntuación finita, se toleran atajos de emoji dañados y se descartan respuestas de palabras/campos anteriores. Los campos de contraseña salen de la ruta de Delight antes de capturar contexto.

La revisión se instaló mediante los montajes de desarrollo, conservando una copia anterior y sin reiniciar el teléfono. La verificación interactiva de ambos perfiles está en curso; esto no valida el primer arranque del módulo.

La comprobación inicial en Google detectó dos fallos interactivos (`erroes` y `buenosdias`) que el arnés aislado no mostraba. Una repetición confirmó tanto el resultado pegado `buenosdías` como el resultado separado `buenos días`; no se considera solucionado solo por esa repetición. Se añadieron pruebas que reproducen la pérdida de un corte válido cuando expira el presupuesto y la reutilización de decisiones incompletas al pulsar espacio. Ambas pasan con la revisión instalada más reciente. La comprobación interactiva posterior figura en las secciones siguientes.

Cuando un corte ha sido aceptado, la barra omite alternativas pegadas que contrarían ese corte. Esto evita mostrar `Buenosdigas` o `buenosdirás` junto a una separación validada, sin aplicar el filtro a completaciones normales ni al espacio de puntuación.

## Expresiones frecuentes y prueba de la barra

La prueba inicial en Google corrigió cinco de seis combinaciones en la barra y al pulsar espacio: `buenasnoches`, `buenastardes`, `tequiero`, `porfavor`, `buenosdias`. `hastamanana` quedó pegada. Una revisión experimental de prioridad de cortes empeoró la barra y fue retirada; no forma parte de este árbol.

Se añadió una tabla local de 16 expresiones frecuentes en `CommonBoundaries.java`, independiente del presupuesto de llamadas nativas. Conserva mayúsculas y tildes; omite nombres con mayúsculas internas, direcciones y correos. El filtro de palabra válida de `Split.wins` se conserva. Las demás expresiones usan el separador general del motor.

Pasaron 17 grupos JVM. La nueva regresión nativa de la ruta completa pasó 80/80 casos (20 entradas, dos repeticiones y dos variantes de coordenadas), con diccionarios base y sin datos personales. Una comparación adicional de 84 casos obtuvo 82 resultados esperados: los dos fallos fueron `erroes → errors` con coordenadas simuladas. El problema bilingüe sigue pendiente. Estas muestras no acreditan una cifra global de precisión.

Esta revisión se instaló por los montajes de desarrollo. En Google, las seis combinaciones `buenasnoches`, `buenastardes`, `tequiero`, `porfavor`, `hastamanana` y `buenosdias` mostraron el límite entre palabras en la barra y se confirmaron separadas al pulsar espacio. La posición a la izquierda cuando solo hay una sugerencia sigue siendo el diseño nativo de Samsung; no se modifica el orden de autocorrección para forzar un centrado.

### Controles finales en Google

En la revisión con expresiones locales pasaron las seis combinaciones tanto en la barra como al confirmar con espacio. Los controles adicionales conservaron `cocodrilo`, corrigieron `programacion → programación` y separaron `gustamucho → gusta mucho`. `holacomo` quedó `holacómo` en esta etapa. La sección posterior registra su reparación y la comprobación interactiva.

## Composición corregida y retroceso

Se reprodujo en Google que `holacomo` y `holaxomo` podían terminar como `holacómo`, y que al retroceder la barra podía concatenar la composición corregida y la antigua. Una regresión demostró que `EditorContext` no excluía del contexto el token cuando la forma del campo difería por tilde o una letra. Se corrige también el caso de cursor dentro de esa composición, conservando el texto realmente posterior.

El separador ahora admite corregir una letra en la parte derecha solo con la decisión y puntuación del motor, conserva palabras válidas, normaliza la mayúscula inicial de prefijos y descarta concatenaciones entre la forma corregida y la antigua. La búsqueda prioriza una palabra izquierda completa y no considera que poner tilde a una forma pegada baste para declararla válida. Se ajustó la referencia cuando el motor no devuelve puntuación del literal; las partes deben seguir pasando su validación individual.

Pasaron 19 grupos JVM y la regresión nativa ampliada, 92/92 casos (23 entradas, dos repeticiones y dos variantes de coordenadas). Incluye `holaxomo`, `holacomo` y `holacómo` sin añadir esas formas a la tabla de expresiones frecuentes. La revisión está instalada y la comprobación en Google pasó para `holacomo` y `holaxomo`: la barra mostró `hola cómo`, espacio lo confirmó separado y retroceso conservó la separación sin las concatenaciones reproducidas.

## Verificación final de paquetes

La repetición nativa posterior obtuvo **90/92**, frente al 92/92 anterior. Fallaron `holaxomo` y `holacomo` en la primera ronda con coordenadas, bajo el presupuesto de 55 ms; ambas pasaron en la otra ronda. La causa temporal precisa no está aislada. No se considera estable al 100 % esa ruta de separación. No se cambió el motor para ocultar esta repetición.

La auditoría del APK completo comprobó 4.993 entradas originales idénticas, hashes de componentes base y ausencia de clases de prueba. Se verificaron firma y alineación; instalación y ejecución completas siguen sin validar. El módulo excluye las clases de preparación propias del APK. HoneyDelight Updates 0.1.0-alpha.2 se instaló sobre la versión anterior y su pantalla principal mostró el identificador de compatibilidad esperado.
