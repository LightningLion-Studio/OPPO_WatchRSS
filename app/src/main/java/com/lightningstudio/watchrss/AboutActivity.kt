package com.lightningstudio.watchrss

import android.os.Bundle
import com.heytap.wearable.support.widget.HeyMultipleDefaultItem
import com.heytap.wearable.support.widget.HeyToast

class AboutActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_about)

        val intro = findViewById<HeyMultipleDefaultItem>(R.id.item_intro)
        intro.setTitle("项目自介")
        intro.setSummary("腕上 RSS 聚合阅读")
        intro.setOnClickListener {
            HeyToast.showToast(this, "项目介绍准备中", android.widget.Toast.LENGTH_SHORT)
        }

        val privacy = findViewById<HeyMultipleDefaultItem>(R.id.item_privacy)
        privacy.setTitle("隐私协议")
        privacy.setSummary("即将补充")
        privacy.setOnClickListener {
            HeyToast.showToast(this, "隐私协议准备中", android.widget.Toast.LENGTH_SHORT)
        }

        val terms = findViewById<HeyMultipleDefaultItem>(R.id.item_terms)
        terms.setTitle("用户协议")
        terms.setSummary("即将补充")
        terms.setOnClickListener {
            HeyToast.showToast(this, "用户协议准备中", android.widget.Toast.LENGTH_SHORT)
        }

        val licenses = findViewById<HeyMultipleDefaultItem>(R.id.item_licenses)
        licenses.setTitle("开源许可与清单")
        licenses.setSummary("即将补充")
        licenses.setOnClickListener {
            HeyToast.showToast(this, "开源清单准备中", android.widget.Toast.LENGTH_SHORT)
        }
    }
}
