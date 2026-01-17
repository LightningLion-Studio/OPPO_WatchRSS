package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.widget.HeyButton
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.ui.viewmodel.AddRssViewModel
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import kotlinx.coroutines.launch

class AddRssActivity : BaseHeytapActivity() {
    private val viewModel: AddRssViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_add_rss)

        val input = findViewById<EditText>(R.id.input_url)
        val errorText = findViewById<HeyTextView>(R.id.text_error)
        val submitButton = findViewById<HeyButton>(R.id.button_submit)

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.updateUrl(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        submitButton.setOnClickListener {
            viewModel.submit()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    submitButton.isEnabled = !state.isSubmitting
                    submitButton.text = if (state.isSubmitting) "添加中" else "添加"

                    if (state.errorMessage.isNullOrBlank()) {
                        errorText.visibility = View.GONE
                    } else {
                        errorText.visibility = View.VISIBLE
                        errorText.text = state.errorMessage
                    }

                    val createdChannelId = state.createdChannelId
                    if (createdChannelId != null) {
                        val intent = Intent(this@AddRssActivity, FeedActivity::class.java)
                        intent.putExtra(FeedActivity.EXTRA_CHANNEL_ID, createdChannelId)
                        startActivity(intent)
                        viewModel.consumeCreatedChannel()
                        finish()
                    }
                }
            }
        }
    }
}
