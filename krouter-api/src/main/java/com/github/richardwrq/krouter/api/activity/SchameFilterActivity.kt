package com.github.richardwrq.krouter.api.activity

import android.app.Activity
import android.os.Bundle
import com.github.richardwrq.krouter.api.core.KRouter

/**
 *
 *
 * @author WuRuiQiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/3/20 下午7:27
 */
class SchemeFilterActivity: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        KRouter.create(uri).request()
        finish()
    }
}