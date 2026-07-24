package com.ledgerly;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import java.nio.file.Paths;

/**
 * Generates Spring Modulith documentation artifacts (PlantUML component diagrams
 * and module canvases) into the committed {@code docs/modulith/} folder.
 *
 * <p>This test is intentionally <strong>disabled by default</strong>. Generating
 * the artifacts wipes the output folder, so running it on every {@code mvn test}
 * would clobber committed diagrams and pollute developer working trees. Enable it
 * explicitly when refreshing the documentation:
 *
 * <pre>
 * docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 \
 *   mvn -B -ntp test -Dtest=ModulithDocumentationTests -Dgen-modulith-docs=true
 * </pre>
 *
 * <p>The CI workflow runs this and commits any changes to {@code docs/modulith/},
 * keeping the diagrams in sync with the source. {@link ModularityTests#verifyModularStructure()}
 * remains the gate that fails the build on architecture violations.
 */
@EnabledIfSystemProperty(named = "gen-modulith-docs", matches = "true")
class ModulithDocumentationTests {

    private final ApplicationModules modules = ApplicationModules.of(LedgerlyApplication.class);

    @Test
    void writeDocumentationSnippets() {
        String outputFolder = Paths.get("docs", "modulith").toAbsolutePath().toString();

        new Documenter(modules, Documenter.Options.defaults().withOutputFolder(outputFolder))
            .writeModulesAsPlantUml()
            .writeIndividualModulesAsPlantUml()
            .writeModuleCanvases(Documenter.CanvasOptions.defaults())
            .writeAggregatingDocument();
    }
}
