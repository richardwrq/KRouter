package fixture.consumer;

import com.github.richardwrq.krouter.annotation.RouteType;
import com.github.richardwrq.krouter.annotation.model.RouteMetadata;

public final class ConsumerApiProbe {
    private ConsumerApiProbe() {
    }

    public static Class<RouteMetadata> routeMetadataType() {
        return RouteMetadata.class;
    }

    public static RouteType routeType() {
        return RouteType.ACTIVITY;
    }
}
