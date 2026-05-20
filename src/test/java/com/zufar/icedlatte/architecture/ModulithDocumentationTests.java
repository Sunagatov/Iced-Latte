package com.zufar.icedlatte.architecture;

import com.zufar.icedlatte.IcedLatteApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModulithDocumentationTests {

    private final ApplicationModules modules = ApplicationModules.of(
            IcedLatteApplication.class,
            ApplicationModules.Filters.withoutModule("openapi"));

    @Test
    void writeModuleDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
