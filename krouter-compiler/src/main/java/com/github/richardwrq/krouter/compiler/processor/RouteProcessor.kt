package com.github.richardwrq.krouter.compiler.processor

import com.github.richardwrq.krouter.annotation.*
import com.github.richardwrq.krouter.annotation.RouteType.*
import com.github.richardwrq.krouter.annotation.model.InterceptorMetaData
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.compiler.*
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import org.jetbrains.annotations.NotNull
import java.io.File
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.collections.HashMap
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.platform.JavaToKotlinClassMap


abstract class BaseProcessor : AbstractProcessor() {

    protected lateinit var mElements: Elements
    protected lateinit var mTypes: Types
    protected lateinit var mLogger: Logger
    protected lateinit var mFormatModuleName: String
    protected lateinit var mOriginalModuleName: String

    override fun init(p0: ProcessingEnvironment) {
        super.init(p0)
        mElements = p0.elementUtils
        mTypes = p0.typeUtils
        mLogger = Logger(p0.messager)

        val options = p0.options
        if (options.isNotEmpty()) {
            mOriginalModuleName = options[MODULE_NAME] ?: ""
            mFormatModuleName = mOriginalModuleName.replace("[^0-9a-zA-Z_]+".toRegex(), "")
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

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
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
@SupportedOptions(MODULE_NAME)
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

        /**
         * generate map code like:
         * Map<String, RouteMetadata>
         * 因为用MutableMap::class作为参数最终生成的kotlin代码类型是Map，而Map在kotlin中是只读的
         */
        val mapTypeOfRouteLoader = ParameterizedTypeName.get(ClassName("kotlin.collections", "MutableMap"), String::class.asClassName(), RouteMetadata::class.asClassName())

        //Generate implement IRouteLoader interface class
        val routeLoaderFunSpecBuild = FunSpec.builder(METHOD_LOAD)
                .addParameter("map", mapTypeOfRouteLoader)
                .addModifiers(KModifier.OVERRIDE)

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
                    mLogger.warning("The route ${routeMap[routeAnn.path]?.name} already has Path { ${routeAnn.path} }, so skip route ${it.asType()}")
                    return@forEach
                }
                routeMap[routeAnn.path] = RouteMetadata(name = it.asType().toString())

                routeLoaderFunSpecBuild.addStatement(
                        "map[%S] = %T(%T.%L, %L, %S, %S, %S, %S, %T::class.java)",
                        routeAnn.path,
                        RouteMetadata::class,
                        RouteType::class,
                        routeType,
                        routeAnn.priority,
                        routeAnn.name,
                        routeAnn.path,
                        routeAnn.pathPrefix,
                        routeAnn.pathPattern,
                        it.asType())
            }
//            pack = processingEnv.elementUtils.getPackageOf(it).toString()
        }

        val typeIRouteLoader = TypeSpec.classBuilder("$ROUTE_LOADER_NAME$SEPARATOR$mFormatModuleName")
                .addSuperinterface(ClassName.bestGuess(ROUTE_LOADER))
                .addKdoc(WARNINGS)
                .addFunction(routeLoaderFunSpecBuild.build())
                .build()

        val kotlinFile = FileSpec.builder(PACKAGE, "$ROUTE_LOADER_NAME$SEPARATOR$mFormatModuleName")
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
@SupportedOptions(MODULE_NAME)
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

        val tmIInterceptor = mElements.getTypeElement(com.github.richardwrq.krouter.compiler.INTERCEPTOR).asType()

        /**
         * generate map code like:
         * Map<String, InterceptorMetaData>
         */
        val mapTypeOfRouteLoader = ParameterizedTypeName.get(TreeMap::class, Int::class, InterceptorMetaData::class)

        val interceptorLoaderFun = FunSpec.builder(METHOD_LOAD)
                .addParameter("map", mapTypeOfRouteLoader)
                .addModifiers(KModifier.OVERRIDE)

        elements.forEach {
            val interceptorAnn = it.getAnnotation(Interceptor::class.java)

            if (mTypes.isSubtype(it.asType(), tmIInterceptor)) {
                if (interceptorMap.containsKey(interceptorAnn.priority)) {
                    val existClass = interceptorMap[interceptorAnn.priority]?.name
                    mLogger.warning("Priority [${interceptorAnn.priority}] interceptor [$existClass] already exist, ${it.asType()} will be skip")
                    return@forEach
                }
                interceptorMap[interceptorAnn.priority] = InterceptorMetaData(name = it.asType().toString())
                mLogger.info("Found Interceptor ${it.asType()} in [$mOriginalModuleName]")
                interceptorLoaderFun.addStatement(
                        "map[${interceptorAnn.priority}] = %T(%L, %S, %T::class.java)",
                        InterceptorMetaData::class,
                        interceptorAnn.priority,
                        interceptorAnn.name.trim(),
                        it.asType())
            } else {
                mLogger.warning("Interceptor ${it.simpleName} does not impl IInterceptor")
            }
        }

        val typeInterceptorLoader = TypeSpec.classBuilder("$INTERCEPTOR_LOADER_NAME$SEPARATOR$mFormatModuleName")
                .addKdoc(WARNINGS)
                .addSuperinterface(ClassName.bestGuess(INTERCEPTOR_LOADER))
                .addFunction(interceptorLoaderFun.build())
                .build()

        val kotlinFile = FileSpec.builder(PACKAGE, "$INTERCEPTOR_LOADER_NAME$SEPARATOR$mFormatModuleName")
                .addType(typeInterceptorLoader)
                .build()
        kotlinFile.writeFile()
    }
}

