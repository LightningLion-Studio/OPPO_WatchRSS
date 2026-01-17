package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

class SettingsActivity : BaseHeytapActivity() {
    private val viewModel: SettingsViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    private val cacheOptions = listOf(
        10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L, 200L, 300L
    )
    private var currentLimitMb: Long = cacheOptions.first()

    private lateinit var usageView: HeyTextView
    private lateinit var valueView: HeyTextView
    private lateinit var minusButton: HeyTextView
    private lateinit var plusButton: HeyTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_settings)

        usageView = findViewById(R.id.text_settings_usage)
        valueView = findViewById(R.id.text_cache_value)
        minusButton = findViewById(R.id.button_cache_minus)
        plusButton = findViewById(R.id.button_cache_plus)

        minusButton.setOnClickListener {
            val next = cacheOptions.lastOrNull { it < currentLimitMb } ?: return@setOnClickListener
            applySelection(next)
            viewModel.updateCacheLimitMb(next)
        }
        plusButton.setOnClickListener {
            val next = cacheOptions.firstOrNull { it > currentLimitMb } ?: return@setOnClickListener
            applySelection(next)
            viewModel.updateCacheLimitMb(next)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.cacheUsageMb.collect { usage ->
                        usageView.text = "当前已用 ${usage}MB"
                    }
                }
                launch {
                    viewModel.cacheLimitMb.collect { limit ->
                        applySelection(limit)
                    }
                }
            }
        }
    }

    private fun applySelection(limitMb: Long) {
        currentLimitMb = limitMb
        valueView.text = "${limitMb}MB"
        val canDecrease = cacheOptions.any { it < limitMb }
        val canIncrease = cacheOptions.any { it > limitMb }
        minusButton.isEnabled = canDecrease
        plusButton.isEnabled = canIncrease
        minusButton.alpha = if (canDecrease) 1f else 0.4f
        plusButton.alpha = if (canIncrease) 1f else 0.4f
    }
}
