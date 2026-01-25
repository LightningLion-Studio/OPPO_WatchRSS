package com.lightningstudio.watchrss.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.lightningstudio.watchrss.data.bili.BiliRepository

class BiliViewModelFactory(
    private val repository: BiliRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()
        return when {
            modelClass.isAssignableFrom(BiliFeedViewModel::class.java) -> {
                BiliFeedViewModel(repository)
            }
            modelClass.isAssignableFrom(BiliLoginViewModel::class.java) -> {
                BiliLoginViewModel(repository)
            }
            modelClass.isAssignableFrom(BiliDetailViewModel::class.java) -> {
                BiliDetailViewModel(savedStateHandle, repository)
            }
            modelClass.isAssignableFrom(BiliPlayerViewModel::class.java) -> {
                BiliPlayerViewModel(savedStateHandle, repository)
            }
            modelClass.isAssignableFrom(BiliListViewModel::class.java) -> {
                BiliListViewModel(savedStateHandle, repository)
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return create(modelClass, CreationExtras.Empty)
    }
}
