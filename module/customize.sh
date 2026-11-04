#!/system/bin/sh
[ "$BOOTMODE" = true ] || abort 'Instala desde el gestor root con Android iniciado.'
HD_MODULE=$MODPATH
. "$MODPATH/lib/compat.sh"
hd_validate_device || abort 'HoneyDelight rechazó un paquete incompatible. Samsung Keyboard se conserva.'
. "$MODPATH/lib/compile.sh"
ui_print 'Preparando código para este teléfono…'
hd_compile || abort 'No se pudo compilar el módulo para este dispositivo'
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/control.sh" 0 0 0755
ui_print 'HoneyDelight preparado. No se importan datos personales de Gboard.'
ui_print 'La activación al arrancar sigue siendo experimental. Conserva otro teclado disponible.'
