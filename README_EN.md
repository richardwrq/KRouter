# KRouter

[简体中文](README.md) | **English**

KRouter is an annotation-driven routing framework for Android. It supports Activities, Services, Fragments, interceptors, Providers, and field injection in mixed Java/Kotlin projects.

> **Revival beta:** The current maintenance line is `0.2.0-beta.1`. The source has been migrated to a modern Android and Gradle toolchain, but API and binary compatibility are not yet stable, and no production-ready Maven artifact is available. Any GitHub prerelease attachments are intended only for source evaluation and compatibility testing. The historical Bintray/JCenter installation path no longer works; do not use the old coordinates.

## Current capabilities

- Register Activity, Service, and Fragment routes with `@Route`.
- Build route requests with paths, parameters, Intent flags, and Service connection options.
- Run route interceptors by priority with `@Interceptor`.
- Register services with `@Provider` and inject parameters or services with `@Inject`.
- Extend custom object serialization through `SerializationProvider`.
- Generate route, Provider, interceptor, and injector code at compile time.
- Support mixed Java/Kotlin modules and MultiDex applications.

External intents and deep links are security-sensitive entry points. `KRouter.init(context)` denies every external deep link by default. KRouter enables an exact URI-to-Activity mapping only when the application supplies an explicit allowlist that permits it.

## Build from source

Requirements:

- JDK 17
- Android SDK Platform 35
- The Gradle Wrapper included in this repository; no separate Gradle installation is required

```bash
git clone https://github.com/richardwrq/KRouter.git
cd KRouter
./gradlew --no-daemon clean check assembleDebug lint
```

The sample APK is generated under `app/build/outputs/apk/debug/`. CI runs the same JDK 17 build, test, assembly, and lint checks.

## Source evaluation and integration status

The sample modules in this repository depend directly on the local source modules:

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'org.jetbrains.kotlin.kapt'
    id 'com.github.richardwrq.krouter'
}

dependencies {
    implementation project(':krouter-api')
    implementation project(':krouter-annotation')
    kapt project(':krouter-compiler')
}
```

This configuration applies only to builds inside this repository. It does not mean that maintained, externally resolvable plugin or library coordinates have been published to a supported public Maven repository. Until maintained `0.2.x` coordinates are available and compatibility guarantees are established, evaluate KRouter through the sample application or GitHub prerelease attachments instead of relying on the old `0.1.x` Bintray/JCenter artifacts.

To inspect the Maven metadata, POMs, source archives, and Gradle plugin marker, generate a test repository under the repository-local `build/repository/` directory:

```bash
./gradlew --no-daemon clean
./gradlew --no-daemon publishLibrariesToBuildRepository publishPluginToBuildRepository
```

Run `clean` as a separate Gradle invocation first to prevent it from competing with publication tasks in the included plugin build. These tasks require no credentials, do not write to Maven Local, and upload nothing. The `0.2.0-beta.1` files under `build/repository/` exist only for testing the current source artifacts; they are not a public Maven release.

The external-consumer test resolves the exact KRouter version only from this workflow-local repository. Those first-party artifacts are built from the same checkout and verified through API compilation, KAPT generation, R8 output inspection, and runtime entry-point preservation checks, so the test does not pin local artifact hashes that may vary across platforms. Every third-party dependency downloaded from Google Maven or Maven Central remains subject to strict SHA-256 verification.

## Basic usage

### Register a route

```kotlin
@Route(path = "/krouter/sample/Main2Activity")
class Main2Activity : AppCompatActivity()
```

### Initialize KRouter

Applications that do not accept external deep links should use the default initialization. This overload keeps every external URI denied:

```kotlin
class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KRouter.init(this)
    }
}
```

During debugging, call `KRouter.openDebug()` before initialization to enable logs. Production builds should control log output as appropriate for the application.

### Explicitly allow external deep links

If the application must accept external deep links, replace the default initialization above with a policy-based call. Declare the exact scheme, host, path, and allowed query keys for each external entry point, then freeze the policy during initialization:

```kotlin
val externalDeepLinks = ExternalDeepLinkPolicy.builder()
    .allow(
        "krouter",
        "wrq.richard.com",
        "/krouter/sample/Main3Activity",
        Main3Activity::class.java,
        "id"
    )
    .build()

