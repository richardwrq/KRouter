package com.github.richardwrq.krouter.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.Locale

/**
 * @author WuRuiQiang <a href="mailto:263454190@qq.com">Contact me.</a>
 * @version v1.0
 * @since 18/2/28 下午3:37
 */
class KRouterPlugin implements Plugin<Project> {
    private static final String MODULE_NAME_OPTION = 'moduleName'

    @Override
    void apply(Project project) {
        project.pluginManager.withPlugin('org.jetbrains.kotlin.kapt') {
            project.extensions.configure('kapt') { kapt ->
                kapt.arguments { arguments ->
                    arguments.arg(MODULE_NAME_OPTION, project.name)
                }
                kapt.includeCompileClasspath = false
            }
        }

        project.pluginManager.withPlugin('com.android.application') {
            configureAndroid(project)
        }
        project.pluginManager.withPlugin('com.android.library') {
            configureAndroid(project)
        }
    }

    private static void configureAndroid(Project project) {
        def androidComponents = project.extensions.getByName('androidComponents')

        androidComponents.onVariants(androidComponents.selector().all()) { variant ->
            String variantName = variant.name
            String variantSuffix = variantName.substring(0, 1).toUpperCase(Locale.ROOT) +
                    variantName.substring(1)

            def markerTask = project.tasks.register(
                    "generate${variantSuffix}KRouterMarker",
                    GenerateKRouterMarkerTask
            ) {
                group = 'krouter'
                description = "Generates the KRouter marker asset for the ${variantName} variant."
                moduleName.set(project.name)
                outputDirectory.set(
                        project.layout.buildDirectory.dir("generated/krouter/assets/${variantName}")
                )
            }

            variant.sources.assets?.addGeneratedSourceDirectory(markerTask) {
                it.outputDirectory
            }
        }
    }
}
