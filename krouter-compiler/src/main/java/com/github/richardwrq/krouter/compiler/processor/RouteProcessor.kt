package com.github.richardwrq.krouter.compiler.processor

import com.github.richardwrq.krouter.annotation.*
import com.github.richardwrq.krouter.annotation.RouteType.*
import com.github.richardwrq.krouter.annotation.model.InterceptorMetaData
import com.github.richardwrq.krouter.annotation.model.RouteMetadata
import com.github.richardwrq.krouter.compiler.*
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy


abstract class BaseProcessor : AbstractProcessor() {

    protected lateinit var mElements: Elements
    protected lateinit var mTypes: Types
    protected lateinit var mLogger: Logger
    protected lateinit var mFormatModuleName: String
    protected lateinit var mOriginalModuleName: String
    private var generated = false

    override fun init(p0: ProcessingEnvironment) {
        super.init(p0)
        mElements = p0.elementUtils
        mTypes = p0.typeUtils
        mLogger = Logger(p0.messager)

        mOriginalModuleName = p0.options[MODULE_NAME].orEmpty()
        mFormatModuleName = mOriginalModuleName.replace("[^0-9a-zA-Z_]+".toRegex(), "")
        mLogger.info("[$mOriginalModuleName] ${this::class.java.simpleName} init")
    }

