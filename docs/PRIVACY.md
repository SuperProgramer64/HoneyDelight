# Privacidad y datos locales

## Fuera del repositorio y de los paquetes

No se incluyen historiales entrenados, contactos, correos, coordenadas del dedo, diccionarios personales, instrucciones guardadas, logs del usuario, conversaciones, tokens, claves de firma ni fuentes tipográficas del dispositivo.

El empaquetador usa una lista explícita de cinco archivos de carga. Los tres componentes base de Gboard tienen hashes fijados para impedir sustituirlos accidentalmente por un modelo personal. Los otros archivos de la carpeta de entrada no se leen ni se copian al ZIP. Una prueba introduce un historial y un token ficticios y verifica su ausencia en el resultado.

## Aprendizaje del teclado

Cada instalación utiliza sus datos aprendidos locales. El módulo conserva la carpeta privada del mod, prepara únicamente modelos base y no importa historiales, contactos ni diccionarios personales desde Gboard. No se copia el aprendizaje del desarrollador a otra instalación ni de un perfil Android a otro.

El registro detallado de decisiones y toques está apagado por defecto. El usuario avanzado puede habilitarlo creando `files/gbport/debug` en los datos privados del teclado; retirarlo detiene el registro al cambiar de campo. Los logs activados pueden contener texto escrito y deben revisarse antes de compartirlos.

## HoneyDelight Updates

La app opcional no solicita root, contactos ni almacenamiento general. Utiliza Internet para consultar las releases del repositorio y descargar sus archivos. No envía texto, historiales, coordenadas ni modelos.

La elección del paquete se realiza localmente a partir de la compilación de Android y del hash del APK instalado de Samsung. El ZIP se descarga, se contrasta con el SHA-256 publicado por GitHub y después se guarda en Descargas/HoneyDelight. Las descargas automáticas se limitan a redes sin medición. Las notificaciones requieren permiso del usuario.

No se incrustan tokens de GitHub. Mientras el repositorio sea privado, el actualizador espera a que haya una publicación pública; no proporciona acceso privado mediante credenciales compartidas.

## Galaxy AI

Las reescrituras invocan los servicios Galaxy AI de Samsung con la configuración y condiciones del dispositivo. No se afirma que todas las funciones operen sin red o exclusivamente en el teléfono.
