package com.androidvip.hebf.models

import android.graphics.drawable.Drawable

import java.io.Serializable

open class App(
        var packageName: String = "unknown",
        var label: String = "unknown",
        var isSystemApp: Boolean = false,
        var id: Int = 0)
    : Serializable {

    var versionName = "unknown"
    var sourceDir = ""
    var isDozeProtected: Boolean = false
    var isEnabled = true
    var isChecked = false
    @Transient
    var icon: Drawable? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as App?

        return this.packageName == that!!.packageName
    }

    override fun hashCode(): Int {
        return packageName.hashCode()
    }

    override fun toString(): String {
        return packageName
    }
}
