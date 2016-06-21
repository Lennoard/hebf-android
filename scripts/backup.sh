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

backup=/data/data/com.androidvip.hebf/BackUps
rm -rf "$backup"
mkdir "/data/data/com.androidvip.hebf/BackUps"
mkdir "/data/data/com.androidvip.hebf/BackUps/init"

cp /system/build.prop /data/data/com.androidvip.hebf/BackUps
cp -R /system/etc/init.d /data/data/com.androidvip.hebf/BackUps/init

mount -o ro,remount /system
