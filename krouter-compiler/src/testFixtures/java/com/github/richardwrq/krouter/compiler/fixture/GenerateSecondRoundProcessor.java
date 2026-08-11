package com.github.richardwrq.krouter.compiler.fixture;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes(
        "com.github.richardwrq.krouter.compiler.integration.GenerateSecondRoundFixture")
public final class GenerateSecondRoundProcessor extends AbstractProcessor {

    private boolean generated;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnvironment) {
        if (generated || roundEnvironment.processingOver()) {
            return false;
        }

        generated = true;
        try {
            writeSource(
                    "com.github.richardwrq.krouter.compiler.integration.generated.GeneratedRoundProvider",
                    "package com.github.richardwrq.krouter.compiler.integration.generated;\n"
                            + "@com.github.richardwrq.krouter.annotation.Provider(\"provider/generated-round\")\n"
                            + "public final class GeneratedRoundProvider implements "
                            + "com.github.richardwrq.krouter.api.interfaces.IProvider {\n"
                            + "  @Override public void init(android.content.Context context) {}\n"
                            + "}\n");
            writeSource(
                    "com.github.richardwrq.krouter.compiler.integration.generated.GeneratedRoundRoute",
                    "package com.github.richardwrq.krouter.compiler.integration.generated;\n"
                            + "@com.github.richardwrq.krouter.annotation.Route(path=\"/generated/round\")\n"
                            + "public final class GeneratedRoundRoute extends android.app.Activity {}\n");
            writeSource(
                    "com.github.richardwrq.krouter.compiler.integration.generated.GeneratedRoundInterceptor",
                    "package com.github.richardwrq.krouter.compiler.integration.generated;\n"
                            + "@com.github.richardwrq.krouter.annotation.Interceptor("
                            + "priority=200, name=\"generated-round\")\n"
                            + "public final class GeneratedRoundInterceptor implements "
                            + "com.github.richardwrq.krouter.api.interfaces.IRouteInterceptor {\n"
                            + "  @Override public boolean intercept(android.content.Context context, "
                            + "String path, android.os.Bundle extras) { return false; }\n"
                            + "}\n");
            writeSource(
                    "com.github.richardwrq.krouter.compiler.integration.generated.GeneratedRoundInjectionTarget",
                    "package com.github.richardwrq.krouter.compiler.integration.generated;\n"
                            + "public final class GeneratedRoundInjectionTarget {\n"
                            + "  @com.github.richardwrq.krouter.annotation.Inject "
                            + "public String generatedValue = \"generated-default\";\n"
                            + "}\n");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate the second-round fixtures", exception);
        }
        return false;
    }

    private void writeSource(String className, String source) throws IOException {
        JavaFileObject file = processingEnv.getFiler().createSourceFile(className);
        try (Writer writer = file.openWriter()) {
            writer.write(source);
        }
    }
}
