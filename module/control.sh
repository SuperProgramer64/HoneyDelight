#!/system/bin/sh
set -eu
HD_MODULE=$(cd "${0%/*}" && pwd)
PKG=com.samsung.android.honeyboard
fail() { echo "$*" >&2; exit 1; }
[ "$(id -u)" = 0 ] || fail 'Hace falta root'
HD_APK=$(pm path --user 0 "$PKG" | sed -n 's/^package://p' | head -n 1)
DIR=${HD_APK%/*}
case "${1:-}" in
 deactivate)
  if [ -r "$HD_MODULE/runtime/apk-location" ]; then HD_APK=$(cat "$HD_MODULE/runtime/apk-location"); fi
  [ -n "$HD_APK" ] || exit 0
  case "$HD_APK" in /data/app/*|/system/*|/system_ext/*|/product/*) ;; *) fail 'Ruta de montaje no válida';; esac
  DIR=${HD_APK%/*}
  changed=0
  # Solo desmontar entradas procedentes de este módulo, sin afectar a otros mods.
  for target in "$DIR/lib/arm64" "$DIR/oat/arm64/base.vdex" "$DIR/oat/arm64/base.odex"; do
   if grep -F " $target " /proc/self/mountinfo | grep -qF '/honeydelight/'; then
    umount "$target" || umount -l "$target"
    changed=1
   fi
  done
  if [ "$changed" = 1 ]; then am force-stop "$PKG" || true; fi
  echo 'HoneyDelight desmontado; datos locales conservados'
  ;;
 activate)
  [ -f "$HD_APK" ] || fail 'Samsung Keyboard no disponible'
  [ ! -e "$HD_MODULE/disable" ] || fail 'Módulo desactivado'
  . "$HD_MODULE/lib/compat.sh"
  hd_validate_device || fail 'Compatibilidad rechazada'
  . "$HD_MODULE/lib/compile.sh"
  hd_runtime_current || hd_compile || fail 'No se pudo preparar ART'
  for target in "$DIR/oat/arm64/base.odex" "$DIR/oat/arm64/base.vdex" "$DIR/lib/arm64"; do
   [ -e "$target" ] || fail 'Falta una ruta original del teclado'
   if grep -qF " $target " /proc/self/mountinfo; then fail 'Ya existe un montaje en la ruta del teclado'; fi
  done
  mkdir -p "$HD_MODULE/runtime"
  # La copia privada de librerías Samsung nunca se empaqueta ni se publica.
  if [ -d "$HD_MODULE/runtime/lib-arm64" ]; then rm -r "$HD_MODULE/runtime/lib-arm64"; fi
  cp -a "$DIR/lib/arm64" "$HD_MODULE/runtime/lib-arm64"
  cp "$HD_MODULE/payload/libintegrated_shared_object.so" "$HD_MODULE/runtime/lib-arm64/"
  chown -R system:system "$HD_MODULE/runtime/lib-arm64"
  chmod -R 755 "$HD_MODULE/runtime/lib-arm64"
  chcon -R u:object_r:apk_data_file:s0 "$HD_MODULE/runtime/lib-arm64"
  chcon u:object_r:dalvikcache_data_file:s0 "$HD_MODULE/runtime/base.odex" "$HD_MODULE/runtime/base.vdex"
  for profile in /data/user/*/com.samsung.android.honeyboard; do
   [ -d "$profile/files" ] || continue
   dest="$profile/files/gbport"
   uid=$(stat -c %u "$profile")
   mkdir -p "$dest/data" "$dest/crash" "$dest/personal" "$dest/emoji"
   cp "$HD_MODULE/payload/es_us.dict" "$HD_MODULE/payload/en_us.dict" "$dest/"
   chown -R "$uid:$uid" "$dest"
   chmod -R u+rwX,go-rwx "$dest"
   chcon -R "$(ls -dZ "$profile/files" | cut -d' ' -f1)" "$dest"
  done
  mount --bind "$HD_MODULE/runtime/base.odex" "$DIR/oat/arm64/base.odex" || fail 'No se pudo montar odex'
  mount --bind "$HD_MODULE/runtime/base.vdex" "$DIR/oat/arm64/base.vdex" || { umount "$DIR/oat/arm64/base.odex"; fail 'No se pudo montar vdex'; }
  mount --bind "$HD_MODULE/runtime/lib-arm64" "$DIR/lib/arm64" || { umount "$DIR/oat/arm64/base.vdex"; umount "$DIR/oat/arm64/base.odex"; fail 'No se pudieron montar librerías'; }
  am force-stop "$PKG"
  echo 'HoneyDelight montado; no se importaron historiales, contactos ni diccionarios personales'
  ;;
 *) fail 'Uso: control.sh activate|deactivate';;
esac
