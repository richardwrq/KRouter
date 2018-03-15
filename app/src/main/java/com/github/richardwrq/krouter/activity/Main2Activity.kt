package com.github.richardwrq.krouter.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.github.richardwrq.common.RouterTable
import com.github.richardwrq.krouter.R
import com.github.richardwrq.krouter.annotation.Inject
import com.github.richardwrq.krouter.annotation.Route
import com.github.richardwrq.krouter.api.core.KRouter
import com.github.richardwrq.krouter.bean.Person
import com.github.richardwrq.krouter.provider.MyProvider
import com.github.richardwrq.krouter.provider.NoImplProvider

@Route(path = RouterTable.MAIN2_ATY_PATH)
class Main2Activity : AppCompatActivity() {

    @Inject("person")
    lateinit var person: Person

    //NoImplProvider服务
    @Inject("NoImplProvider")
    lateinit var provider: NoImplProvider

    //MyProvider服务
    @Inject("provider/myprovider")
    lateinit var myProvider: MyProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        KRouter.inject(this)//调用该方法会从bundle中取出请求参数为上述字段赋值，如果被注解的字段是一个服务，则会自动实例化一个服务进行赋值
        myProvider.helloWorld()
        findViewById<TextView>(R.id.tv).text = person.toString()
    }
}
