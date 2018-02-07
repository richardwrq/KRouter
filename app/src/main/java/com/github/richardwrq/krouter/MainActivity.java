package com.github.richardwrq.krouter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.github.richardwrq.krouter.annotation.Route;
import com.github.richardwrq.krouter.api.core.KRouter;



@Route(path = "/RemoteControl/MainActivity")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openMain2(View view) {
        Log.i("MainActivity", "openMain2");
        KRouter.INSTANCE.create("/RemoteControl/Main2Activity").withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).request();
    }
}