/**
 * Provider Processor
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/26 上午11:54
 */
@AutoService(Processor::class)
@SupportedOptions(MODULE_NAME)
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
                ClassName("kotlin.collections", "MutableMap"),
                String::class.asClassName(),
                ParameterizedTypeName.get(ClassName.bestGuess(Class::class.java.name), TypeVariableName.invoke("*")))

        val providerLoaderFun = FunSpec.builder(METHOD_LOAD)
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

        val typeProviderLoader = TypeSpec.classBuilder("$PROVIDER_LOADER_NAME$SEPARATOR$mFormatModuleName")
                .addKdoc(WARNINGS)
                .addSuperinterface(ClassName.bestGuess(PROVIDER_LOADER))
                .addFunction(providerLoaderFun.build())
                .build()

        val kotlinFile = FileSpec.builder(PACKAGE, "$PROVIDER_LOADER_NAME$SEPARATOR$mFormatModuleName")
                .addType(typeProviderLoader)
                .build()
        kotlinFile.writeFile()
    }
}

/**
 * Inject Processor
 *
 * @author: Wuruiqiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version: v1.0
 * @since: 18/1/26 上午11:54
 */

@AutoService(Processor::class)
@SupportedOptions(MODULE_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(value = ["com.github.richardwrq.krouter.annotation.Inject"])
class InjectProcessor : BaseProcessor() {

    private val categoryElement = HashMap<TypeElement, MutableList<Element>>()

    private lateinit var tmProvider: TypeMirror
    private lateinit var tmBundle: TypeMirror
    private lateinit var tmIllegalArgumentException: TypeMirror
    //  private lateinit var tmNPException: TypeMirror
    private lateinit var tmKRouter: TypeMirror

    override fun init(p0: ProcessingEnvironment) {
        super.init(p0)

        tmProvider = mElements.getTypeElement(PROVIDER).asType()
        tmBundle = mElements.getTypeElement(BUNDLE).asType()
        tmIllegalArgumentException = mElements.getTypeElement(ILLEGAL_ARGUMENT_EXCEPTION).asType()
//      tmNPException = mElements.getTypeElement(NP_EXCEPTION).asType()
        tmKRouter = mElements.getTypeElement(KROUTER).asType()

    }

    override fun collectInfo(roundEnv: RoundEnvironment) {
        categoryElement.clear()
        val elements = roundEnv.getElementsAnnotatedWith(Inject::class.java)
        categories(elements)
        if (elements.isEmpty()) {
            return
        }

        //extension func file
        val getBundleFun = buildGetBundleFun()

        val parseObjectFun = buildParseObjectFun()

        val extensionFileBuilder = FileSpec.builder(PACKAGE, "$INJECTOR_NAME$SEPARATOR$mFormatModuleName")
                .addComment(WARNINGS)
                .addFunction(getBundleFun)
                .addFunction(parseObjectFun)

        extensionFileBuilder.build().writeFile()

        categoryElement.forEach { parent, children ->

            val injectorFileBuilder = FileSpec.builder(parent.asClassName().packageName(), transferInjectorClassName(parent.qualifiedName.toString()))
                    .addComment(WARNINGS)
                    .addStaticImport(PACKAGE, METHOD_PARSE_OBJECT, METHOD_GET_BUNDLE)

            val exInjectFun = FunSpec.builder(METHOD_EX_INJECT)
                    .receiver(parent.asType().asTypeName())
                    .addParameter("bundle", tmBundle.asTypeName())
                    .addModifiers(KModifier.PRIVATE)

            val injectFun = FunSpec.builder(METHOD_INJECT)
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("any", Any::class)
                    .addParameter("extras", tmBundle.asTypeName().asNullable())
                    .addStatement("val bundle = getBundle(any, extras)")
                    .addStatement("(any as %T).exInject(bundle)", parent.asType().asTypeName())

            mLogger.info("start inject class ${parent.simpleName}")
            children.forEach { element ->
                val injectAnn = element.getAnnotation(Inject::class.java)
                val key = if (injectAnn.name.isNotBlank()) {
                    injectAnn.name
                } else {
                    element.simpleName
                }
                if (element.kind != ElementKind.FIELD) {
                    throw IllegalArgumentException("Only field can be annotated with @${Inject::class.simpleName}")
                }

                mLogger.info("inject field [${element.simpleName}]")
                if (mTypes.isSubtype(element.asType(), tmProvider)) {
                    exInjectFun.addStatement("${element.simpleName} = (%T.getProvider<%T>(%S))${element.asNonNullable()}",
                            tmKRouter.asTypeName(),
                            element.asType().asTypeName(),
                            key)
                } else {
                    //获取Java-》kotlin映射类型，如 java.lang.String转化为kotlin.String，如无需映射则直接返回element类型即可
                    val className = element.javaToKotlinType() ?: element.asType().asTypeName()
                    exInjectFun.addStatement("${element.simpleName} = bundle.get(%S) as? %T ?: parseObject(bundle.getString(%S), object : %T() {}.getType())${element.asNonNullable()}",
                            key,
                            className,
                            key,
                            ParameterizedTypeName.get(ClassName.bestGuess(TYPE_TOKEN), className))
                }

            }

            val typeInjector = TypeSpec.classBuilder(transferInjectorClassName(parent.qualifiedName.toString()))
                    .addSuperinterface(ClassName.bestGuess(INJECTOR))
                    .addFunction(injectFun.build())
                    .addFunction(exInjectFun.build())
                    .build()

            injectorFileBuilder.addType(typeInjector)
            injectorFileBuilder.build().writeFile()
        }

    }

    /**
     * Categories field, find his papa. reference ARouter
     *
     * @param elements Field need inject
     */
    @Throws(IllegalAccessException::class)
    private fun categories(elements: Set<Element>) {
        if (elements.isNotEmpty()) {
            for (element in elements) {
                val enclosingElement = element.enclosingElement as TypeElement

                if (isJavaFile(enclosingElement) && element.modifiers.contains(Modifier.PRIVATE)) {
                    throw IllegalAccessException("The inject fields CAN NOT BE 'private'!!! please check field ["
                            + element.simpleName + "] in class [" + enclosingElement.qualifiedName + "]")
                }

                if (categoryElement.containsKey(enclosingElement)) { // Has categories
                    categoryElement[enclosingElement]?.add(element)
                } else {
                    val children = ArrayList<Element>()
                    children.add(element)
                    categoryElement[enclosingElement] = children
                }
            }

            mLogger.info("categories finished.")
        }
    }

    /**
     * if true mean this class is java class
     */
    private fun isJavaFile(element: TypeElement): Boolean {
        val tmMetadata = mElements.getTypeElement(METADATA).asType()
        return element.annotationMirrors.find { it.annotationType == tmMetadata } == null
    }

    private fun Element.asNonNullable(): String {
        val tmNonNull = mElements.getTypeElement(NONNULL).asType()

        return if (getAnnotation(NotNull::class.java) != null
                || this.annotationMirrors.find { it.annotationType == tmNonNull } != null) {
            " ?: throw $NP_EXCEPTION(\"Field [$simpleName] must not be null in [${enclosingElement.simpleName}]!\")"
        } else {
            ""
        }
    }

    /**
     * 获取需要把java类型映射成kotlin类型的ClassName  如：java.lang.String 在kotlin中的类型为kotlin.String 如果是空则表示该类型无需进行映射
     */
    private fun Element.javaToKotlinType(): ClassName? {
        val className = JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(this.asType().asTypeName().toString()))?.asSingleFqName()?.asString()
        return if (className == null) {
            null
        } else {
            ClassName.bestGuess(className)
        }
    }

    private fun buildGetBundleFun(): FunSpec {
        val tmActivity = mElements.getTypeElement(ACTIVITY.className).asType()
        val tmFragment = mElements.getTypeElement(FRAGMENT.className).asType()
        val tmFragmentV4 = mElements.getTypeElement(FRAGMENT_V4.className).asType()
        return FunSpec.builder(METHOD_GET_BUNDLE)
                .addModifiers(KModifier.INTERNAL)
                .addParameter("any", Any::class)
                .addParameter("extras", tmBundle.asTypeName().asNullable())
                .returns(tmBundle.asTypeName())
                .addCode("return extras ?: when (any) {\n" +
                        "            is %T -> {\n" +
                        "                any.intent.extras\n" +
                        "            }\n" +
                        "            is %T -> {\n" +
                        "                any.arguments\n" +
                        "            }\n" +
                        "            is %T -> {\n" +
                        "                any.arguments\n" +
                        "            }\n" +
                        "            else -> {\n" +
                        "                null\n" +
                        "            }\n" +
                        "        } ?: throw %T(%S)\n",
                        tmActivity.asTypeName(),
                        tmFragment.asTypeName(),
                        tmFragmentV4.asTypeName(),
                        tmIllegalArgumentException.asTypeName(),
                        "The field must be inject from bundle, the bundle can be passed from Activity or fragment or manually")
                .build()
    }

    private fun buildParseObjectFun(): FunSpec {
        val tmType = mElements.getTypeElement(TYPE).asType()
        val tmSerializeProvider = mElements.getTypeElement(SERIALIZE_PROVIDER).asType()
        val serializePathClassName = ClassName.bestGuess(SERIALIZE_PATH)

        return FunSpec.builder(METHOD_PARSE_OBJECT)
                .addModifiers(KModifier.INTERNAL)
                .addTypeVariable(TypeVariableName.invoke("T"))
                .returns(TypeVariableName.invoke("T").asNullable())
                .addParameter("text", String::class.asTypeName().asNullable())
                .addParameter("type", tmType.asTypeName())
                .addStatement("val serializeProvider = %T.getProvider<%T>(%T) ?: throw %T(%S)",
                        tmKRouter.asTypeName(),
                        tmSerializeProvider.asTypeName(),
                        serializePathClassName,
                        tmIllegalArgumentException.asTypeName(),
                        "Missing $tmSerializeProvider, Do you declare a class that implements the $tmSerializeProvider interface?")
                .addStatement("return serializeProvider.parseObject(text, type)")
                .build()

    }
}