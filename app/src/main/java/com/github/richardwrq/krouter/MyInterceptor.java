package com.github.richardwrq.krouter;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.github.richardwrq.krouter.annotation.Interceptor;
import com.github.richardwrq.krouter.api.interfaces.IRouteInterceptor;

import org.jetbrains.annotations.NotNull;

/**
 * @author Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/1/26 上午11:12
 */
@Interceptor(priority = 1, name = "MyInterceptor")
public class MyInterceptor implements IRouteInterceptor {
    @Override
    public boolean intercept(@NotNull Context context, @NotNull String path, @NotNull Bundle extras) {
        Toast.makeText(context, "拦截器执行，当前路径为：" + path, Toast.LENGTH_SHORT).show();
        return "/RemoteControl/Main2Activity".equals(path);
    }
}
