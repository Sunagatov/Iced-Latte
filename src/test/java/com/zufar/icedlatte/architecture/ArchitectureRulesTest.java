package com.zufar.icedlatte.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "com.zufar.icedlatte",
        importOptions = ImportOption.DoNotIncludeTests.class
)
@SuppressWarnings("unused") // fields are accessed by ArchUnit engine via reflection
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule rest_controllers_should_not_access_repositories =
            noClasses()
                    .that().areAnnotatedWith(RestController.class)
                    .should().accessClassesThat().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule common_should_not_depend_on_feature_modules =
            noClasses()
                    .that().resideInAPackage("..common..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..product..", "..cart..", "..order..",
                            "..payment..", "..review..", "..favorite..",
                            "..email..", "..filestorage.."
                    );

    @ArchTest
    static final ArchRule no_module_should_depend_on_astartup =
            noClasses()
                    .that().resideOutsideOfPackage("..astartup..")
                    .should().dependOnClassesThat().resideInAPackage("..astartup..");

    private static final DescribedPredicate<JavaClass> INFRASTRUCTURE_MODULE =
            new DescribedPredicate<>("belongs to infrastructure module") {
                @Override
                public boolean test(JavaClass javaClass) {
                    String pkg = javaClass.getPackageName();
                    return pkg.startsWith("com.zufar.icedlatte.security")
                            || pkg.startsWith("com.zufar.icedlatte.user")
                            || pkg.startsWith("com.zufar.icedlatte.common")
                            || pkg.startsWith("com.zufar.icedlatte.openapi");
                }
            };

    /**
     * API packages (named interfaces) should not depend on repositories, entities, or converters.
     * This ensures module boundaries expose only clean contracts (interfaces, records, DTOs).
     * Currently enforced only for order.api which has been fully refactored.
     * TODO: extend to cart.api, product.api, review.api after their api packages are cleaned.
     */
    @ArchTest
    static final ArchRule api_packages_should_not_depend_on_internals =
            noClasses()
                    .that().resideInAPackage("..order.api..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..order.repository..", "..order.entity..", "..order.converter.."
                    );

    /**
     * Checks that core business feature modules do not form dependency cycles.
     * Infrastructure modules (security, user, common, openapi) are excluded
     * because security↔user has inherent bidirectional coupling.
     */
    @ArchTest
    static final ArchRule feature_packages_should_be_free_of_cycles =
            slices()
                    .matching("com.zufar.icedlatte.(*)..")
                    .should().beFreeOfCycles()
                    .ignoreDependency(INFRASTRUCTURE_MODULE, DescribedPredicate.alwaysTrue())
                    .ignoreDependency(DescribedPredicate.alwaysTrue(), INFRASTRUCTURE_MODULE);
}
