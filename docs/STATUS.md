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