KRouter.init(this, externalDeepLinks)
```

Make the manifest intent filter equally restrictive:

```xml
<activity
    android:name="com.github.richardwrq.krouter.api.activity.SchemeFilterActivity"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="krouter"
            android:host="wrq.richard.com"
            android:path="/krouter/sample/Main3Activity" />
    </intent-filter>
</activity>
```

The target Activity must also be explicitly enabled in the merged Manifest and remain `android:exported="false"`. KRouter rejects query keys that are not allowlisted, duplicate keys, ports, userinfo, fragments, non-canonical paths, and Intents whose action is not `ACTION_VIEW`. It also rejects ClipData and selectors. Incoming Intent extras and flags do not cross this boundary, and the explicit component must be unset (`null`) or identify the current application's `SchemeFilterActivity`. Each allowlisted path must exactly match its declared Activity during initialization; a custom fuzzy matcher cannot widen an external entry point.

### Make a request

```kotlin
KRouter.create("/krouter/sample/Main2Activity")
    .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    .withString("param", "test")
    .request()
```

Retrieve a Fragment:

```kotlin
val fragment = KRouter.create("/krouter/sample/fragment1").request() as Fragment1
```

Start a Service:

```kotlin
KRouter.create("/krouter/sample/MyService").request()
```

### Register and retrieve a Provider

A Provider must have a no-argument constructor. If it implements `IProvider`, KRouter calls `init(context)` after creating the instance.

```kotlin
@Provider("provider/my-provider")
class MyProvider : IProvider {
    override fun init(context: Context) {
        // Initialize without retaining an Activity context.
    }
}
```

```kotlin
val provider: MyProvider? = KRouter.getProvider("provider/my-provider")
```

Provider keys cannot be blank or duplicated. Keys are included in compile-time generated code, so run the full `check` task after changing one.

### Inject parameters or Providers

```kotlin
class Main2Activity : AppCompatActivity() {
    @Inject(name = "person", isRequired = true)
    lateinit var person: Person

    @Inject(name = "provider/my-provider", isRequired = true)
    lateinit var provider: MyProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KRouter.inject(this)
    }
}
```

`isRequired = true` throws immediately when a value or Provider is missing. Optional injection preserves the initial value only when the key is genuinely absent and no matching Provider exists, so optional fields should define an initial value. Do not treat an uninitialized `lateinit` field as optional. If a Bundle contains the key, a null value, type mismatch, missing `SerializationProvider`, or deserialization failure throws an exception containing the field and key context instead of being treated as "not provided."

At runtime, only assets whose names begin with `KRouter_` are treated as module markers. `transferModuleName` throws `IllegalArgumentException` for any other input, preventing arbitrary filenames from being interpreted as generated tables.

### Declare an interceptor

Lower numbers have higher priority. Returning `true` stops the current route request.

```kotlin
@Interceptor(priority = 1, name = "Authentication")
class AuthenticationInterceptor : IRouteInterceptor {
    override fun intercept(context: Context, path: String, extras: Bundle): Boolean {
        return false
    }
}
```

### Serialize custom objects

Register an object serializer through `SerializationProvider`, then pass an object with `withObject`. The implementation must safely handle untrusted input, enforce type restrictions, and report parse failures; do not deserialize arbitrary types from unknown sources. Parser exceptions remain attached as the cause, making the actual failure visible instead of silently falling back to a field's default value.

## R8 and obfuscation rules

The `krouter-api` AAR includes focused consumer rules for route, Provider, and interceptor tables loaded through reflection, plus generated Injectors and their target class names. The rules also preserve the `TypeToken<T>` generic signatures required for custom-object injection. You do not need a broad keep rule for the entire `com.github.richardwrq.krouter` package.

If the application uses another reflection mechanism outside KRouter, provide precise rules for that separate reflection contract.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting code. Report security issues privately according to [SECURITY.md](SECURITY.md). Do not open a public issue containing exploit details before reporting it privately.

See [CHANGELOG.md](CHANGELOG.md) for the maintained change history.

## License

KRouter is licensed under the [Apache License 2.0](LICENSE).
Published AAR/JAR archives also carry `META-INF/LICENSE`, and generated Maven POMs declare the same license.
