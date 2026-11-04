#!/system/bin/sh
HD_MODULE=${0%/*}
count=0
while [ "$(getprop sys.boot_completed)" != 1 ]; do
  count=$((count + 1)); [ "$count" -ge 120 ] && exit 1
  sleep 1
done
[ -e "$HD_MODULE/disable" ] && exit 0
sh "$HD_MODULE/control.sh" activate > "$HD_MODULE/status.log" 2>&1 || touch "$HD_MODULE/disable"
