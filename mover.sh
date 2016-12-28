#!/system/bin/sh
# HEBF
#=======================================================================#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#  GNU General Public License for more details.
#  You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
#=======================================================================#

mount -o rw,remount /system
mount -o rw,remount /data

mkdir /data/data/com.androidvip.hebf/LMK
mkdir /data/data/com.androidvip.hebf/limpador
mkdir /data/data/com.androidvip.hebf/arquivos
mkdir /data/data/com.androidvip.hebf/hebf_logs

busybox touch /data/data/com.androidvip.hebf/hebf_logs/app.log

IN1=/data/data/com.androidvip.hebf/adblock
OUT1=/data/data/com.androidvip.hebf/arquivos/adblock
dd if="$IN1" of="$OUT1"
rm -f "$IN1"

IN2=/data/data/com.androidvip.hebf/no_adblock
OUT2=/data/data/com.androidvip.hebf/arquivos/no_adblock
dd if="$IN2" of="$OUT2"
rm -f "$IN2"

IN3=/data/data/com.androidvip.hebf/adblock_on.sh
OUT3=/data/data/com.androidvip.hebf/arquivos/adblock_on.sh
dd if="$IN3" of="$OUT3"
rm -f "$IN3"

IN3a=/data/data/com.androidvip.hebf/adblock_off.sh
OUT3a=/data/data/com.androidvip.hebf/arquivos/adblock_off.sh
dd if="$IN3a" of="$OUT3a"
rm -f "$IN3a"

IN4=/data/data/com.androidvip.hebf/cleaner
OUT4=/data/data/com.androidvip.hebf/limpador/cleaner
dd if="$IN4" of="$OUT4"
rm -f "$IN4"

IN5=/data/data/com.androidvip.hebf/cleaner_etc
OUT5=/data/data/com.androidvip.hebf/limpador/cleaner_etc
dd if="$IN5" of="$OUT5"
rm -f "$IN5"

IN6=/data/data/com.androidvip.hebf/fstrim
OUT6=/system/bin/fstrim
dd if="$IN6" of="$OUT6"
rm -f "$IN6"
chmod 755 /system/bin/fstrim

IN7=/data/data/com.androidvip.hebf/zipalign
OUT7=/system/bin/zipalign
dd if="$IN7" of="$OUT7"
rm -f "$IN7"
chmod 755 /system/bin/zipalign
rm -f /system/xbin/zipalign

IN14=/data/data/com.androidvip.hebf/thumb
OUT14=/data/data/com.androidvip.hebf/limpador/thumb
dd if="$IN14" of="$OUT14"
rm -f "$IN14"

IN15=/data/data/com.androidvip.hebf/get_lmk
OUT15=/data/data/com.androidvip.hebf/LMK/get_lmk
dd if="$IN15" of="$OUT15"
rm -f "$IN15"

IN16=/data/data/com.androidvip.hebf/sqlite3
OUT16=/system/bin/sqlite3
dd if="$IN16" of="$OUT16"
rm -f "$IN16"
chmod 755 /system/bin/sqlite3

busybox touch /data/data/com.androidvip.hebf/hebf.hebf
busybox echo "" >> /data/data/com.androidvip.hebf/hebf_logs/app.log
busybox echo "Created file by mover.sh or user force copy $(date +%A), $(date +%H:%M)" >> /data/data/com.androidvip.hebf/hebf.hebf
busybox echo "Created file by mover.sh or user force copy $(date +%A), $(date +%H:%M)" >> /data/data/com.androidvip.hebf/hebf_logs/app.log
busybox echo "" >> /data/data/com.androidvip.hebf/hebf_logs/app.log
mount -o rw,remount /data
mount -o ro,remount /system

