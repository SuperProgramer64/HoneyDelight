#!/system/bin/sh
# Quita el porte v2: desmonta odex/vdex/lib y reinicia el teclado. No borra tus datos ni toca el APK.
# Ejecutar como root:  su -c sh /sdcard/Download/KeyboardMod_v2/desinstalar.sh
PKG=com.samsung.android.honeyboard
W=/data/adb/gbport2
APKDIR=$(cat $W/montado 2>/dev/null)
[ -n "$APKDIR" ] || { APK=$(pm path $PKG | head -1 | cut -d: -f2); APKDIR=${APK%/base.apk}; }
for m in "$APKDIR/lib/arm64" "$APKDIR/oat/arm64/base.vdex" "$APKDIR/oat/arm64/base.odex"; do
    grep -q " $m " /proc/1/mountinfo && umount "$m" && echo "desmontado: $m" || umount -l "$m" 2>/dev/null
done
rm -f $W/montado
am force-stop $PKG
echo "Samsung Keyboard original restaurado."
