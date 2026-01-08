package com.lightningstudio.watchrss

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
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

class HeytapWidgetDemoActivity : ComponentActivity() {
    private var loadingDialog: HeyLoadingDialog? = null

    override fun attachBaseContext(newBase: Context) {
        val safeBase = if (newBase is SafeReceiverContextWrapper) {
            newBase
        } else {
            SafeReceiverContextWrapper(newBase)
        }
        super.attachBaseContext(safeBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heytap_widget_demo)

        val dialogButton = findViewById<HeyButton>(R.id.hey_button)
        dialogButton.setOnClickListener {
            HeyDialog.HeyBuilder(this)
                .setTitle("HeyDialog")
                .setMessage("This is a HeyDialog test.")
                .setPositiveButton("OK") { _ -> }
                .setNegativeButton("Cancel") { _ -> }
                .create()
                .show()
        }

        val loadingButton = findViewById<HeyButton>(R.id.hey_loading_button)
        loadingButton.setOnClickListener {
            if (loadingDialog == null) {
                loadingDialog = HeyLoadingDialog(this, "Loading...")
            }
            loadingDialog?.show()
            loadingButton.postDelayed({ loadingDialog?.dismiss() }, 1500)
        }

        val checkBox = findViewById<HeyCheckBox>(R.id.hey_checkbox)
        checkBox.setState(HeyCheckBox.SELECT_ALL)

        val toggle = findViewById<HeySwitch>(R.id.hey_switch)
        toggle.isChecked = true

        val progressButton = findViewById<HeyProgressButton>(R.id.hey_progress_button)
        progressButton.setProgressbtnText("Download")
        progressButton.setState(HeyProgressButton.STATE_DOWNLOADING)
        progressButton.progress = 45

        val horizontalProgress = findViewById<HeyHorizontalProgressBar>(R.id.hey_horizontal_progress)
        horizontalProgress.setMax(100)
        horizontalProgress.setProgress(60)

        val roundProgress = findViewById<HeyRoundProgress>(R.id.hey_round_progress)
        roundProgress.setMax(100)
        roundProgress.setProgress(70)

        val numberPicker = findViewById<HeyNumberPicker>(R.id.hey_number_picker)
        numberPicker.setValue(3)

        val timePicker = findViewById<HeyTimePicker>(R.id.hey_time_picker)
        timePicker.setInitValue(10, 30)

        val pageIndicator = findViewById<HeyPageIndicator>(R.id.hey_page_indicator)
        pageIndicator.setDotsCount(5, false)
        pageIndicator.setCurrentPosition(2)

        val spinnerIndicator = findViewById<HeyProgressSpinnerIndicator>(R.id.hey_spinner_indicator)
        spinnerIndicator.setText("Loading")
        spinnerIndicator.setIndeterminateMode(true)

        val textView = findViewById<HeyTextView>(R.id.hey_text_view)
        textView.text = "HeyTextView"
    }
}
