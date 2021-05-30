package com.androidvip.hebf.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.androidvip.hebf.R

class NotificationViewModel : ViewModel() {
    private var _notificationCount: MutableLiveData<Int> = MutableLiveData()
    val notificationCount = _notificationCount

    init {
        _notificationCount.postValue(0)
    }

    fun setNotificationCount(count: Int) {
        _notificationCount.postValue(count)
    }

    fun increment(by: Int = 1) {
        var value = notificationCount.value ?: 0
        value += by
        _notificationCount.postValue(value)
    }

    fun decrement(by: Int = 1) {
        var value = notificationCount.value ?: 0
        value -= by
        _notificationCount.postValue(value)
    }
}