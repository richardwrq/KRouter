package com.github.richardwrq.krouter.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.github.richardwrq.krouter.R
import com.github.richardwrq.common.RouterTable
import com.github.richardwrq.krouter.annotation.Inject
import com.github.richardwrq.krouter.annotation.Route
import com.github.richardwrq.krouter.api.core.KRouter
import com.github.richardwrq.krouter.api.interfaces.IProvider
import com.github.richardwrq.krouter.bean.Person

@Route(RouterTable.MAIN2_ATY_PATH)
class Main2Activity : AppCompatActivity() {

    @Inject("person")
    lateinit var person: Person

    @Inject("provider/myprovider")
    lateinit var provider: IProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        KRouter.inject(this)
        findViewById<TextView>(R.id.tv).text = person.toString()
    }
}
