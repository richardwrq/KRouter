package com.github.richardwrq.krouter;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.github.richardwrq.krouter.annotation.Interceptor;
import com.github.richardwrq.krouter.api.interfaces.IRouteInterceptor;

import org.jetbrains.annotations.NotNull;

/**
 * @author Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/1/26 上午11:12
 */
@Interceptor(priority = 1, name = "Interceptor1")
public class Interceptor1 implements IRouteInterceptor {
    @Override
    public boolean intercept(@NotNull Context context, @NotNull String path, @NotNull Bundle extras) {
        Log.i("Interceptor1", "Interceptor1 invoke, path: " + path);
        return false;
    }
}
