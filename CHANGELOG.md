# Cambios

## 0.1.0-alpha — preparación, 2026-11-04

Primera preparación del código estable: puente Delight5, integración Samsung, mejoras de contexto, separación de palabras, estilos Galaxy AI, aprendizaje y reparación del lag del editor. Pruebas y limitaciones documentadas.

El prototipo PROCESS_TEXT de Corregir fue retirado; no está incluido.

### Preparación privada de distribución

Módulo ZIP como entrega principal, APK opcional de avisos/descarga, comprobación de variantes y SHA-256, exclusión de datos entrenados y logs apagados por defecto. Banner ámbar con One UI Sans renderizada y spark de Galaxy AI.

### Conexión y predicción

Arranque por perfil con reintentos, corrección del contexto reutilizado al consultar la misma palabra en frases distintas, filtro de repeticiones sin depender de la puntuación, ordenación de alternativas, rechazo de resultados atrasados y salida temprana de campos privados. Recursos opcionales de emoji dañados dejan de interrumpir la carga de los idiomas base.

### Separación de expresiones frecuentes

Tabla local de 16 expresiones frecuentes para que su separación y tilde no dependan del presupuesto opcional de la búsqueda. Comprobaciones de nombres, enlaces y correos. Regresión nativa de la ruta completa con componentes base verificables y sin datos personales; las herramientas de prueba quedan fuera del DEX de producción.

### Erratas al separar y retroceso

Se excluye del contexto la composición que difiere por tilde o una letra respecto a la forma del campo. Se filtran duplicaciones entre composición antigua/corregida y se revisan erratas de una letra en la parte derecha de un corte sin alterar palabras válidas. La regresión nativa se amplía con los casos de `holaxomo` y `holacómo`.

### Paquetes de alpha

APK completo experimental con preparación asíncrona de modelos base, integridad comprobada y firma propia. HoneyDelight Updates pasa a `0.1.0-alpha.2` con la interfaz renovada, iconos adaptativos y el mismo certificado. README y banner actualizados; las restricciones de firma del teclado completo quedan documentadas.
