# Cambios

## 0.1.0-alpha — preparación, 2026-11-04

Primera preparación del código estable: puente Delight5, integración Samsung, mejoras de contexto, separación de palabras, estilos Galaxy AI, aprendizaje y reparación del lag del editor. Pruebas y limitaciones documentadas.

El prototipo PROCESS_TEXT de Corregir fue retirado; no está incluido.

### Preparación privada de distribución

Módulo ZIP como entrega principal, APK opcional de avisos/descarga, comprobación de variantes y SHA-256, exclusión de datos entrenados y logs apagados por defecto. Banner ámbar con One UI Sans renderizada y spark de Galaxy AI.

### Conexión y predicción

Arranque por perfil con reintentos, corrección del contexto reutilizado al consultar la misma palabra en frases distintas, filtro de repeticiones sin depender de la puntuación, ordenación de alternativas, rechazo de resultados atrasados y salida temprana de campos privados. Recursos opcionales de emoji dañados dejan de interrumpir la carga de los idiomas base.
