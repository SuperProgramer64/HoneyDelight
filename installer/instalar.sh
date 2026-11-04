#!/system/bin/sh
# Porte Gboard v2: el APK de Samsung NO se toca (firma intacta -> sobrevive soft reboot).
# El puente va solo en el codigo compilado (odex/vdex) y en lib/arm64, que Android no verifica por firma.
# Ejecutar como root:  su -c sh /sdcard/Download/KeyboardMod_v2/instalar.sh
PKG=com.samsung.android.honeyboard
SRC=$(cd "${0%/*}" && pwd)
W=/data/adb/gbport2
DATA=/data/user/0/$PKG
D=$DATA/files/gbport
APK=$(pm path $PKG | head -1 | cut -d: -f2)
APKDIR=${APK%/base.apk}
OAT=$APKDIR/oat/arm64

fail() { echo "ERROR: $*"; exit 1; }
[ "$(id -u)" = 0 ] || fail "hace falta root (su)"
[ -f "$APK" ] || fail "no encuentro Samsung Keyboard"
[ -f "$OAT/base.odex" ] || fail "el teclado no tiene odex (abre el teclado una vez y reintenta)"
for f in v.odex v.vdex libintegrated_shared_object.so es_us.dict en_us.dict; do
    [ -f "$SRC/$f" ] || fail "falta $SRC/$f"
done
grep -q " $OAT/base.odex " /proc/1/mountinfo && fail "ya esta instalado (usa desinstalar.sh primero)"

# Comprobacion de version: el odex se compilo para unos checksums concretos del APK.
VER=$(dumpsys package $PKG | grep -m1 versionName | tr -d ' ' | cut -d= -f2)
[ "$VER" = "5.9.30.97" ] || fail "version no compatible: $VER"
EXPECTED=$(cat "$SRC/samsung-apk.sha256") || fail "falta checksum de compatibilidad"
ACTUAL=$(sha256sum "$APK" | cut -d' ' -f1)
[ "$ACTUAL" = "$EXPECTED" ] || fail "APK no compatible: checksum distinto"

mkdir -p $W
cp "$SRC/v.odex" $W/base.odex || fail "copia odex"
cp "$SRC/v.vdex" $W/base.vdex || fail "copia vdex"
chown "$(stat -c %u:%g $OAT/base.odex)" $W/base.odex $W/base.vdex
chmod 644 $W/base.odex $W/base.vdex
chcon u:object_r:dalvikcache_data_file:s0 $W/base.odex $W/base.vdex

# lib/arm64 = libs originales de Samsung + motor de Gboard
[ -d $W/lib-arm64 ] && rm -r $W/lib-arm64
cp -a "$APKDIR/lib/arm64" $W/lib-arm64 || fail "copia lib"
cp "$SRC/libintegrated_shared_object.so" $W/lib-arm64/ || fail "copia motor"
chown -R system:system $W/lib-arm64; chmod 755 $W/lib-arm64 $W/lib-arm64/*.so
chcon -R u:object_r:apk_data_file:s0 $W/lib-arm64

# Diccionarios de Gboard (es-US + en-US) en los datos del teclado
U=$(stat -c %u $DATA)
mkdir -p $D/data $D/crash
cp "$SRC/es_us.dict" "$SRC/en_us.dict" $D/ || fail "copia diccionarios"
# No se importan historiales, contactos ni diccionarios personales de Gboard.
mkdir -p "$D/personal"
# Atajos de emoji de Gboard (palabra -> emoji), es y en.
SP=/data/data/com.google.android.inputmethod.latin/files/superpacks
mkdir -p $D/emoji
for l in es_us en_us; do
    f=$(ls $SP/emoji/*/$l.shortcuts $SP/bundled_emoji/*/$l.shortcuts 2>/dev/null | head -1)
    [ -n "$f" ] && cp "$f" $D/emoji/$l.shortcuts
done
echo "Emoji de Gboard: $(ls $D/emoji 2>/dev/null | tr '\n' ' ')"
# Erratas a quitar del historial aprendido (el motor las olvida al arrancar).
[ -f "$SRC/olvidar.txt" ] && cp "$SRC/olvidar.txt" $D/olvidar.txt && echo "Por olvidar: $(grep -vc '^#' "$SRC/olvidar.txt") palabras"
chown -R $U:$U $D; chmod -R u+rwX,go-rwx $D
chcon -R "$(ls -dZ $DATA/files | cut -d' ' -f1)" $D

for profile in /data/user/*/com.samsung.android.honeyboard; do
    [ -d "$profile/files" ] || continue
    [ "$profile" = /data/user/0/com.samsung.android.honeyboard ] && continue
    dest="$profile/files/gbport"
    uid=$(stat -c %u "$profile")
    mkdir -p "$dest/data" "$dest/crash" "$dest/personal" "$dest/emoji"
    cp "$SRC/es_us.dict" "$SRC/en_us.dict" "$dest/"
    chown -R "$uid:$uid" "$dest"
    chmod -R u+rwX,go-rwx "$dest"
    chcon -R "$(ls -dZ "$profile/files" | cut -d' ' -f1)" "$dest"
    echo "Modelos base preparados en $profile"
done

# Montajes reversibles. El base.apk NO se monta: queda el original firmado por Samsung.
mount --bind $W/base.odex $OAT/base.odex || fail "montaje odex"
mount --bind $W/base.vdex $OAT/base.vdex || { umount $OAT/base.odex; fail "montaje vdex"; }
mount --bind $W/lib-arm64 "$APKDIR/lib/arm64" || { umount $OAT/base.vdex; umount $OAT/base.odex; fail "montaje lib"; }
echo "$APKDIR" > $W/montado

am force-stop $PKG
echo "Listo. Abre un campo de texto. Comprueba: logcat -d -s GboardPort"
echo "Si el teclado funciona pero SIN correccion de Gboard, Android no acepto el odex (sin dano): usa desinstalar.sh."
