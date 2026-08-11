package com.github.richardwrq.krouter.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@CacheableTask
abstract class GenerateKRouterMarkerTask extends DefaultTask {
    @Input
    abstract Property<String> getModuleName()

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory()

    @TaskAction
    void generate() {
        String normalizedModuleName = moduleName.get().replaceAll('[^0-9A-Za-z_]+', '')
        if (normalizedModuleName.isEmpty()) {
            throw new GradleException('KRouter module name is empty after normalization.')
        }

        Path directory = outputDirectory.get().asFile.toPath()
        Files.createDirectories(directory)
        Files.write(
                directory.resolve("KRouter_${normalizedModuleName}"),
                new byte[0],
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        )
    }
}
