#!/system/bin/sh
# HEBF
# Credits to: Paget96 and CALIBAN666
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

mount -o remount,rw /
mount -o remount,rw rootfs
mount -o remount,rw /system
mount -o remount,rw /data
mount -o remount,rw /cache

HEBF=/system/etc/HEBF/cleaner.log

 rm -f $HEBF
 busybox touch $HEBF

 echo "#Junk Files" | busybox tee -a $HEBF
 echo "" | busybox tee -a $HEBF
 echo "Cleaning..." | busybox tee -a $HEBF

 rm -f /cache/*.apk 2> /dev/null
 rm -f /cache/*.tmp 2> /dev/null
 rm -f /cache/recovery/* 2> /dev/null
 rm -f /data/*.log 2> /dev/null
 rm -f /data/*.txt 2> /dev/null
 rm -f /data/anr/*.* 2> /dev/null
 rm -f /data/backup/pending/*.tmp 2> /dev/null
 rm -f /data/cache/*.* 2> /dev/null
 rm -f /data/dalvik-cache/*.apk 2> /dev/null
 rm -f /data/dalvik-cache/*.tmp 2> /dev/null
 rm -f /data/log/*.* 2> /dev/null
 rm -f /data/local/*.apk 2> /dev/null
 rm -f /data/local/*.log 2> /dev/null
 rm -f /data/local/tmp/*.* 2> /dev/null
 rm -f /data/last_alog/* 2> /dev/null
 rm -f /data/last_kmsg/* 2> /dev/null
 rm -f /data/mlog/* 2> /dev/null
 rm -f /data/tombstones/* 2> /dev/null
 rm -f /data/system/dropbox/* 2> /dev/null
 rm -f /data/system/usagestats/* 2> /dev/null
 rm -rf /sdcard/LOST.DIR 2> /dev/null

 echo "" | busybox tee -a $HEBF
 echo "Cleaned" | busybox tee -a $HEBF

mount -o ro,remount /system
mount -o ro,remount /data
mount -o ro,remount /cache