    override fun process(set: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (mOriginalModuleName.isBlank()) {
            mLogger.warning("this module name is null!!! skip this module!!")
            return false
        }

//        if (mAssetsPath.isBlank()) {
//            mLogger.warning("tis module assets path is null!!! skip this module!!")
//            return false
//        }

        try {
            if (roundEnv.processingOver()) {
                if (!generated) {
                    mLogger.info("[$mOriginalModuleName] ${this::class.java.simpleName} generate!!!")
                    generateCode()
                    generated = true
                }
            } else {
                mLogger.info("[$mOriginalModuleName] ${this::class.java.simpleName} collect!!!")
                collectInfo(roundEnv)
            }
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

    abstract fun generateCode()

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

    private data class CollectedRoute(
            val type: TypeName,
            val routeType: RouteType,
            val priority: Int,
            val name: String,
            val path: String,
            val pathPrefix: String,
            val pathPattern: String)

    private val routeMap = TreeMap<String, CollectedRoute>()
    private val routeOwners = LinkedHashMap<String, String>()

    override fun collectInfo(roundEnv: RoundEnvironment) {
        val elements = roundEnv.getElementsAnnotatedWith(Route::class.java)
        if (elements.isEmpty()) {
            return
        }
        mLogger.info("Found ${elements.size} routes in [$mOriginalModuleName]")

        val tmActivity = mElements.getTypeElement(ACTIVITY.className).asType()
        val tmService = mElements.getTypeElement(SERVICE.className).asType()
        val tmFragment = mElements.getTypeElement(FRAGMENT.className).asType()
        val tmFragmentV4 = mElements.getTypeElement(FRAGMENTX.className)?.asType()
        val tmContentProvider = mElements.getTypeElement(CONTENT_PROVIDER.className).asType()

        elements.sortedBy { it.asType().toString() }.forEach {
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
                tmFragmentV4 != null && mTypes.isSubtype(it.asType(), tmFragmentV4) -> {
                    mLogger.info("Found Fragment_v4 ${it.asType()}")
                    FRAGMENTX
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
            val typeName = it.asType().toString()
            registerRoutePath(routeOwners, routeAnn.path, typeName)
            routeMap[routeAnn.path] = CollectedRoute(
                    type = it.asType().asTypeName(),
                    routeType = routeType,
                    priority = routeAnn.priority,
                    name = routeAnn.name,
                    path = routeAnn.path,
                    pathPrefix = routeAnn.pathPrefix,
                    pathPattern = routeAnn.pathPattern)
        }
    }

    override fun generateCode() {
        if (routeMap.isEmpty()) {
            return
        }

        /**
         * generate map code like:
         * Map<String, RouteMetadata>
         * 因为用MutableMap::class作为参数最终生成的kotlin代码类型是Map，而Map在kotlin中是只读的
         */
        val map = ClassName("kotlin.collections", "MutableMap")
        val mapTypeOfRouteLoader = map.parameterizedBy(String::class.asClassName(), RouteMetadata::class.asClassName())

        val routeLoaderFunSpecBuild = FunSpec.builder(METHOD_LOAD)
                .addParameter("map", mapTypeOfRouteLoader)
                .addModifiers(KModifier.OVERRIDE)

        routeMap.values.forEach { route ->
            routeLoaderFunSpecBuild.addStatement(
                    "map[%S] = %T(%T.%L, %L, %S, %S, %S, %S, %T::class.java)",
                    route.path,
                    RouteMetadata::class,
                    RouteType::class,
                    route.routeType,
                    route.priority,
                    route.name,
                    route.path,
                    route.pathPrefix,
                    route.pathPattern,
                    route.type)
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

    private data class CollectedInterceptor(
            val type: TypeName,
            val typeName: String,
            val priority: Int,
            val name: String)

    private val interceptorMap = TreeMap<Int, CollectedInterceptor>()

    override fun collectInfo(roundEnv: RoundEnvironment) {
        val elements = roundEnv.getElementsAnnotatedWith(Interceptor::class.java)
        if (elements.isEmpty()) {
            return
        }

        val tmIInterceptor = mElements.getTypeElement(com.github.richardwrq.krouter.compiler.INTERCEPTOR).asType()

        elements.sortedBy { it.asType().toString() }.forEach {
            val interceptorAnn = it.getAnnotation(Interceptor::class.java)

            if (mTypes.isSubtype(it.asType(), tmIInterceptor)) {
                if (interceptorMap.containsKey(interceptorAnn.priority)) {
                    val existClass = interceptorMap[interceptorAnn.priority]?.typeName
                    throw IllegalArgumentException(
                            "Duplicate interceptor priority '${interceptorAnn.priority}': $existClass and ${it.asType()}")
                }
                interceptorMap[interceptorAnn.priority] = CollectedInterceptor(
                        type = it.asType().asTypeName(),
                        typeName = it.asType().toString(),
                        priority = interceptorAnn.priority,
                        name = interceptorAnn.name.trim())
                mLogger.info("Found Interceptor ${it.asType()} in [$mOriginalModuleName]")
            } else {
                mLogger.warning("Interceptor ${it.simpleName} does not impl IInterceptor")
            }
        }
    }

    override fun generateCode() {
        if (interceptorMap.isEmpty()) {
            return
        }

        /**
         * generate map code like:
         * Map<String, InterceptorMetaData>
         */
        val map = ClassName("java.util", "TreeMap")
        val mapTypeOfRouteLoader = map.parameterizedBy(Int::class.asTypeName(), InterceptorMetaData::class.asTypeName())

        val interceptorLoaderFun = FunSpec.builder(METHOD_LOAD)
                .addParameter("map", mapTypeOfRouteLoader)
                .addModifiers(KModifier.OVERRIDE)

        interceptorMap.values.forEach { interceptor ->
            interceptorLoaderFun.addStatement(
                    "map[%L] = %T(%L, %S, %T::class.java)",
                    interceptor.priority,
                    InterceptorMetaData::class,
                    interceptor.priority,
                    interceptor.name,
                    interceptor.type)
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

    private data class CollectedProvider(
            val type: TypeName,
            val key: String)

    private val providerMap = TreeMap<String, CollectedProvider>()
    private val providerOwners = LinkedHashMap<String, String>()

    override fun collectInfo(roundEnv: RoundEnvironment) {
        val elements = roundEnv.getElementsAnnotatedWith(Provider::class.java)
        if (elements.isEmpty()) {
            return
        }

        elements.sortedBy { it.asType().toString() }.forEach {
            val providerAnn = it.getAnnotation(Provider::class.java)
            val typeName = it.asType().toString()

            registerProviderKey(providerOwners, providerAnn.value, typeName)
            providerMap[providerAnn.value] = CollectedProvider(
                    type = it.asType().asTypeName(),
                    key = providerAnn.value)

            mLogger.info("Found Provider ${it.asType()} in [$mOriginalModuleName]")
        }
    }

    override fun generateCode() {
        if (providerMap.isEmpty()) {
            return
        }

        /**
         * generate map code like:
         * Map<String, Class>
         */
        val map = ClassName("kotlin.collections", "MutableMap")
        val clazz = ClassName.bestGuess(Class::class.java.name)
        val mapTypeOfProviderLoader = map.parameterizedBy(String::class.asClassName(),
                clazz.parameterizedBy(TypeVariableName.invoke("*")))

        val providerLoaderFun = FunSpec.builder(METHOD_LOAD)
                .addParameter("map", mapTypeOfProviderLoader)
                .addModifiers(KModifier.OVERRIDE)

        providerMap.values.forEach { provider ->
            providerLoaderFun.addCode("%L\n", providerAssignment(provider.key, provider.type))
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

    private data class CollectedInjectField(
            val name: String,
            val key: String,
            val type: TypeName,
            val required: Boolean,
            val provider: Boolean)

    private data class CollectedInjectTarget(
            val type: TypeName,
            val className: ClassName,
            val binaryName: String,
            val simpleName: String,
            val fields: TreeMap<String, CollectedInjectField> = TreeMap())

    private val injectTargets = TreeMap<String, CollectedInjectTarget>()

    private lateinit var tmProvider: TypeMirror
    private lateinit var tmBundle: TypeMirror
    private lateinit var tmKRouter: TypeMirror

    override fun init(p0: ProcessingEnvironment) {
        super.init(p0)

        tmProvider = mElements.getTypeElement(PROVIDER).asType()
        tmBundle = mElements.getTypeElement(BUNDLE).asType()
        tmKRouter = mElements.getTypeElement(KROUTER).asType()
    }

    override fun collectInfo(roundEnv: RoundEnvironment) {
        val elements = roundEnv.getElementsAnnotatedWith(Inject::class.java)
        if (elements.isEmpty()) {
            return
        }

        elements.sortedBy { "${it.enclosingElement}.${it.simpleName}" }.forEach { element ->
            if (element.kind != ElementKind.FIELD) {
                throw IllegalArgumentException("Only field can be annotated with @${Inject::class.simpleName}")
            }

            val parent = element.enclosingElement as TypeElement
            if (isJavaFile(parent) && element.modifiers.contains(Modifier.PRIVATE)) {
                throw IllegalAccessException("The inject fields CAN NOT BE 'private'!!! please check field ["
                        + element.simpleName + "] in class [" + parent.qualifiedName + "]")
            }

            val injectAnn = element.getAnnotation(Inject::class.java)
            val fieldName = element.simpleName.toString()
            val key = injectAnn.name.ifBlank { fieldName }
            val qualifiedName = parent.qualifiedName.toString()
            val binaryName = mElements.getBinaryName(parent).toString()
            val target = injectTargets.getOrPut(binaryName) {
                CollectedInjectTarget(
                        type = parent.asType().asTypeName(),
                        className = parent.asClassName(),
                        binaryName = binaryName,
                        simpleName = parent.simpleName.toString())
            }
            val previous = target.fields.putIfAbsent(
                    fieldName,
                    CollectedInjectField(
                            name = fieldName,
                            key = key,
                            type = element.javaToKotlinType().copy(nullable = false),
                            required = injectAnn.isRequired,
                            provider = mTypes.isSubtype(element.asType(), tmProvider)))
            require(previous == null) {
                "Duplicate injectable field '$qualifiedName.$fieldName'"
            }
            mLogger.info("Found injectable field [$qualifiedName.$fieldName]")
        }
    }

    override fun generateCode() {
        if (injectTargets.isEmpty()) {
            return
        }

        FileSpec.builder(PACKAGE, "$INJECTOR_NAME$SEPARATOR$mFormatModuleName")
                .addFileComment(WARNINGS)
                .addFunction(buildGetBundleFun())
                .addFunction(buildParseObjectFun())
                .build()
                .writeFile()

        injectTargets.values.forEach { target ->
            generateInjector(target)
        }
    }

    private fun generateInjector(target: CollectedInjectTarget) {
        val injectorFileBuilder = FileSpec.builder(
                target.className.packageName,
                transferInjectorClassName(target.binaryName))
                .addFileComment(WARNINGS)
                .addImport(PACKAGE, METHOD_PARSE_OBJECT, METHOD_GET_BUNDLE)

        val exInjectFun = FunSpec.builder(METHOD_EX_INJECT)
                .receiver(target.type)
                .addParameter("bundle", tmBundle.asTypeName())
                .addModifiers(KModifier.PRIVATE)

        val injectFun = FunSpec.builder(METHOD_INJECT)
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("any", Any::class)
                .addParameter("extras", tmBundle.asTypeName().copy(true))
                .addStatement("val bundle = getBundle(any, extras)")
                .addStatement("(any as %T).exInject(bundle)", target.type)

        mLogger.info("start inject class ${target.simpleName}")
        target.fields.values.forEachIndexed { index, field ->
            addFieldResolution(exInjectFun, field, index)
        }

        val typeInjector = TypeSpec.classBuilder(transferInjectorClassName(target.binaryName))
                .addSuperinterface(ClassName.bestGuess(INJECTOR))
                .addFunction(injectFun.build())
                .addFunction(exInjectFun.build())
                .build()

        injectorFileBuilder.addType(typeInjector)
        injectorFileBuilder.build().writeFile()
    }

    private fun addFieldResolution(
            function: FunSpec.Builder,
            field: CollectedInjectField,
            index: Int) {
        val resolvedName = "resolved$index"
        val valueType = field.type.copy(nullable = false)

        if (field.provider) {
            function.addStatement(
                    "val %N: %T = %T.getProvider<%T>(%S)",
                    resolvedName,
                    valueType.copy(nullable = true),
                    tmKRouter.asTypeName(),
                    valueType,
                    field.key)
        } else {
            val hasValueName = "hasValue$index"
            val rawValueName = "rawValue$index"
            val token = ClassName.bestGuess(TYPE_TOKEN).parameterizedBy(valueType)
            function.addStatement("val %N = bundle.containsKey(%S)", hasValueName, field.key)
            function.beginControlFlow(
                    "val %N: %T = if (%N)",
                    resolvedName,
                    valueType.copy(nullable = true),
                    hasValueName)
            function.addStatement("val %N = bundle.get(%S)", rawValueName, field.key)
            function.beginControlFlow(
                    "%N as? %T ?: when (%N)",
                    rawValueName,
                    valueType,
                    rawValueName)
            function.addStatement(
                    "null -> throw %T(%S)",
                    IllegalArgumentException::class,
                    "Injected value for field [${field.name}] from key [${field.key}] was null")
            function.addStatement(
                    "is %T -> parseObject<%T>(%N, object : %T() {}.getType(), %S, %S)",
                    String::class,
                    valueType,
                    rawValueName,
                    token,
                    field.key,
                    field.name)
            function.addStatement(
                    "else -> throw %T(%S + %N.javaClass.name + %S)",
                    IllegalArgumentException::class,
                    "Injected value for field [${field.name}] from key [${field.key}] has incompatible type [",
                    rawValueName,
                    "]")
            function.endControlFlow()
            function.nextControlFlow("else")
            function.addStatement(
                    "%T.getProvider<%T>(%S)",
                    tmKRouter.asTypeName(),
                    valueType,
                    field.key)
            function.endControlFlow()
        }

        if (field.required) {
            function.addStatement(
                    "this.%N = %N ?: throw %T(%S)",
                    field.name,
                    resolvedName,
                    NullPointerException::class,
                    "Field [${field.name}] must not be null in an injected instance")
        } else {
            function.beginControlFlow("if (%N != null)", resolvedName)
            function.addStatement("this.%N = %N", field.name, resolvedName)
            function.endControlFlow()
        }
    }

    /**
     * if true mean this class is java class
     */
    private fun isJavaFile(element: TypeElement): Boolean {
        val tmMetadata = mElements.getTypeElement(METADATA)?.asType() ?: return true
        return element.annotationMirrors.find { it.annotationType == tmMetadata } == null
    }

    /**
     * 获取需要把java类型映射成kotlin类型的ClassName  如：java.lang.String 在kotlin中的类型为kotlin.String 如果是空则表示该类型无需进行映射
     */
    private fun Element.javaToKotlinType(): TypeName =
            asType().asTypeName().toKotlinType()

    private fun buildGetBundleFun(): FunSpec {
        val tmActivity = mElements.getTypeElement(ACTIVITY.className).asType()
        val tmFragment = mElements.getTypeElement(FRAGMENT.className).asType()
        val tmFragmentV4 = mElements.getTypeElement(FRAGMENTX.className).asType()
        return FunSpec.builder(METHOD_GET_BUNDLE)
                .addModifiers(KModifier.INTERNAL)
                .addParameter("any", Any::class)
                .addParameter("extras", tmBundle.asTypeName().copy(true))
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
                        "        } ?: %T()\n",
                        tmActivity.asTypeName(),
                        tmFragment.asTypeName(),
                        tmFragmentV4.asTypeName(),
                        tmBundle.asTypeName())
                .build()
    }

    private fun buildParseObjectFun(): FunSpec {
        val tmType = mElements.getTypeElement(TYPE).asType()
        val tmSerializeProvider = mElements.getTypeElement(SERIALIZE_PROVIDER).asType()
        val serializePathClassName = ClassName.bestGuess(SERIALIZE_PATH)

        return FunSpec.builder(METHOD_PARSE_OBJECT)
                .addModifiers(KModifier.INTERNAL)
                .addTypeVariable(TypeVariableName.invoke("T"))
                .returns(TypeVariableName.invoke("T"))
                .addParameter("text", String::class)
                .addParameter("type", tmType.asTypeName())
                .addParameter("key", String::class)
                .addParameter("field", String::class)
                .addStatement("val serializeProvider = %T.getProvider<%T>(%T) ?: throw %T(%S + field + %S + key + %S)",
                        tmKRouter.asTypeName(),
                        tmSerializeProvider.asTypeName(),
                        serializePathClassName,
                        IllegalStateException::class,
                        "Missing SerializationProvider while injecting field [",
                        "] from key [",
                        "]")
                .beginControlFlow("val parsed = try")
                .addStatement("serializeProvider.parseObject<T>(text, type)")
                .nextControlFlow("catch (cause: %T)", Exception::class)
                .addStatement("throw %T(%S + field + %S + key + %S, cause)",
                        IllegalArgumentException::class,
                        "Unable to parse injected field [",
                        "] from key [",
                        "]")
                .endControlFlow()
                .addStatement("return parsed ?: throw %T(%S + field + %S + key + %S)",
                        IllegalArgumentException::class,
                        "SerializationProvider returned null while injecting field [",
                        "] from key [",
                        "]")
                .build()

    }
}
