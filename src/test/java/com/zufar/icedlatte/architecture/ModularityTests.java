package com.zufar.icedlatte.architecture;

import com.zufar.icedlatte.IcedLatteApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

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
}
