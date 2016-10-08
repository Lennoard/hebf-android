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

cp /data/data/com.androidvip.hebf/BackUps/build.prop /system/build.prop
chmod 644 /system/build.prop

rm -f /system/etc/init.d/02play
rm -f /system/etc/init.d/03zipalign
rm -f /system/etc/init.d/04net
rm -f /system/etc/init.d/04tcp
rm -f /system/etc/init.d/09yrolram
rm -f /system/etc/init.d/08wifi

busybox echo "#Backup restored $(date +%A) at $(date +%H:%M)" >> /system/etc/HEBF/app.log

mount -o ro,remount /system
