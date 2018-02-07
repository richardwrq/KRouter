package com.github.richardwrq.krouter.compiler.processor

import com.github.richardwrq.krouter.annotation.*
import com.github.richardwrq.krouter.annotation.RouteType.*
import com.github.richardwrq.krouter.annotation.model.InterceptorMetaData
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.google.auto.service.AutoService
import com.github.richardwrq.krouter.compiler.Logger
import com.squareup.kotlinpoet.*
import java.io.File
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.collections.HashMap
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName


abstract class BaseProcessor : AbstractProcessor() {

    protected lateinit var mElements: Elements
    protected lateinit var mTypes: Types
    protected lateinit var mLogger: Logger
    protected lateinit var mFormatModuleName: String
    protected lateinit var mOriginalModuleName: String
    protected lateinit var mAssetsPath: String

    override fun init(p0: ProcessingEnvironment) {
        super.init(p0)
        mElements = p0.elementUtils
        mTypes = p0.typeUtils
        mLogger = Logger(p0.messager)

        val options = p0.options
        if (options.isNotEmpty()) {
            mOriginalModuleName = options[com.github.richardwrq.krouter.compiler.MODULE_NAME] ?: ""
            mFormatModuleName = mOriginalModuleName.replace("[^0-9a-zA-Z_]+".toRegex(), "")
            mAssetsPath = options[com.github.richardwrq.krouter.compiler.ASSETS_PATH] ?: ""
        }
        mLogger.info("[$mOriginalModuleName] ${this::class.java.simpleName} init")
    }

