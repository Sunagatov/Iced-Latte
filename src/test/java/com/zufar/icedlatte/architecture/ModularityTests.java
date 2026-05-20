package com.zufar.icedlatte.architecture;

import com.zufar.icedlatte.IcedLatteApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(IcedLatteApplication.class);

    @Test
    void printsApplicationModules() {
        modules.forEach(System.out::println);
    }

    @Test
    void verifiesApplicationModuleStructure() {
        modules.verify();
    }

    /**
     * Guards against accidental exposure of new subpackages.
     * If you need to expose a new @NamedInterface, update this set and explain why in the PR.
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
                "common :: audit", "common :: config", "common :: correlation",
                "common :: exception", "common :: exception-handler", "common :: http",
                "common :: monitoring", "common :: pagination", "common :: temporarycache",
                "common :: util", "common :: validation-pagination",
                "email :: api-token", "email :: exception", "email :: sender",
                "filestorage :: aws", "filestorage :: dto", "filestorage :: exception",
                "order :: api", "order :: exception",
                "product :: api", "product :: api-filestorage", "product :: converter",
                "product :: entity", "product :: exception",
                "review :: api"
        );

        assertThat(allNamedInterfaces).isEqualTo(expected);
    }
}
