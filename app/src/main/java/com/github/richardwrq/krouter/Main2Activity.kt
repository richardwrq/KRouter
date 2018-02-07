package com.github.richardwrq.krouter

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.github.richardwrq.krouter.annotation.Route

@Route(path = "/RemoteControl/Main2Activity")
class Main2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
    }
}
