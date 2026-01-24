package com.lightningstudio.watchrss

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.heytap.wearable.support.widget.HeyButton
import com.heytap.wearable.support.widget.HeyCheckBox
import com.heytap.wearable.support.widget.HeyDialog
import com.heytap.wearable.support.widget.HeyHorizontalProgressBar
import com.heytap.wearable.support.widget.HeyLoadingDialog
import com.heytap.wearable.support.widget.HeyNumberPicker
import com.heytap.wearable.support.widget.HeyProgressButton
import com.heytap.wearable.support.widget.HeyRoundProgress
import com.heytap.wearable.support.widget.HeySwitch
import com.heytap.wearable.support.widget.HeyTextView
import com.heytap.wearable.support.widget.HeyTimePicker
import com.heytap.wearable.support.widget.pageindicator.HeyPageIndicator
import com.heytap.wearable.support.widget.progressindicator.HeyProgressSpinnerIndicator
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme

class HeytapWidgetDemoActivity : BaseHeytapActivity() {
    private var loadingDialog: HeyLoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContent {
            WatchRSSTheme {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { _ ->
                        val root = layoutInflater.inflate(
                            R.layout.activity_heytap_widget_demo,
                            null,
                            false
                        )
                        bindViews(root)
                        root
                    }
                )
            }
        }
    }

    private fun bindViews(root: View) {
        val dialogButton = root.findViewById<HeyButton>(R.id.hey_button)
        dialogButton.setOnClickListener {
            HeyDialog.HeyBuilder(this)
                .setTitle("HeyDialog")
                .setMessage("This is a HeyDialog test.")
                .setPositiveButton("OK") { _ -> }
                .setNegativeButton("Cancel") { _ -> }
                .create()
                .show()
        }

        val loadingButton = root.findViewById<HeyButton>(R.id.hey_loading_button)
        loadingButton.setOnClickListener {
            if (loadingDialog == null) {
                loadingDialog = HeyLoadingDialog(this, "Loading...")
            }
            loadingDialog?.show()
            loadingButton.postDelayed({ loadingDialog?.dismiss() }, 1500)
        }

        val checkBox = root.findViewById<HeyCheckBox>(R.id.hey_checkbox)
        checkBox.setState(HeyCheckBox.SELECT_ALL)

        val toggle = root.findViewById<HeySwitch>(R.id.hey_switch)
        toggle.isChecked = true

        val progressButton = root.findViewById<HeyProgressButton>(R.id.hey_progress_button)
        progressButton.setProgressbtnText("Download")
        progressButton.setState(HeyProgressButton.STATE_DOWNLOADING)
        progressButton.progress = 45

        val horizontalProgress = root.findViewById<HeyHorizontalProgressBar>(R.id.hey_horizontal_progress)
        horizontalProgress.setMax(100)
        horizontalProgress.setProgress(60)

        val roundProgress = root.findViewById<HeyRoundProgress>(R.id.hey_round_progress)
        roundProgress.setMax(100)
        roundProgress.setProgress(70)

        val numberPicker = root.findViewById<HeyNumberPicker>(R.id.hey_number_picker)
        numberPicker.setValue(3)

        val timePicker = root.findViewById<HeyTimePicker>(R.id.hey_time_picker)
        timePicker.setInitValue(10, 30)

        val pageIndicator = root.findViewById<HeyPageIndicator>(R.id.hey_page_indicator)
        pageIndicator.setDotsCount(5, false)
        pageIndicator.setCurrentPosition(2)

        val spinnerIndicator = root.findViewById<HeyProgressSpinnerIndicator>(R.id.hey_spinner_indicator)
        spinnerIndicator.setText("Loading")
        spinnerIndicator.setIndeterminateMode(true)

        val textView = root.findViewById<HeyTextView>(R.id.hey_text_view)
        textView.text = "HeyTextView"
    }
}
