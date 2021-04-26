package com.androidvip.hebf.appintro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import com.androidvip.hebf.R
import com.androidvip.hebf.ui.base.BaseFragment

class Intro5Fragment : BaseFragment() {
    private lateinit var grantPermission: Button

    companion object {
        private const val REQUEST_CODE_WRITE_STORAGE = 871
        internal var isPermissionGranted: Boolean = false
            private set
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_intro5, container, false)
        grantPermission = view.findViewById(R.id.intro_button_grant_permission)
        grantPermission.setOnClickListener { askForPermissions() }

        return view
    }

    override fun onResume() {
        super.onResume()
        askForPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        val writePerm = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (requestCode == REQUEST_CODE_WRITE_STORAGE) {
            if (permissions.isNotEmpty() && grantResults.isNotEmpty()) {
                if (permissions.first() == writePerm && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                    grantPermission.visibility = View.GONE
                    isPermissionGranted = true
                } else {
                    grantPermission.visibility = View.VISIBLE
                    isPermissionGranted = false
                }
            } else {
                grantPermission.visibility = View.VISIBLE
                isPermissionGranted = false
            }
        }
    }

    private fun askForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (hasStoragePermissions()) {
                isPermissionGranted = false
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_WRITE_STORAGE)
            } else
                isPermissionGranted = true
        } else {
            isPermissionGranted = true
        }
    }

    private fun hasStoragePermissions(): Boolean {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(findContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
    }
}