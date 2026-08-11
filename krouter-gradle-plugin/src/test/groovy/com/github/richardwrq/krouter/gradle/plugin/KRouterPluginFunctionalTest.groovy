package com.github.richardwrq.krouter.gradle.plugin

import groovy.io.FileType
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import static org.junit.Assume.assumeTrue

class KRouterPluginFunctionalTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder()

    private File testProjectDir

    @Before
    void setUp() {
        testProjectDir = temporaryFolder.root

        String androidSdk = System.getenv('ANDROID_HOME') ?: System.getenv('ANDROID_SDK_ROOT')
        assumeTrue('ANDROID_HOME or ANDROID_SDK_ROOT must point to an Android SDK.',
                androidSdk != null && new File(androidSdk).isDirectory())

        Properties localProperties = new Properties()
        localProperties.setProperty('sdk.dir', androidSdk)
        new File(testProjectDir, 'local.properties').withOutputStream {
            localProperties.store(it, null)
        }

        writeFile('settings.gradle', '''
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }

            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    google()
                    mavenCentral()
                }
            }

            rootProject.name = 'krouter-plugin-fixture'
            include ':app', ':probe-processor'
        ''')

        writeFile('app/build.gradle', '''
            plugins {
                id 'com.android.application' version '8.13.2'
                id 'org.jetbrains.kotlin.android' version '2.3.21'
                id 'org.jetbrains.kotlin.kapt' version '2.3.21'
                id 'com.github.richardwrq.krouter'
            }

            android {
                namespace = 'fixture.app'
                compileSdk = 35

                defaultConfig {
                    applicationId = 'fixture.app'
                    minSdk = 21
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_1_8
                    targetCompatibility = JavaVersion.VERSION_1_8
                }
            }

            kotlin {
                jvmToolchain(17)
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
                }
            }

            dependencies {
                kapt project(':probe-processor')
            }
        ''')

        writeFile('app/src/main/AndroidManifest.xml', '''
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application />
            </manifest>
        ''')

        writeFile('app/src/main/kotlin/fixture/Sample.kt', '''
            package fixture

            @Target(AnnotationTarget.CLASS)
            annotation class Probe

            @Probe
            class Sample
        ''')

        writeFile('probe-processor/build.gradle', '''
            plugins {
                id 'java-library'
            }

            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
        ''')

        writeFile('probe-processor/src/main/java/fixture/ProbeProcessor.java', '''
            package fixture;

            import java.io.IOException;
            import java.io.Writer;
            import java.util.Set;
            import javax.annotation.processing.AbstractProcessor;
            import javax.annotation.processing.RoundEnvironment;
            import javax.annotation.processing.SupportedAnnotationTypes;
            import javax.annotation.processing.SupportedOptions;
            import javax.annotation.processing.SupportedSourceVersion;
            import javax.lang.model.SourceVersion;
            import javax.lang.model.element.TypeElement;
            import javax.tools.FileObject;
            import javax.tools.StandardLocation;

            @SupportedAnnotationTypes("fixture.Probe")
            @SupportedOptions("moduleName")
            @SupportedSourceVersion(SourceVersion.RELEASE_17)
            public final class ProbeProcessor extends AbstractProcessor {
                private boolean written;

                @Override
                public boolean process(
                        Set<? extends TypeElement> annotations,
                        RoundEnvironment roundEnvironment
                ) {
                    if (written || roundEnvironment.processingOver()) {
                        return false;
                    }

                    String moduleName = processingEnv.getOptions().get("moduleName");
                    if (moduleName == null) {
                        throw new IllegalStateException("KRouter did not provide the moduleName option.");
                    }

                    written = true;
                    try {
                        FileObject output = processingEnv.getFiler().createResource(
                                StandardLocation.CLASS_OUTPUT,
                                "",
                                "krouter-module-name.txt"
                        );
                        try (Writer writer = output.openWriter()) {
                            writer.write(moduleName);
                        }
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                    return false;
                }
            }
        ''')

        writeFile(
                'probe-processor/src/main/resources/META-INF/services/javax.annotation.processing.Processor',
                'fixture.ProbeProcessor\n'
        )
    }

    @Test
    void 'wires kapt and generated assets while reusing the configuration cache'() {
        BuildResult firstBuild = runBuild()

        assertNotNull(firstBuild.task(':app:kaptDebugKotlin'))
        assertTrue(
                firstBuild.task(':app:generateDebugKRouterMarker').outcome in
                        [TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE]
        )

        File marker = new File(
                testProjectDir,
                'app/build/generated/krouter/assets/debug/KRouter_app'
        )
        assertTrue('The generated KRouter marker asset is missing.', marker.isFile())
        assertFalse(
                'The plugin must not write generated markers into source directories.',
                new File(testProjectDir, 'app/src/main/assets/KRouter_app').exists()
        )

        List<File> optionFiles = []
        new File(testProjectDir, 'app/build').eachFileRecurse(FileType.FILES) { file ->
            if (file.name == 'krouter-module-name.txt') {
                optionFiles.add(file)
            }
        }
        assertFalse('The probe annotation processor did not produce its option file.', optionFiles.isEmpty())
        assertEquals(['app'] as Set, optionFiles.collect { it.getText('UTF-8') }.toSet())

        BuildResult secondBuild = runBuild()

        assertTrue(secondBuild.output.contains('Reusing configuration cache.'))
        assertTrue(
                secondBuild.task(':app:generateDebugKRouterMarker').outcome in
                        [TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE]
        )
    }

    private BuildResult runBuild() {
        List<String> arguments = [
                ':app:kaptDebugKotlin',
                ':app:mergeDebugAssets',
                '--configuration-cache',
                '--configuration-cache-problems=fail',
                '--stacktrace'
        ]

        String initScript = System.getenv('KROUTER_TEST_INIT_SCRIPT')
        if (initScript != null && !initScript.isBlank()) {
            arguments.addAll(['--init-script', initScript])
        }

        return GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .withArguments(arguments)
                .build()
    }

    private void writeFile(String path, String content) {
        File target = new File(testProjectDir, path)
        assertTrue(target.parentFile.mkdirs() || target.parentFile.isDirectory())
        target.setText(content.stripIndent().trim() + '\n', 'UTF-8')
    }
}
