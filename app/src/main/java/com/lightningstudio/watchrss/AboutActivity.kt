package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.lightningstudio.watchrss.ui.screen.AboutScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme

class AboutActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        setContent {
            WatchRSSTheme {
                AboutScreen(
                    onIntroClick = {
                        openInfo("项目自介", getString(R.string.about_intro_content))
                    },
                    onPrivacyClick = {
                        openInfo("隐私协议", getString(R.string.about_privacy_content))
                    },
                    onTermsClick = {
                        openInfo("用户协议", getString(R.string.about_terms_content))
                    },
                    onLicensesClick = {
                        openInfo("开源许可与清单", getString(R.string.about_licenses_content))
                    },
                    onCollaboratorsClick = {
                        startActivity(Intent(this, CollaboratorsActivity::class.java))
                    }
                )
            }
        }
    }

    private fun openInfo(title: String, content: String) {
        val intent = Intent(this, InfoActivity::class.java)
        intent.putExtra(InfoActivity.EXTRA_TITLE, title)
        intent.putExtra(InfoActivity.EXTRA_CONTENT, content)
        startActivity(intent)
    }
}
