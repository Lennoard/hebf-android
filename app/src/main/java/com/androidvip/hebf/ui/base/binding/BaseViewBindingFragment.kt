package com.androidvip.hebf.ui.base.binding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.androidvip.hebf.ui.base.BaseFragment

typealias FragmentInflater<T> = (LayoutInflater, ViewGroup?, Boolean) -> T

abstract class BaseViewBindingFragment<T : ViewBinding>(
    private val inflate: FragmentInflater<T>,
) : BaseFragment() {

    private var _binding: T? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = inflate.invoke(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}