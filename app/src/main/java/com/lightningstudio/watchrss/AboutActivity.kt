package com.lightningstudio.watchrss

import android.os.Bundle
import android.content.Intent

class AboutActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_about)

        val intro = findViewById<android.view.View>(R.id.item_intro)
        intro.setOnClickListener {
            openInfo("项目自介", getString(R.string.about_intro_content))
        }

        val privacy = findViewById<android.view.View>(R.id.item_privacy)
        privacy.setOnClickListener {
            openInfo("隐私协议", getString(R.string.about_privacy_content))
        }

        val terms = findViewById<android.view.View>(R.id.item_terms)
        terms.setOnClickListener {
            openInfo("用户协议", getString(R.string.about_terms_content))
        }

        val licenses = findViewById<android.view.View>(R.id.item_licenses)
        licenses.setOnClickListener {
            openInfo("开源许可与清单", getString(R.string.about_licenses_content))
        }
    }

    private fun openInfo(title: String, content: String) {
        val intent = Intent(this, InfoActivity::class.java)
        intent.putExtra(InfoActivity.EXTRA_TITLE, title)
        intent.putExtra(InfoActivity.EXTRA_CONTENT, content)
        startActivity(intent)
    }
}
