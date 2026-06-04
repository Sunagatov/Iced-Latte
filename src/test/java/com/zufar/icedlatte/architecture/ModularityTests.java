package com.zufar.icedlatte.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import com.zufar.icedlatte.IcedLatteApplication;

class ModularityTests {

    private final ApplicationModules modules =
            ApplicationModules.of(IcedLatteApplication.class, ApplicationModules.Filters.withoutModule("openapi"));

    @Test
    void verifiesApplicationModuleStructure() {
        modules.verify();
    }

    /**
     * Guards against accidental exposure of new subpackages. If you need to expose a new @NamedInterface, update this
     * set and explain why in the PR.
     */
    @Test
    void exposedNamedInterfacesAreControlled() {
        Set<String> allNamedInterfaces = modules.stream()
                .flatMap(m -> m.getNamedInterfaces().stream()
                        .filter(ni -> !ni.isUnnamed())
                        .map(ni -> m.getIdentifier() + " :: " + ni.getName()))
                .collect(Collectors.toSet());

        Set<String> expected = Set.of(
                "cart :: api",
                "common :: audit",
                "common :: config",
                "common :: correlation",
                "common :: exception",
                "common :: exception-handler",
                "common :: http",
                "common :: monitoring",
                "common :: pagination",
                "common :: turnstile",
                "common :: util",
                "common :: validation-pagination",
                "filestorage :: api",
                "filestorage :: aws",
                "filestorage :: exception",
                "order :: api",
                "order :: converter",
                "order :: exception",
                "product :: api",
                "product :: converter",
                "product :: exception",
                "ratelimit :: api",
                "review :: api",
                "security :: api",
                "user :: api",
                "user :: exception");

        assertThat(allNamedInterfaces).isEqualTo(expected);
    }
}
