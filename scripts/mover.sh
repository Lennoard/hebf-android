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

busybox mkdir /data/data/com.androidvip.hebf/arquivos

cp /data/data/com.androidvip.hebf/adblock /data/data/com.androidvip.hebf/arquivos/adblock
rm -f /data/data/com.androidvip.hebf/adblock

cp /data/data/com.androidvip.hebf/no_adblock /data/data/com.androidvip.hebf/arquivos/no_adblock
rm -f /data/data/com.androidvip.hebf/no_adblock

IN4=/data/data/com.androidvip.hebf/cleaner
OUT4=/data/data/com.androidvip.hebf/cleaner
dd if="$IN4" of="$OUT4"
rm -f "$IN4"

cp /data/data/com.androidvip.hebf/zipalign /system/bin/zipalign
rm -f /data/data/com.androidvip.hebf/zipalign
busybox chmod 755 /system/bin/zipalign

cp /data/data/com.androidvip.hebf/sqlite3 /system/xbin/sqlite3
rm -f /data/data/com.androidvip.hebf/sqlite3
busybox chmod 755 /system/xbin/sqlite3

busybox touch /data/data/com.androidvip.hebf/hebf.hebf
busybox echo "[I] |$(date +%Y/%m/%d) $(date +%A), $(date +%H:%M:%S)| HEBF has been updated / set up" >> /data/data/com.androidvip.hebf/hebf.hebf

mount -o rw,remount /data
mount -o ro,remount /system
