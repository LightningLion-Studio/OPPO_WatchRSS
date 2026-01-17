package com.lightningstudio.watchrss

import android.os.Bundle
import com.heytap.wearable.support.widget.HeyTextView

class InfoActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_info)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""

        findViewById<HeyTextView>(R.id.text_info_title).text = title
        findViewById<HeyTextView>(R.id.text_info_content).text = content
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_CONTENT = "content"
    }
}
