package com.github.richardwrq.krouter.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.github.richardwrq.krouter.R;
import com.github.richardwrq.common.RouterTable;
import com.github.richardwrq.krouter.annotation.Inject;
import com.github.richardwrq.krouter.annotation.Route;
import com.github.richardwrq.krouter.api.core.KRouter;
import com.github.richardwrq.krouter.bean.Person;
import com.github.richardwrq.krouter.provider.MyProvider;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;


@Route(path = RouterTable.MAIN_ATY_PATH)
public class MainActivity extends AppCompatActivity {
    @Inject
    String test;

    MyProvider mProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = new Bundle();
//        bundle.putString("test", "inject main1");
        getIntent().putExtras(bundle);
        KRouter.INSTANCE.inject(this);
        setContentView(R.layout.activity_main);
        Toast.makeText(this, test, Toast.LENGTH_SHORT).show();
    }

    public void openMain2(View view) {
        KRouter.INSTANCE.create(RouterTable.MAIN2_ATY_PATH)
                .withObject("person", new Person())
                .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .request();
    }

    public void openMain3(View view) {
        KRouter.INSTANCE.create(RouterTable.MAIN3_ATY_PATH)
                .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .request();
    }

    public void openMyService(View view) {
        KRouter.INSTANCE
                .create(RouterTable.MY_SERVICE_PATH + "?test=this is MyService")
                .subscribeNotFound(new Function2<KRouter.Navigator, String, Unit>() {
                    @Override
                    public Unit invoke(KRouter.Navigator navigator, String s) {
                        Toast.makeText(MainActivity.this, "未找到该路由: " + s, Toast.LENGTH_SHORT).show();
                        return null;
                    }
                })
                .subscribeArrived(new Function2<KRouter.Navigator, String, Unit>() {
                    @Override
                    public Unit invoke(KRouter.Navigator navigator, String s) {
                        Toast.makeText(MainActivity.this, "subscribeArrived : " + s, Toast.LENGTH_SHORT).show();
                        return null;
                    }
                })
                .subscribeBefore(new Function2<KRouter.Navigator, String, Unit>() {
                    @Override
                    public Unit invoke(KRouter.Navigator navigator, String s) {
                        Toast.makeText(MainActivity.this, "subscribeBefore : " + s, Toast.LENGTH_SHORT).show();
                        return null;
                    }
                })
                .subscribeRouteIntercept(new Function2<KRouter.Navigator, String, Unit>() {
                    @Override
                    public Unit invoke(KRouter.Navigator navigator, String s) {
                        Toast.makeText(MainActivity.this, "subscribeRouteIntercept : " + s, Toast.LENGTH_SHORT).show();
                        return null;
                    }
                })
                .request();
    }
}
