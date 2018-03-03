package com.github.richardwrq.module.java

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import com.github.richardwrq.common.RouterTable.RESULT_ATY_PATH
import com.github.richardwrq.krouter.annotation.Route
import org.rc.wrq.module.java.R

@Route(RESULT_ATY_PATH)
class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
    }

    fun returnResult(view: View) {
        val intent = Intent()
        val editText = findViewById<EditText>(R.id.et)
        intent.putExtra("result", editText.text.toString())
        setResult(1, intent)
        finish()
    }
}
