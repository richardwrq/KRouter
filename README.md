## KRouter
```使用Kotlin打造的Android平台路由框架，实现了主流路由框架的大部分功能，支持依赖注入```

##### [![Join the chat at https://gitter.im/richardwrq/KRouter](https://badges.gitter.im/richardwrq/KRouter.svg)](https://gitter.im/richardwrq/KRouter?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

---

krouter-gradle-plugin|krouter-api|krouter-compiler|krouter-annotation
:---:|:---:|:---:|:---:
[ ![Download](https://api.bintray.com/packages/qq263454190/Richard_Maven/krouter-gradle-plugin/images/download.svg) ](https://bintray.com/qq263454190/Richard_Maven/krouter-gradle-plugin/_latestVersion)|[ ![Download](https://api.bintray.com/packages/qq263454190/Richard_Maven/krouter-api/images/download.svg) ](https://bintray.com/qq263454190/Richard_Maven/krouter-api/_latestVersion)|[ ![Download](https://api.bintray.com/packages/qq263454190/Richard_Maven/krouter-compiler/images/download.svg) ](https://bintray.com/qq263454190/Richard_Maven/krouter-compiler/_latestVersion)|[ ![Download](https://api.bintray.com/packages/qq263454190/Richard_Maven/krouter-annotation/images/download.svg) ](https://bintray.com/qq263454190/Richard_Maven/krouter-annotation/_latestVersion)



支持功能
--------
- 支持启动Activity、Service以及获取Fragment
- 添加拦截器、按优先级依次调用拦截，优先级为整形数，数值越大优先级越低
- 支持模糊匹配、自由配置路由路径的pathPrefix、pathPattern
- 支持Java、Kotlin混编项目
- 支持InstantRun、MultiDex
- 支持依赖注入

Download
--------
在项目根目录``build.gradle``文件中加入如下配置


```
buildscript {
       ext.versions = [
               'kotlin'        : '1.2.21',
       ]
       repositories {
               jcenter()
       }
       dependencies {
           classpath "com.github.richardwrq:krouter-gradle-plugin:0.1.1"
           classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${versions.kotlin}"
       }
   }
```
各Module``build.gradle``文件加入
```
apply plugin: 'kotlin-android'
apply plugin: 'kotlin-kapt'
apply plugin: "com.github.richardwrq.krouter"
```

功能介绍
--------
1. 添加``@Route``注解
```
//目前@Route支持的组件为Activity、Service、Fragment
@Route(path = "krouter/sample/Main2Activity")
class Main2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
    }
}
```
2. SDK初始化
```
public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        KRouter.openDebug();//打开KRouter调试日志
        KRouter.init(this);
    }
}
```
3. 发起路由请求
启动**Activity**:
```
KRouter.INSTANCE.create("krouter/sample/Main2Activity")
        .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .withString("param", "test")//传入请求参数
        .request();
```
获取**Fragment**:
```
MyFragment fragment = (MyFragment) KRouter.INSTANCE.create("myfragment").request();
```
启动**Service**:

bind方式
```
KRouter.INSTANCE
        .create("krouter/sample/BindService?test=this is MyService")//路径中的参数将会被自动解析并存入bundle中
        .withServiceFlags(BIND_AUTO_CREATE)
        .withServiceConn(new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Toast.makeText(MainActivity.this, name.getClassName() + " bind 成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Toast.makeText(MainActivity.this, name.getClassName() + " disconnected", Toast.LENGTH_SHORT).show();
            }
        })
        .request()
```
start方式
```
KRouter.INSTANCE.create("krouter/sample/MyService").request();
```
Proguard
--------
```
-keep class com.github.richardwrq.krouter.** {*;}
-keep class * implements com.github.richardwrq.krouter.api.interfaces.IInjector {*;}
#如果在非四大组件的类中使用了依赖注入，需要把这些类保留，比如
-keep class com.github.richardwrq.krouter.fragment.Fragment1
```

更多功能
--------
1. 声明拦截器
```
/**
 * priority为优先级，数值越大、优先级越低
 * @author Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/1/26 上午11:12
 */
@Interceptor(priority = 1, name = "Interceptor1")
public class Interceptor1 implements IRouteInterceptor {
    @Override
    public boolean intercept(@NotNull Context context, @NotNull String path, @NotNull Bundle extras) {
        Log.i("Interceptor1", "Interceptor1 invoke, path: " + path);
        return false;//返回true表示拦截
    }
}
```
2. 依赖注入实现解耦
首先注册服务
```
/**
 * 被注册的服务可以是任意类型，但是当被注册的服务实现了IProvider接口时，init(ApplicationContext)函数会被调用
 * 被注册的类必须存在无参构造函数
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/8 下午3:55
 */
@Provider("provider/myprovider")
public class MyProvider implements IProvider {
    @Override
    public void init(@NotNull Context context) {
        Toast.makeText(context, "MyProvider init", Toast.LENGTH_SHORT).show();
        Log.i("MyProvider", "init!");
    }
    
    public void helloWorld() {
        Log.i("MyProvider", "hello world!!!");
    }
}
```
服务的注入
```
class Main2Activity : AppCompatActivity() {

    @Inject("person")
    lateinit var person: Person

    //NoImplProvider服务
    @Inject("NoImplProvider")
    lateinit var provider: NoImplProvider

    //MyProvider服务
    @Inject("provider/myprovider")
    lateinit var myProvider: MyProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        KRouter.inject(this)//调用该方法会从bundle中取出请求参数为上述字段赋值，如果被注解的字段是一个服务，则会自动实例化一个服务进行赋值
        myProvider.helloWorld()
    }
}
```
3. 订阅路由处理回调
```
KRouter.INSTANCE.create(RouterTable.MY_SERVICE_PATH + "?test=this is MyService")
        .subscribeNotFound(new Function2<KRouter.Navigator, String, Unit>() {
            @Override
            public Unit invoke(KRouter.Navigator navigator, String s) {
                Toast.makeText(MainActivity.this, "未找到该路由: " + s, Toast.LENGTH_SHORT).show();
                return null;
            }
        })
        .subscribeArrived(new Function2<KRouter.Navigator, String, Unit>() {
            @Override
            public Unit invoke(KRouter.Navigator navigator, String s) {//执行跳转后回调
                Toast.makeText(MainActivity.this, "subscribeArrived : " + s, Toast.LENGTH_SHORT).show();
                return null;
            }
        })
        .subscribeBefore(new Function2<KRouter.Navigator, String, Unit>() {
            @Override
            public Unit invoke(KRouter.Navigator navigator, String s) {//执行跳转前回调
                Toast.makeText(MainActivity.this, "subscribeBefore : " + s, Toast.LENGTH_SHORT).show();
                return null;
            }
        })
        .subscribeRouteIntercept(new Function2<KRouter.Navigator, String, Unit>() {
            @Override
            public Unit invoke(KRouter.Navigator navigator, String s) {//路由请求被拦截时回调
                Toast.makeText(MainActivity.this, "subscribeRouteIntercept : " + s, Toast.LENGTH_SHORT).show();
                return null;
            }
        })
        .request();
```
4. 自定义对象的解析

声明对象解析器：
```
/**
 * 加入在发起路由请求时通过withObject传入了自定义对象，那么就需要定义一个实现SerializationProvider接口
 * 的类，使用@Provider(com.github.richardwrq.krouter.api.utils.Const.SERIALIZE_PATH)注解
 * 这里以使用Gson序列化对象为例
 *
 * @author WuRuiQiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/2/27 下午5:38
 */
@Provider(SERIALIZE_PATH)
class GsonProvider : SerializationProvider {

    private lateinit var gson: Gson

    override fun init(context: Context) {
        gson = Gson()
    }

    override fun <T> parseObject(text: String?, clazz: Type): T? {
        return gson.fromJson(text, clazz)
    }

    override fun serialize(instance: Any): String {
        return gson.toJson(instance)
    }
}
```
发起路由请求时传入自定义对象：
```
KRouter.INSTANCE.create(RouterTable.MAIN2_ATY_PATH)
                .withObject("person", new Person())
                .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .request();
```
被启动的页面实现对自定义对象的解析
```
@Route(path = RouterTable.MAIN2_ATY_PATH)
class Main2Activity : AppCompatActivity() {

    @Inject("person")
    lateinit var person: Person

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        KRouter.inject(this)//调用该方法开始解析自定义对象并进行赋值
        findViewById<TextView>(R.id.tv).text = person.toString()
    }
}
```

License
-------

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


 [1]: https://square.github.io/okhttp
 [2]: https://github.com/square/okhttp/wiki
 [3]: https://search.maven.org/remote_content?g=com.squareup.okhttp3&a=okhttp&v=LATEST
 [4]: https://search.maven.org/remote_content?g=com.squareup.okhttp3&a=mockwebserver&v=LATEST
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
