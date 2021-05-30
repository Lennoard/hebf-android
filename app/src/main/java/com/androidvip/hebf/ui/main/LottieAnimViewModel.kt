package com.androidvip.hebf.ui.main

import androidx.annotation.RawRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.androidvip.hebf.R

class LottieAnimViewModel : ViewModel() {
    private var _animRes: MutableLiveData<Int> = MutableLiveData()
    val animRes = _animRes

    init {
        _animRes.postValue(R.raw.dashboard_anim)
    }

    fun setAnimRes(@RawRes rawRes: Int) {
        _animRes.postValue(rawRes)
    }
}