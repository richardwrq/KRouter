package com.github.richardwrq.krouter;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.github.richardwrq.krouter.annotation.Interceptor;
import com.github.richardwrq.krouter.api.interfaces.IRouteInterceptor;

import org.jetbrains.annotations.NotNull;

/**
 * @author WuRuiQiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/2/27 下午7:11
 */
@Interceptor(priority = 2, name = "Interceptor2")
public class Interceptor2 implements IRouteInterceptor {
    @Override
    public boolean intercept(@NotNull Context context, @NotNull String path, @NotNull Bundle extras) {
        Log.i("Interceptor2", "Interceptor2 invoke, path: " + path);
        return false;
    }
}
