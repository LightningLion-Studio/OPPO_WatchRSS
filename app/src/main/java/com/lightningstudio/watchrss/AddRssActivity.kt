package com.lightningstudio.watchrss

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.widget.HeyButton
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.ui.viewmodel.AddRssViewModel
import com.lightningstudio.watchrss.ui.viewmodel.AddRssStep
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
        val inputContainer = findViewById<LinearLayout>(R.id.container_input)
        val previewContainer = findViewById<LinearLayout>(R.id.container_preview)
        val existingContainer = findViewById<LinearLayout>(R.id.container_existing)
        val errorActions = findViewById<LinearLayout>(R.id.container_error_actions)
        val retryButton = findViewById<HeyButton>(R.id.button_retry)
        val cancelButton = findViewById<HeyButton>(R.id.button_cancel)

        val previewTitle = findViewById<HeyTextView>(R.id.text_preview_title)
        val previewDesc = findViewById<HeyTextView>(R.id.text_preview_desc)
        val confirmButton = findViewById<HeyButton>(R.id.button_confirm)
        val editButton = findViewById<HeyButton>(R.id.button_edit)

        val existingHint = findViewById<HeyTextView>(R.id.text_existing_hint)
        val goChannelButton = findViewById<HeyButton>(R.id.button_go_channel)
        val existingBackButton = findViewById<HeyButton>(R.id.button_existing_back)

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.updateUrl(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val presetUrl = intent.getStringExtra(EXTRA_URL)?.trim().orEmpty()
        if (presetUrl.isNotEmpty()) {
            input.setText(presetUrl)
            input.setSelection(presetUrl.length)
            viewModel.updateUrl(presetUrl)
        }

        input.setOnEditorActionListener { _, actionId, _ ->
            val shouldSubmit = actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_SEND
            if (shouldSubmit) {
                viewModel.updateUrl(input.text?.toString().orEmpty())
                hideKeyboard(input)
                input.clearFocus()
                viewModel.submit()
            }
            shouldSubmit
        }

        input.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                viewModel.updateUrl(input.text?.toString().orEmpty())
                hideKeyboard(input)
                input.clearFocus()
                viewModel.submit()
                true
            } else {
                false
            }
        }

        submitButton.setOnClickListener {
            viewModel.updateUrl(input.text?.toString().orEmpty())
            hideKeyboard(input)
            input.clearFocus()
            viewModel.submit()
        }

        confirmButton.setOnClickListener {
            viewModel.confirmAdd()
        }

        editButton.setOnClickListener {
            viewModel.backToInput()
        }

        existingBackButton.setOnClickListener {
            viewModel.backToInput()
        }

        retryButton.setOnClickListener {
            val state = viewModel.uiState.value
            if (state.step == AddRssStep.PREVIEW && state.preview != null) {
                viewModel.confirmAdd()
            } else {
                viewModel.submit()
            }
        }

        cancelButton.setOnClickListener {
            viewModel.clearError()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    submitButton.isEnabled = !state.isLoadingPreview && !state.isSubmitting
                    submitButton.text = if (state.isLoadingPreview) "解析中" else "添加"
                    confirmButton.isEnabled = !state.isSubmitting
                    confirmButton.text = if (state.isSubmitting) "添加中" else "确认添加"

                    if (state.errorMessage.isNullOrBlank()) {
                        errorText.visibility = View.GONE
                        errorActions.visibility = View.GONE
                    } else {
                        errorText.visibility = View.VISIBLE
                        errorText.text = state.errorMessage
                        val showActions = state.errorMessage != "请输入 RSS 地址" && state.errorMessage != "URL 不合法"
                        errorActions.visibility = if (showActions) View.VISIBLE else View.GONE
                    }

                    inputContainer.visibility = if (state.step == AddRssStep.INPUT) View.VISIBLE else View.GONE
                    previewContainer.visibility = if (state.step == AddRssStep.PREVIEW) View.VISIBLE else View.GONE
                    existingContainer.visibility = if (state.step == AddRssStep.EXISTING) View.VISIBLE else View.GONE

                    val preview = state.preview
                    if (preview != null) {
                        previewTitle.text = preview.title
                        previewDesc.text = preview.description ?: "暂无简介"
                    }

                    val existing = state.existingChannel
                    if (existing != null) {
                        existingHint.text = "已存在：${existing.title}"
                        goChannelButton.setOnClickListener {
                            openChannel(existing.url, existing.id)
                        }
                    }

                    val createdChannelId = state.createdChannelId
                    if (createdChannelId != null) {
                        openChannel(state.url, createdChannelId)
                        viewModel.consumeCreatedChannel()
                        finish()
                    }
                }
            }
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun openChannel(url: String?, channelId: Long) {
        val builtin = com.lightningstudio.watchrss.data.rss.BuiltinChannelType.fromUrl(url)
            ?: builtinFromInputUrl(url)
        when (builtin) {
            com.lightningstudio.watchrss.data.rss.BuiltinChannelType.BILI -> {
                startActivity(Intent(this, BiliEntryActivity::class.java))
            }
            com.lightningstudio.watchrss.data.rss.BuiltinChannelType.DOUYIN -> {
                startActivity(Intent(this, DouyinEntryActivity::class.java))
            }
            null -> {
                val intent = Intent(this, FeedActivity::class.java)
                intent.putExtra(FeedActivity.EXTRA_CHANNEL_ID, channelId)
                startActivity(intent)
            }
        }
        finish()
    }

    private fun builtinFromInputUrl(url: String?): com.lightningstudio.watchrss.data.rss.BuiltinChannelType? {
        if (url.isNullOrBlank()) return null
        val host = runCatching { Uri.parse(url).host }.getOrNull()
        return com.lightningstudio.watchrss.data.rss.BuiltinChannelType.fromHost(host)
    }

    companion object {
        const val EXTRA_URL = "extra_url"
    }
}
