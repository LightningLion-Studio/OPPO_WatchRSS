package com.lightningstudio.watchrss

import android.os.Bundle
import com.heytap.wearable.support.widget.HeyTextView

class BiliEntryActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_platform_entry)

        val titleView = findViewById<HeyTextView>(R.id.text_title)
        titleView.text = "B站"

        val message = findViewById<HeyTextView>(R.id.text_platform_message)
        message.text = "B站内容接入准备中"
    }
}
