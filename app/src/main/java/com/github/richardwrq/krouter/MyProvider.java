package com.github.richardwrq.krouter;

import android.content.Context;
import android.util.Log;

import com.github.richardwrq.krouter.annotation.Provider;
import com.github.richardwrq.krouter.api.interfaces.IProvider;

import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * <p>
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/8 下午3:55
 */
@Provider("provider/myprovider")
public class MyProvider implements IProvider {
    @Override
    public void init(@NotNull Context context) {
        Log.i("MyProvider", "init!");
    }
}
