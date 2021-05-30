package com.androidvip.hebf.ui.internal

import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.androidvip.hebf.R
import com.androidvip.hebf.createVectorDrawable
import com.androidvip.hebf.utils.Logger
import com.androidvip.hebf.utils.Utils
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_translate.*
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class TranslateActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translate)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(createVectorDrawable(R.drawable.ic_arrow_back))
        }

        translationsGetXmlFile.setOnClickListener { copyTranslateFile() }

        translationsGitHub.setOnClickListener {
            Utils.webPage(this, "https://github.com/Lennoard/HEBF/blob/master/app/src/main/res/values/strings.xml")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> { finish() }
        }
        return super.onOptionsItemSelected(item)
    }


    fun openTranslatorPage(v: View) {
        v.tag?.let {
            runCatching {
                Utils.webPage(this, it as String)
            }
        }
    }

    private fun copyTranslateFile() {
        val assetManager = assets
        if (assetManager != null) {
            val files: Array<String>?
            try {
                files = assetManager.list("Traducao")
                for (filename in files!!) {
                    val inputStream: InputStream? = assetManager.open("Traducao/$filename")
                    val outputStream: OutputStream
                    outputStream = FileOutputStream("${Environment.getExternalStorageDirectory()}/$filename")
                    copyFile(inputStream!!, outputStream)
                    inputStream.close()
                    outputStream.close()
                }
                val snackbar = Snackbar.make(translationsGetXmlFile, "Check your internal storage for the file", Snackbar.LENGTH_INDEFINITE)
                snackbar.setAction("OK") { snackbar.dismiss() }
                snackbar.show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                Logger.logError(e.message, this)
            }

        } else {
            Toast.makeText(this, "Failed to get files from the app package, please reinstall it.", Toast.LENGTH_LONG).show()
        }
    }

    private fun copyFile(inputStream: InputStream, outputStream: OutputStream) {
        inputStream.buffered().copyTo(outputStream)
    }
}
