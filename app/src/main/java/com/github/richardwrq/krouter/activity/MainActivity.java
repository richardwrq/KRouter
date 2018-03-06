package com.github.richardwrq.krouter.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        setContentView(R.layout.activity_main);
        Bundle bundle = new Bundle();
        //        bundle.putString("test", "inject main1");
        getIntent().putExtras(bundle);
        KRouter.INSTANCE.inject(this);
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

    public void bindService(View view) {
        KRouter.INSTANCE
                .create(RouterTable.BIND_SERVICE_PATH)
                .withServiceFlags(BIND_AUTO_CREATE)
                .withServiceConn(new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        Toast.makeText(MainActivity.this, name.getClassName() + " bind 成功", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        Toast.makeText(MainActivity.this, name.getClassName() + " disconnected", Toast.LENGTH_SHORT).show();
                    }
                })
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

    public void startActivityForResult(View view) {
        Fragment fragment = (Fragment) KRouter.INSTANCE.create("myfragment").request();
        Toast.makeText(this, "找到Fragment: " + fragment.getClass().getSimpleName() + ", hashcode: " + fragment, Toast.LENGTH_SHORT).show();
//        KRouter.INSTANCE.create(RouterTable.RESULT_ATY_PATH)
//                .withRequestCode(this, 2)
//                .request();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(this, "result : " + data.getStringExtra("result"), Toast.LENGTH_SHORT).show();
    }

    @Route(path = "myfragment")
    public static class MyFragment extends Fragment {
        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            Log.d("MyFragment", "onAttach: ");
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d("MyFragment", "onCreate: ");
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.activity_main, container, false);
            Log.d("MyFragment", "onCreateView: ");
            return view;
        }
    }
}
