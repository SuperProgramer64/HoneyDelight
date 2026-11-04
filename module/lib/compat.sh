#!/system/bin/sh
hd_value() { sed -n "s/^$1=//p" "$HD_MODULE/compat.prop" | head -n 1; }
hd_validate() {
  [ -f "$HD_MODULE/compat.prop" ] || { echo 'Falta compat.prop del paquete local'; return 1; }
  [ "$(sha256sum "$1" | cut -d' ' -f1)" = "$(hd_value apk_sha256)" ] || { echo 'APK de Samsung no compatible'; return 1; }
  [ "$2" = "$(hd_value fingerprint)" ] || { echo 'Compilación Android/ART no compatible'; return 1; }
  [ "$3" = "$(hd_value abi)" ] && [ "$4" = "$(hd_value sdk)" ] || { echo 'Arquitectura o SDK no compatibles'; return 1; }
  (cd "$HD_MODULE/payload" && sha256sum -c SHA256SUMS) || { echo 'Payload incompleto o alterado'; return 1; }
}
hd_validate_device() {
  HD_APK=$(pm path --user 0 com.samsung.android.honeyboard | sed -n 's/^package://p' | head -n 1)
  [ -f "$HD_APK" ] || { echo 'Samsung Keyboard no está instalado'; return 1; }
  hd_validate "$HD_APK" "$(getprop ro.build.fingerprint)" "$(getprop ro.product.cpu.abi)" "$(getprop ro.build.version.sdk)"
}
