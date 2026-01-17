package com.lightningstudio.watchrss

import android.os.Bundle
import com.heytap.wearable.support.widget.HeyMultipleDefaultItem
import android.content.Intent

class AboutActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_about)

        val intro = findViewById<HeyMultipleDefaultItem>(R.id.item_intro)
        intro.setTitle("项目自介")
        intro.setSummary("腕上 RSS 聚合阅读")
        intro.setOnClickListener {
            openInfo("项目自介", getString(R.string.about_intro_content))
        }

        val privacy = findViewById<HeyMultipleDefaultItem>(R.id.item_privacy)
        privacy.setTitle("隐私协议")
        privacy.setSummary("数据与网络说明")
        privacy.setOnClickListener {
            openInfo("隐私协议", getString(R.string.about_privacy_content))
        }

        val terms = findViewById<HeyMultipleDefaultItem>(R.id.item_terms)
        terms.setTitle("用户协议")
        terms.setSummary("使用条款")
        terms.setOnClickListener {
            openInfo("用户协议", getString(R.string.about_terms_content))
        }

        val licenses = findViewById<HeyMultipleDefaultItem>(R.id.item_licenses)
        licenses.setTitle("开源许可与清单")
        licenses.setSummary("第三方库信息")
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
