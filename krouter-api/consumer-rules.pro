# KRouter discovers generated loader implementations by their exact names.
-keep,allowoptimization class com.github.richardwrq.krouter.KRouter_RouteLoader_* implements com.github.richardwrq.krouter.api.interfaces.IRouteLoader { *; }
-keep,allowoptimization class com.github.richardwrq.krouter.KRouter_InterceptorLoader_* implements com.github.richardwrq.krouter.api.interfaces.IInterceptorLoader { *; }
-keep,allowoptimization class com.github.richardwrq.krouter.KRouter_ProviderLoader_* implements com.github.richardwrq.krouter.api.interfaces.IProviderLoader { *; }

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
