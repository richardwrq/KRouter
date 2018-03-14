package com.github.richardwrq.krouter.activity

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.github.richardwrq.krouter.R
import com.github.richardwrq.common.RouterTable
import com.github.richardwrq.common.RouterTable.MAIN3_ATY_PATH
import com.github.richardwrq.krouter.annotation.Route
import com.github.richardwrq.krouter.api.core.KRouter
import com.github.richardwrq.krouter.fragment.Fragment1
import com.github.richardwrq.krouter.fragment.Fragment2

@Route(MAIN3_ATY_PATH)
class Main3Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("param1", "1")
        intent.putExtra("param2", "2")
        startActivity(intent)
    }

    fun openFragment1(view: View) {
        val fragment = KRouter.create(RouterTable.FGM1_PATH).withString("test", "fragment1").request() as Fragment1
        fragmentManager.beginTransaction().replace(R.id.fl, fragment).commit()
    }

    fun openFragment2(view: View) {
        val fragment2 =  KRouter.create(RouterTable.FGM2_PATH).withString("test", "fragment2").request() as Fragment2
        fragmentManager.beginTransaction().replace(R.id.fl, fragment2).commit()
    }
}