    override fun process(set: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {

        if (set.isEmpty()) {
            return false
        }

        if (mOriginalModuleName.isBlank()) {
            mLogger.warning("this module name is null!!! skip this module!!")
            return false
        }

//        if (mAssetsPath.isBlank()) {
//            mLogger.warning("tis module assets path is null!!! skip this module!!")
//            return false
//        }

        try {
            mLogger.info("[$mOriginalModuleName] ${this::class.java.simpleName} process!!!")
            collectInfo(roundEnv)
        } catch (e: Exception) {
            mLogger.error(e)
        }

        return true
    }

    fun FileSpec.writeFile() {

        val kaptKotlinGeneratedDir = processingEnv.options[com.github.richardwrq.krouter.compiler.KAPT_KOTLIN_GENERATED_OPTION_NAME]
        val outputFile = File(kaptKotlinGeneratedDir).apply {
            mkdirs()
        }
        writeTo(outputFile.toPath())
    }

    abstract fun collectInfo(roundEnv: RoundEnvironment)

}

/**
 * Route Processor
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/2 上午11:54
 */
@AutoService(Processor::class)
@SupportedOptions(com.github.richardwrq.krouter.compiler.MODULE_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(value = ["com.github.richardwrq.krouter.annotation.Route"])
class RouteProcessor : BaseProcessor() {

    private val routeMap = HashMap<String, RouteMetadata>()

    override fun collectInfo(roundEnv: RoundEnvironment) {
        routeMap.clear()
        val elements = roundEnv.getElementsAnnotatedWith(Route::class.java)
        if (elements.isEmpty()) {
            return
        }
        mLogger.info("Found ${elements.size} routes in [$mOriginalModuleName]")

        val tmActivity = mElements.getTypeElement(ACTIVITY.className).asType()
        val tmService = mElements.getTypeElement(SERVICE.className).asType()
        val tmFragment = mElements.getTypeElement(FRAGMENT.className).asType()
        val tmFragmentV4 = mElements.getTypeElement(FRAGMENT_V4.className)?.asType()
        val tmContentProvider = mElements.getTypeElement(CONTENT_PROVIDER.className).asType()

        elements.forEach {
            val routeAnn = it.getAnnotation(Route::class.java)
            val routeType = when {
                mTypes.isSubtype(it.asType(), tmActivity) -> {
                    mLogger.info("Found Activity ${it.asType()}")
                    ACTIVITY
                }
                mTypes.isSubtype(it.asType(), tmService) -> {
                    mLogger.info("Found Service ${it.asType()}")
                    SERVICE
                }
                mTypes.isSubtype(it.asType(), tmFragment) -> {
                    mLogger.info("Found Fragment ${it.asType()}")
                    FRAGMENT
                }
                mTypes.isSubtype(it.asType(), tmFragmentV4) -> {
                    mLogger.info("Found Fragment_v4 ${it.asType()}")
                    FRAGMENT_V4
                }
                mTypes.isSubtype(it.asType(), tmContentProvider) -> {
                    mLogger.info("Found Content Provider ${it.asType()}")
                    CONTENT_PROVIDER
                }
                else -> {
                    mLogger.info("Unknown route ${it.asType()}")
                    UNKNOWN
                }
            }
            if (routeAnn.path.isNotBlank()) {
                if (routeMap.containsKey(routeAnn.path)) {
                    mLogger.warning("The route ${routeMap[routeAnn.path]?.className} already has Path { ${routeAnn.path} }, so skip route ${it.asType()}")
                    return@forEach
                }
                val routeMetaData = RouteMetadata(routeType, routeAnn.priority, routeAnn.name.trim(), routeAnn.path.trim(), routeAnn.pathPrefix.trim(), routeAnn.pathPattern.trim(), it.asType().toString())
                routeMap[routeAnn.path] = routeMetaData
            }
//            pack = processingEnv.elementUtils.getPackageOf(it).toString()
        }
        generateKotlin()
    }

    private fun generateKotlin() {

        /**
         * generate map code like:
         * Map<String, RouteMetadata>
         */
        val mapTypeOfRouteLoader = ParameterizedTypeName.get(HashMap::class, String::class, RouteMetadata::class)

        //Generate implement IRouteLoader interface class
        val routeLoaderFunSpecBuild = FunSpec.builder("loadInto")
                .addParameter("map", mapTypeOfRouteLoader)
                .addModifiers(KModifier.OVERRIDE)
        routeMap.forEach { s, routeMetaData ->
            routeLoaderFunSpecBuild.addStatement(
                    "map[\"$s\"] = %T(%T.%L, %L, %S, %S, %S, %S, %S)",
                    RouteMetadata::class,
                    RouteType::class,
                    routeMetaData.routeType,
                    routeMetaData.priority,
                    routeMetaData.name,
                    routeMetaData.path,
                    routeMetaData.pathPrefix,
                    routeMetaData.pathPattern,
                    routeMetaData.className)
        }
        val typeIRouteLoader = TypeSpec.classBuilder("${com.github.richardwrq.krouter.compiler.ROUTE_LOADER_NAME}${com.github.richardwrq.krouter.compiler.SEPARATOR}$mFormatModuleName")
                .addSuperinterface(ClassName.bestGuess(com.github.richardwrq.krouter.compiler.IROUTE_LOADER))
                .addFunction(routeLoaderFunSpecBuild.build())
                .build()

        val kotlinFile = FileSpec.builder(com.github.richardwrq.krouter.compiler.PACKAGE, "${com.github.richardwrq.krouter.compiler.ROUTE_LOADER_NAME}${com.github.richardwrq.krouter.compiler.SEPARATOR}$mFormatModuleName")
                .addType(typeIRouteLoader)
                .build()
        kotlinFile.writeFile()

    }
}

/**
 * Interceptor Processor
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/2 上午11:54
 */
@AutoService(Processor::class)
@SupportedOptions(com.github.richardwrq.krouter.compiler.MODULE_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(value = ["com.github.richardwrq.krouter.annotation.Interceptor"])
class InterceptorProcessor : BaseProcessor() {

    private val interceptorMap = TreeMap<Int, InterceptorMetaData>()

    override fun collectInfo(roundEnv: RoundEnvironment) {
        interceptorMap.clear()
        val elements = roundEnv.getElementsAnnotatedWith(Interceptor::class.java)
        if (elements.isEmpty()) {
            return
        }

        val tmIInterceptor = mElements.getTypeElement(com.github.richardwrq.krouter.compiler.IINTERCEPTOR).asType()

        /**
         * generate map code like:
         * Map<String, InterceptorMetaData>
         * 使用HashMap而不是MutableMap接口作为参数类型是因为用MutableMap::class作为参数最终生成的kotlin代码类型是Map，而Map在kotlin中是只读的
         */
        val mapTypeOfRouteLoader = ParameterizedTypeName.get(TreeMap::class, Int::class, InterceptorMetaData::class)

        val interceptorLoaderFun = FunSpec.builder("loadInto")
                .addParameter("map", mapTypeOfRouteLoader)
                .addModifiers(KModifier.OVERRIDE)

        elements.forEach {
            val interceptorAnn = it.getAnnotation(Interceptor::class.java)

            if (mTypes.isSubtype(it.asType(), tmIInterceptor)) {
                val interceptorClass = it.asType().toString()
                if (interceptorMap.containsKey(interceptorAnn.priority)) {
                    val existClass = interceptorMap[interceptorAnn.priority]?.className
                    mLogger.warning("Priority [${interceptorAnn.priority}] interceptor [$existClass] already exist, $interceptorClass will be skip")
                    return@forEach
                }
                interceptorMap[interceptorAnn.priority] = InterceptorMetaData(interceptorAnn.priority, interceptorAnn.name.trim(), interceptorClass)
                mLogger.info("Found Interceptor ${it.asType()} in [$mOriginalModuleName]")
                interceptorLoaderFun.addStatement(
                        "map[${interceptorAnn.priority}] = %T(%L, %S, %S)",
                        InterceptorMetaData::class,
                        interceptorAnn.priority,
                        interceptorAnn.name.trim(),
                        interceptorClass)
            } else {
                mLogger.warning("Interceptor ${it.simpleName} does not impl IInterceptor")
            }
        }

        val typeInterceptorLoader = TypeSpec.classBuilder("${com.github.richardwrq.krouter.compiler.INTERCEPTOR_LOADER_NAME}${com.github.richardwrq.krouter.compiler.SEPARATOR}$mFormatModuleName")
                .addSuperinterface(ClassName.bestGuess(com.github.richardwrq.krouter.compiler.IINTERCEPTOR_LOADER))
                .addFunction(interceptorLoaderFun.build())
                .build()

        val kotlinFile = FileSpec.builder(com.github.richardwrq.krouter.compiler.PACKAGE, "${com.github.richardwrq.krouter.compiler.INTERCEPTOR_LOADER_NAME}${com.github.richardwrq.krouter.compiler.SEPARATOR}$mFormatModuleName")
                .addType(typeInterceptorLoader)
                .build()
        kotlinFile.writeFile()
    }
}

/**
 * Interceptor Processor
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/26 上午11:54
 */
@AutoService(Processor::class)
@SupportedOptions(com.github.richardwrq.krouter.compiler.MODULE_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(value = ["com.github.richardwrq.krouter.annotation.Provider"])
class ProviderProcessor : BaseProcessor() {

    private val providerMap = HashMap<String, String>()

    override fun collectInfo(roundEnv: RoundEnvironment) {
        providerMap.clear()
        val elements = roundEnv.getElementsAnnotatedWith(Provider::class.java)
        if (elements.isEmpty()) {
            return
        }

        /**
         * generate map code like:
         * Map<String, Class>
         */
//        val classTypeOfProviderLoader = ParameterizedTypeName.get(ClassName.bestGuess(Class::class.java.name), mElements.getTypeElement(IPROVIDER).asType().asTypeName())
        val mapTypeOfProviderLoader = ParameterizedTypeName.get(
                HashMap::class.asClassName(),
                String::class.asClassName(),
                ParameterizedTypeName.get(ClassName.bestGuess(Class::class.java.name), TypeVariableName.invoke("*")))

        val providerLoaderFun = FunSpec.builder("loadInto")
                .addParameter("map", mapTypeOfProviderLoader)
                .addModifiers(KModifier.OVERRIDE)

        elements.forEach {
            val providerAnn = it.getAnnotation(Provider::class.java)

            mLogger.info("Found Provider ${it.asType()} in [$mOriginalModuleName]")
//            if (mTypes.isSubtype(it.asType(), tmProvider)) {
            providerLoaderFun.addStatement(
                    "map[\"${providerAnn.value}\"] = %T::class.java", it.asType())
//            } else {
//                mLogger.warning("Provider ${it.simpleName} does not impl IProvider")
//                return@forEach
//            }
        }

        val typeProviderLoader = TypeSpec.classBuilder("${com.github.richardwrq.krouter.compiler.PROVIDER_LOADER_NAME}${com.github.richardwrq.krouter.compiler.SEPARATOR}$mFormatModuleName")
                .addSuperinterface(ClassName.bestGuess(com.github.richardwrq.krouter.compiler.IPROVIDER_LOADER))
                .addFunction(providerLoaderFun.build())
                .build()

        val kotlinFile = FileSpec.builder(com.github.richardwrq.krouter.compiler.PACKAGE, "${com.github.richardwrq.krouter.compiler.PROVIDER_LOADER_NAME}${com.github.richardwrq.krouter.compiler.SEPARATOR}$mFormatModuleName")
                .addType(typeProviderLoader)
                .build()
        kotlinFile.writeFile()
    }
}

/**
 * Interceptor Processor
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/26 上午11:54
 */
@SupportedOptions(com.github.richardwrq.krouter.compiler.MODULE_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(value = ["com.github.richardwrq.krouter.annotation.Inject"])
class InjectProcessor : BaseProcessor() {

    override fun collectInfo(roundEnv: RoundEnvironment) {
        val elements = roundEnv.getElementsAnnotatedWith(Inject::class.java)
        if (elements.isEmpty()) {
            return
        }

        val injectorFun = FunSpec.builder("inject")
                .addParameter("instance", Any::class.java)
                .addModifiers(KModifier.INTERNAL)

        elements.forEachIndexed { index, element ->
            val injectAnn = element.getAnnotation(Inject::class.java)
            if (index == 0) {
                injectorFun.receiver(element.asType().asTypeName())
            }

            injectorFun.addStatement("")
        }
    }
}