package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.widget.HeySwitch
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
    private val fontOptions = (12..32 step 2).toList()
    private var currentFontSp: Int = fontOptions[1]

    private lateinit var usageView: HeyTextView
    private lateinit var valueView: HeyTextView
    private lateinit var minusButton: HeyTextView
    private lateinit var plusButton: HeyTextView
    private lateinit var themeValue: HeyTextView
    private lateinit var themeToggle: HeySwitch
    private lateinit var progressIndicatorValue: HeyTextView
    private lateinit var progressIndicatorToggle: HeySwitch
    private lateinit var shareModeValue: HeyTextView
    private lateinit var shareModeToggle: HeySwitch
    private lateinit var fontValue: HeyTextView
    private lateinit var fontMinus: HeyTextView
    private lateinit var fontPlus: HeyTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_settings)

        usageView = findViewById(R.id.text_settings_usage)
        valueView = findViewById(R.id.text_cache_value)
        minusButton = findViewById(R.id.button_cache_minus)
        plusButton = findViewById(R.id.button_cache_plus)
        themeValue = findViewById(R.id.text_reading_theme_value)
        themeToggle = findViewById(R.id.button_reading_theme_toggle)
        progressIndicatorValue = findViewById(R.id.text_progress_indicator_value)
        progressIndicatorToggle = findViewById(R.id.button_progress_indicator_toggle)
        shareModeValue = findViewById(R.id.text_share_mode_value)
        shareModeToggle = findViewById(R.id.button_share_mode_toggle)
        fontValue = findViewById(R.id.text_font_size_value)
        fontMinus = findViewById(R.id.button_font_minus)
        fontPlus = findViewById(R.id.button_font_plus)

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

        themeToggle.setOnClickListener { viewModel.toggleReadingTheme() }
        progressIndicatorToggle.setOnClickListener { viewModel.toggleDetailProgressIndicator() }
        shareModeToggle.setOnClickListener { viewModel.toggleShareUseSystem() }

        fontMinus.setOnClickListener {
            val next = fontOptions.lastOrNull { it < currentFontSp } ?: return@setOnClickListener
            applyFontSelection(next)
            viewModel.updateReadingFontSizeSp(next)
        }

        fontPlus.setOnClickListener {
            val next = fontOptions.firstOrNull { it > currentFontSp } ?: return@setOnClickListener
            applyFontSelection(next)
            viewModel.updateReadingFontSizeSp(next)
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
                launch {
                    viewModel.readingThemeDark.collect { isDark ->
                        themeValue.text = if (isDark) "深色" else "浅色"
                        themeToggle.isChecked = isDark
                    }
                }
                launch {
                    viewModel.detailProgressIndicatorEnabled.collect { enabled ->
                        progressIndicatorValue.text = if (enabled) "开启" else "关闭"
                        progressIndicatorToggle.isChecked = enabled
                    }
                }
                launch {
                    viewModel.shareUseSystem.collect { useSystem ->
                        shareModeValue.text = if (useSystem) "系统分享" else "二维码"
                        shareModeToggle.isChecked = useSystem
                    }
                }
                launch {
                    viewModel.readingFontSizeSp.collect { value ->
                        applyFontSelection(value)
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

    private fun applyFontSelection(valueSp: Int) {
        currentFontSp = valueSp
        fontValue.text = "${valueSp}sp"
        val canDecrease = fontOptions.any { it < valueSp }
        val canIncrease = fontOptions.any { it > valueSp }
        fontMinus.isEnabled = canDecrease
        fontPlus.isEnabled = canIncrease
        fontMinus.alpha = if (canDecrease) 1f else 0.4f
        fontPlus.alpha = if (canIncrease) 1f else 0.4f
    }
}
