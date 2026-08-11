package com.github.richardwrq.krouter;

import android.app.Application;

import com.github.richardwrq.krouter.annotation.RouteType;
import com.github.richardwrq.krouter.annotation.model.RouteMetadata;
import com.github.richardwrq.krouter.api.core.ExternalDeepLinkPolicy;
import com.github.richardwrq.krouter.api.core.KRouter;
import com.github.richardwrq.krouter.activity.Main3Activity;
import com.github.richardwrq.module.java.BindService;

import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static com.github.richardwrq.common.RouterTable.BIND_SERVICE_PATH;
import static com.github.richardwrq.common.RouterTable.MAIN3_ATY_PATH;

/**
 * @author Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/1/26 下午7:43
 */
public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        KRouter.openDebug();
        ExternalDeepLinkPolicy externalDeepLinks = ExternalDeepLinkPolicy.builder()
                .allow("krouter", "wrq.richard.com", MAIN3_ATY_PATH, Main3Activity.class)
                .build();
        KRouter.init(this, externalDeepLinks);
        KRouter.INSTANCE.addRoutePath(new Function1<Map<String, RouteMetadata>, Unit>() {
            @Override
            public Unit invoke(Map<String, RouteMetadata> stringRouteMetadataMap) {
                stringRouteMetadataMap.put(BIND_SERVICE_PATH, new RouteMetadata(RouteType.SERVICE, -1, "", BIND_SERVICE_PATH, "", "", BindService.class));
                return null;
            }
        });
    }
}
