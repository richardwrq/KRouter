package com.github.richardwrq.krouter.compiler.processor

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteProcessorDiagnosticTest {

    @Test
    fun `duplicate route generated in a later round is a compiler error`() {
        val result = compile(
                compilerSources("InitialRoute", "/duplicate"),
                listOf(DuplicateRouteGenerator(), RouteProcessor()))

        assertFalse(result.messages, result.succeeded)
        assertTrue(
                result.errors,
                result.errors.contains(
                        "Duplicate route path '/duplicate': " +
                                "fixture.InitialRoute and fixture.GeneratedRoute"))
    }

    @Test
    fun `blank and whitespace route paths are compiler errors`() {
        listOf("EmptyRoute" to "", "WhitespaceRoute" to "   ").forEach { (className, path) ->
            val result = compile(
                    compilerSources(className, path),
                    listOf(RouteProcessor()))

            assertFalse(result.messages, result.succeeded)
            assertTrue(
                    result.errors,
                    result.errors.contains(
                            "Route path must not be blank: fixture.$className"))
        }
    }

    private fun compile(
            sources: List<JavaFileObject>,
            processors: List<AbstractProcessor>): CompilationResult {
        val compiler = ToolProvider.getSystemJavaCompiler()
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val temporaryRoot = Files.createTempDirectory("krouter-route-diagnostic")
        val generatedKotlin = Files.createDirectories(temporaryRoot.resolve("kotlin"))
        val generatedJava = Files.createDirectories(temporaryRoot.resolve("java"))
        val generatedClasses = Files.createDirectories(temporaryRoot.resolve("classes"))

        try {
            val succeeded = compiler.getStandardFileManager(diagnostics, null, Charsets.UTF_8)
                    .use { fileManager ->
                        val task = compiler.getTask(
                                null,
                                fileManager,
                                diagnostics,
                                listOf(
                                        "-proc:only",
                                        "-source", "8",
                                        "-target", "8",
                                        "-classpath", System.getProperty("java.class.path"),
                                        "-s", generatedJava.toString(),
                                        "-d", generatedClasses.toString(),
                                        "-AmoduleName=diagnostic-test",
                                        "-Akapt.kotlin.generated=$generatedKotlin"),
                                null,
                                sources)
                        task.setProcessors(processors)
                        task.call()
                    }
            val messages = diagnostics.diagnostics.joinToString("\n") {
                "${it.kind}: ${it.getMessage(null)}"
            }
            val errors = diagnostics.diagnostics
                    .filter { it.kind == Diagnostic.Kind.ERROR }
                    .joinToString("\n") { it.getMessage(null) }
            return CompilationResult(succeeded, messages, errors)
        } finally {
            temporaryRoot.deleteRecursively()
        }
    }

    private fun compilerSources(routeSimpleName: String, routePath: String): List<JavaFileObject> = listOf(
            source("android.app.Activity", "package android.app; public class Activity {}"),
            source("android.app.Service", "package android.app; public class Service {}"),
            source("android.app.Fragment", "package android.app; public class Fragment {}"),
            source(
                    "android.content.ContentProvider",
                    "package android.content; public class ContentProvider {}"),
            source(
                    "androidx.fragment.app.Fragment",
                    "package androidx.fragment.app; public class Fragment {}"),
            source(
                    "fixture.$routeSimpleName",
                    "package fixture; " +
                            "@com.github.richardwrq.krouter.annotation.Route(path=\"$routePath\") " +
                            "public final class $routeSimpleName extends android.app.Activity {}"))

    private data class CompilationResult(
            val succeeded: Boolean,
            val messages: String,
            val errors: String)

    private fun source(className: String, source: String): JavaFileObject =
            object : SimpleJavaFileObject(
                    URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
                    JavaFileObject.Kind.SOURCE) {
                override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = source
            }

    private fun Path.deleteRecursively() {
        Files.walk(this).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    private class DuplicateRouteGenerator : AbstractProcessor() {
        private var generated = false

        override fun getSupportedAnnotationTypes(): MutableSet<String> = mutableSetOf("*")

        override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

        override fun process(
                annotations: MutableSet<out TypeElement>,
                roundEnvironment: RoundEnvironment): Boolean {
            if (generated || roundEnvironment.processingOver()) {
                return false
            }

            generated = true
            processingEnv.filer.createSourceFile("fixture.GeneratedRoute").openWriter().use {
                it.write(
                        "package fixture; " +
                                "@com.github.richardwrq.krouter.annotation.Route(path=\"/duplicate\") " +
                                "public final class GeneratedRoute extends android.app.Activity {}")
            }
            return false
        }
    }
}
