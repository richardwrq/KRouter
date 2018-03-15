package com.github.richardwrq.krouter.provider;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.github.richardwrq.krouter.annotation.Provider;
import com.github.richardwrq.krouter.api.interfaces.IProvider;

import org.jetbrains.annotations.NotNull;

/**
 * 被注册的服务可以是任意类型，但是当被注册的服务实现了IProvider接口时，init(ApplicationContext)函数会被调用
 * 被注册的类必须存在无参构造函数
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/8 下午3:55
 */
@Provider("provider/myprovider")
public class MyProvider implements IProvider {
    @Override
    public void init(@NotNull Context context) {
        Toast.makeText(context, "MyProvider init", Toast.LENGTH_SHORT).show();
        Log.i("MyProvider", "init!");
    }

    public void helloWorld() {
        Log.i("MyProvider", "hello world!!!");
    }
}
