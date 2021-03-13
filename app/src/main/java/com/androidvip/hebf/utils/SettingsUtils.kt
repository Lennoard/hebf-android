package com.androidvip.hebf.utils

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * Helper class for reading / writing on System settings
 * This class may not be able to write "Secure" or "Global" settings
 *
 * @param applicationContext non-null
 */
class SettingsUtils(val applicationContext: Context?) {
    private val cr: ContentResolver
    private val canWriteSettings: Boolean

    init {
        requireNotNull(applicationContext) { "Context may not be null" }
        cr = applicationContext.contentResolver
        canWriteSettings = Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(applicationContext)
    }

    /**
     * Updates a system setting as an integer (or creates if it is not already present
     *
     * @param setting The setting to modify / create
     * @param value The integer value to set
     * @return true if the operation is successfully complete, false otherwise
     */
    fun putInt(setting: String, value: Int): Boolean {
        return if (canWriteSettings) {
            Settings.System.putInt(cr, setting, value)
        } else false
        // We are not allowed to do this operation, return false
    }

    /**
     * Retrieves a system settings value as an integer.
     *
     * @param setting The setting to retrieve
     * @param defaultValue Value to return if the setting is not defined.
     * @return The setting's current value, or {@param defaultValue} if it is not defined
     * or not a valid integer.
     */
    fun getInt(setting: String, defaultValue: Int): Int {
        return Settings.System.getInt(cr, setting, defaultValue)
    }

    /**
     * Updates a system setting as a String (or creates if it is not already present
     *
     * @param setting The setting to modify / create
     * @param value The String value to set
     * @return true if the operation is successfully complete, false otherwise
     */
    fun putString(setting: String, value: String): Boolean {
        return if (canWriteSettings) {
            Settings.System.putString(cr, setting, value)
        } else false
        // We are not allowed to do this operation, return false
    }

    /**
     * Retrieves a system settings value as a String.
     *
     * @param setting The setting to retrieve
     * @return The setting's current value
     */
    fun getString(setting: String): String {
        return Settings.System.getString(cr, setting)
    }

    /**
     * Changes the screen backlight brightness
     *
     * @param level the screen brightness level
     * @return true if the operation was successfully completed, false otherwise
     * @throws IllegalArgumentException if the level is not between 0 and 255
     */
    fun changeBrightness(level: Int = 150): Boolean {
        // Check level values before start
        if (level < 0 || level > 255) {
            Logger.logWarning("The brightness level must be between 0 and 255. Got $level.", applicationContext)
        }
        // Check if the brightness is on auto mode, and if so, change to manual
        val brightnessMode = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
        if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            if (!putInt(Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)) {
                // Couldn't change brightness mode, finish returning false
                return false
            }
        }
        return putInt(Settings.System.SCREEN_BRIGHTNESS, level)
    }
}
