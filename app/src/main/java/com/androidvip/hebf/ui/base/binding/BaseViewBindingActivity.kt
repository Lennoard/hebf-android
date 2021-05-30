package com.androidvip.hebf.ui.base.binding

import android.os.Bundle
import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding
import com.androidvip.hebf.ui.base.BaseActivity

typealias ActivityInflater<T> = (LayoutInflater) -> T

abstract class BaseViewBindingActivity<T : ViewBinding>(
    private val inflate: ActivityInflater<T>
) : BaseActivity() {
    private var _binding: T? = null
    val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

}