# KRouter discovers generated loader implementations by their exact names.
-keep,allowoptimization class com.github.richardwrq.krouter.KRouter_RouteLoader_* implements com.github.richardwrq.krouter.api.interfaces.IRouteLoader { *; }
-keep,allowoptimization class com.github.richardwrq.krouter.KRouter_InterceptorLoader_* implements com.github.richardwrq.krouter.api.interfaces.IInterceptorLoader { *; }
-keep,allowoptimization class com.github.richardwrq.krouter.KRouter_ProviderLoader_* implements com.github.richardwrq.krouter.api.interfaces.IProviderLoader { *; }

# Generated loaders put Provider and interceptor Class values into KRouter's registries.
# KRouter later instantiates those classes through Class.newInstance(), so R8 must not
# merge a concrete implementation into an abstract class or remove its public no-arg
# constructor. Names may still be obfuscated because KRouter does not look them up by name.
-keep,allowobfuscation interface com.github.richardwrq.krouter.api.interfaces.IProvider
-keep,allowobfuscation interface com.github.richardwrq.krouter.api.interfaces.SerializationProvider
-keep,allowobfuscation interface com.github.richardwrq.krouter.api.interfaces.IRouteInterceptor
-keepclasseswithmembers,allowobfuscation class * implements com.github.richardwrq.krouter.api.interfaces.IProvider {
    public <init>();
}
-keepclasseswithmembers,allowobfuscation class * implements com.github.richardwrq.krouter.api.interfaces.IRouteInterceptor {
    public <init>();
}

# Route loaders also retain Fragment Class values which KRouter constructs reflectively.
# AndroidX publishes the equivalent FragmentFactory rule; keep both supported KRouter
# Fragment families explicit so the platform Fragment path has the same guarantee.
-if public class ** extends android.app.Fragment
-keepclasseswithmembers,allowobfuscation public class <1> {
    public <init>();
}
-if public class ** extends androidx.fragment.app.Fragment
-keepclasseswithmembers,allowobfuscation public class <1> {
    public <init>();
}

# Each injectable target has a generated implementation loaded through Class.forName.
-keep,includedescriptorclasses,allowoptimization class **_KRouter_Injector implements com.github.richardwrq.krouter.api.interfaces.IInjector { *; }

# Generated injectors use anonymous TypeToken<T> subclasses to recover custom-object types.
# R8 full mode only retains their generic superclass metadata when both the subclass and
# Signature attribute are explicitly kept. The conditional rule targets those subclasses
# without pinning unrelated application classes or their names.
-keepattributes Signature
-keep,allowobfuscation,allowoptimization class com.github.richardwrq.krouter.api.utils.TypeToken
-if class ** extends com.github.richardwrq.krouter.api.utils.TypeToken
-keep,allowobfuscation,allowoptimization class <1>
