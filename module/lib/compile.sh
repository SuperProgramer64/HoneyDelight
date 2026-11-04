#!/system/bin/sh
hd_art_stamp() {
 sha256sum /apex/com.android.art/bin/dex2oat64 /apex/com.android.art/lib64/libart.so
}
hd_runtime_current() {
 [ -f "$HD_MODULE/runtime/base.odex" ] && [ -f "$HD_MODULE/runtime/base.vdex" ] &&
 [ "$(cat "$HD_MODULE/runtime/apk-location" 2>/dev/null)" = "$HD_APK" ] &&
 [ "$(cat "$HD_MODULE/runtime/art-stamp" 2>/dev/null)" = "$(hd_art_stamp)" ]
}
hd_compile() {
 mkdir -p "$HD_MODULE/runtime"
 CONTEXT='PCL[]{PCL[/system/framework/SmpsManager.jar]#PCL[/system_ext/framework/androidx.window.extensions.jar]#PCL[/system_ext/framework/androidx.window.sidecar.jar]}'
 /apex/com.android.art/bin/dex2oat64 --dex-file="$HD_MODULE/payload/mod.zip" --dex-location="$HD_APK" --oat-file="$HD_MODULE/runtime/new.odex" --oat-location="${HD_APK%/*}/oat/arm64/base.odex" --instruction-set=arm64 --compiler-filter=speed --class-loader-context="$CONTEXT" -j2 || return 1
 CLASSPATH="$HD_MODULE/tools.dex" /system/bin/app_process /system/bin io.honeydelight.tools.CrcPatch "$HD_APK" "$HD_MODULE/payload/mod.zip" "$HD_MODULE/runtime/new.odex" "$HD_MODULE/runtime/base.odex.next" || return 1
 CLASSPATH="$HD_MODULE/tools.dex" /system/bin/app_process /system/bin io.honeydelight.tools.CrcPatch "$HD_APK" "$HD_MODULE/payload/mod.zip" "$HD_MODULE/runtime/new.vdex" "$HD_MODULE/runtime/base.vdex.next" || return 1
 mv "$HD_MODULE/runtime/base.odex.next" "$HD_MODULE/runtime/base.odex"
 mv "$HD_MODULE/runtime/base.vdex.next" "$HD_MODULE/runtime/base.vdex"
 chmod 644 "$HD_MODULE/runtime/base.odex" "$HD_MODULE/runtime/base.vdex"
 chcon u:object_r:dalvikcache_data_file:s0 "$HD_MODULE/runtime/base.odex" "$HD_MODULE/runtime/base.vdex"
 printf '%s\n' "$HD_APK" > "$HD_MODULE/runtime/apk-location"
 hd_art_stamp > "$HD_MODULE/runtime/art-stamp"
 rm -f "$HD_MODULE/runtime/new.odex" "$HD_MODULE/runtime/new.vdex"
}
