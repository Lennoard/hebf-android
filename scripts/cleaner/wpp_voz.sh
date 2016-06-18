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
# Spaaaaaaaaaaaaarta!

mount -o rw,remount /data

# Freedom gives you the ability to defend yourself, whatever that costs 
LIBERDADE="/data/media/0/WhatsApp/Media/WhatsApp Voice Notes"
ANTI_ERRO="/storage/emulated/0/WhatsApp/Media/WhatsApp Voice Notes"

rm -Rf "$LIBERDADE"
rm -Rf "$ANTI_ERRO" 

mount -o ro,remount /data
