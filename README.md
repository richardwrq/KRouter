# KRouter

KRouter 是一个面向 Android 的注解驱动路由框架，支持 Activity、Service、Fragment、拦截器、Provider 和字段注入，也支持 Java/Kotlin 混合工程。

> **Revival beta:** 当前维护线为 `0.2.0-beta.1`。源码已迁移到现代 Android/Gradle 工具链，但 API 与二进制兼容性尚未稳定，也没有可供生产使用的稳定 Maven 制品。若提供 GitHub prerelease 附件，它们仅用于源码评估和兼容性测试；历史 Bintray/JCenter 下载方式已经失效，请勿继续使用旧坐标。

## 当前能力

- 通过 `@Route` 注册 Activity、Service 和 Fragment 路由。
- 使用路径、参数、Intent flags 和 Service 连接选项发起路由请求。
- 通过 `@Interceptor` 按优先级执行路由拦截。
- 通过 `@Provider` 注册服务，并使用 `@Inject` 注入参数或服务。
- 通过 `SerializationProvider` 扩展自定义对象序列化。
- 编译期生成路由、Provider、拦截器和注入器代码。
- 支持 Java/Kotlin 混合模块和 MultiDex 应用。

外部 Intent/深链入口属于安全敏感边界。`KRouter.init(context)` 默认拒绝所有外部深链；应用只有传入显式 allowlist 后才会开放精确的 URI 和 Activity 路由映射。

## 从源码构建

需要：

- JDK 17
- Android SDK Platform 35
- 仓库自带的 Gradle Wrapper（无需单独安装 Gradle）

```bash
git clone https://github.com/richardwrq/KRouter.git
cd KRouter
./gradlew --no-daemon clean check assembleDebug lint
```

示例 APK 生成在 `app/build/outputs/apk/debug/`。CI 使用同一套 JDK 17、测试、组装和 lint 检查。

## 源码评估与集成状态

仓库内的示例模块直接依赖本仓库中的源码模块：

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

这段配置仅适用于本仓库源码构建，不代表已经发布了可供外部项目解析的插件或库坐标。在维护版制品发布并经过兼容性验证前，建议通过示例应用评估，不要依赖旧的 `0.1.x` Bintray/JCenter 制品。

如需检查 Maven metadata、POM、源码包和 Gradle plugin marker，可以生成仅位于仓库 `build/repository/` 的本地测试仓库：

```bash
./gradlew --no-daemon clean
./gradlew --no-daemon publishLibrariesToBuildRepository publishPluginToBuildRepository
```

`clean` 必须作为单独一次 Gradle 调用先完成，以免和 included plugin build 的 publication 任务竞争。上述任务不需要凭证，不写入 Maven Local，也不会上传到远端；`build/repository/` 中的 `0.2.0-beta.1` 仅用于检查当前源码产物，不是公共 Maven 发布。

外部消费者测试只从这个工作流本地仓库解析精确版本的 KRouter 制品。这些第一方制品由同一次检出的源码现场构建并通过 API、KAPT、R8 和运行入口检查，因此不固定跨平台可能变化的本地制品哈希；所有从 Google Maven 或 Maven Central 获取的第三方依赖仍执行严格的 SHA-256 校验。

## 基本用法

### 注册路由

```kotlin
@Route(path = "/krouter/sample/Main2Activity")
class Main2Activity : AppCompatActivity()
```

### 初始化

不接收外部深链的应用使用默认初始化；该重载对所有外部 URI 保持 deny-all：

```kotlin
class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KRouter.init(this)
    }
}
```

调试期间可以在初始化前调用 `KRouter.openDebug()` 打开日志；生产构建应根据需要控制日志输出。

### 显式允许外部深链

需要接收外部深链时，用下面的策略初始化替换前面的默认调用。为每个外部入口声明精确的 scheme、host、path 和允许的 query key，然后在初始化时冻结策略：

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

Manifest 过滤器也应保持同样的精确边界：

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

目标 Activity 也必须在合并 Manifest 中显式启用并保持 `android:exported="false"`。未列出的 query key、重复 key、端口、userinfo、fragment、非规范化 path 和非 `ACTION_VIEW` Intent 会被拒绝；ClipData 和 selector 同样会被拒绝。传入 Intent 的 extras 与 flags 不会跨越该边界，component 只能为空或指向当前应用的 `SchemeFilterActivity`。allowlist 中的 path 与声明的 Activity 必须在初始化时精确对应；自定义模糊匹配器不能扩大外部入口。

### 发起请求

```kotlin
KRouter.create("/krouter/sample/Main2Activity")
    .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    .withString("param", "test")
    .request()
```

获取 Fragment：

```kotlin
val fragment = KRouter.create("/krouter/sample/fragment1").request() as Fragment1
```

启动 Service：

```kotlin
KRouter.create("/krouter/sample/MyService").request()
```

### 注册和获取 Provider

Provider 必须提供无参构造方式。实现 `IProvider` 时，KRouter 会在实例创建后调用 `init(context)`。

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

Provider key 不得为空或重复。key 会进入编译期生成代码，因此变更后应运行完整的 `check` 任务。

### 注入参数或 Provider

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

`isRequired = true` 会在值或 Provider 缺失时立即抛出异常。默认的可选注入只在 key 确实不存在且没有对应 Provider 时保留初始值，因此可选字段应提供初始值；不要把未初始化的 `lateinit` 字段当作可选字段。若 Bundle 已包含 key，空值、类型不匹配、缺少 `SerializationProvider` 或反序列化失败都会抛出带字段和 key 上下文的异常，不会被当作“未提供”。

运行时只会把以 `KRouter_` 开头的 asset 当作模块 marker；`transferModuleName` 对其他输入会抛出 `IllegalArgumentException`，避免把任意文件名误识别为生成表。

### 声明拦截器

数值越小，拦截器优先级越高；返回 `true` 表示终止当前路由请求。

```kotlin
@Interceptor(priority = 1, name = "Authentication")
class AuthenticationInterceptor : IRouteInterceptor {
    override fun intercept(context: Context, path: String, extras: Bundle): Boolean {
        return false
    }
}
```

### 自定义对象序列化

使用 `SerializationProvider` 注册对象序列化实现，并在请求中通过 `withObject` 传入对象。序列化实现负责处理不可信输入、类型限制和解析失败；不要反序列化来源不明的任意类型。解析器抛出的异常会作为 cause 保留，便于定位具体失败，而不是静默回退到字段默认值。

## 混淆规则

`krouter-api` AAR 已内置最小 consumer rules，用于保留运行时反射加载的路由表、Provider 表、拦截器表、Injector 及其目标类名，并保留自定义对象注入所需的 `TypeToken<T>` 泛型签名；不需要再对整个 `com.github.richardwrq.krouter` 包添加 broad keep。

如果应用还通过 KRouter 之外的自定义反射机制访问类型或成员，仍需为那部分反射契约单独提供精确规则。

## 参与维护

提交代码前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。安全问题请按照 [SECURITY.md](SECURITY.md) 私下报告，不要先公开包含利用细节的 Issue。

变更记录见 [CHANGELOG.md](CHANGELOG.md)。

## License

KRouter is licensed under the [Apache License 2.0](LICENSE).
Published AAR/JAR archives also carry `META-INF/LICENSE`, and generated Maven POMs declare the same license.
