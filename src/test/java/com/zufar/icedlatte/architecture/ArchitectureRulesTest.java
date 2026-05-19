package com.zufar.icedlatte.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "com.zufar.icedlatte",
        importOptions = ImportOption.DoNotIncludeTests.class
)
@SuppressWarnings("unused") // fields are accessed by ArchUnit engine via reflection
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule endpoints_should_not_access_repositories =
            noClasses()
                    .that().haveSimpleNameEndingWith("Endpoint")
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

    private static final DescribedPredicate<JavaClass> INFRASTRUCTURE_MODULE =
            new DescribedPredicate<>("belongs to infrastructure module") {
                @Override
                public boolean test(JavaClass javaClass) {
                    String pkg = javaClass.getPackageName();
                    return pkg.startsWith("com.zufar.icedlatte.security")
                            || pkg.startsWith("com.zufar.icedlatte.common")
                            || pkg.startsWith("com.zufar.icedlatte.openapi")
                            || pkg.startsWith("com.zufar.icedlatte.astartup");
                }
            };

    /**
     * Checks that core business feature modules do not form dependency cycles.
     * Infrastructure modules (security, common, openapi, astartup) are excluded
     * because they have known coupling that will be addressed in later phases.
     */
    @ArchTest
    static final ArchRule feature_packages_should_be_free_of_cycles =
            slices()
                    .matching("com.zufar.icedlatte.(*)..")
                    .should().beFreeOfCycles()
                    .ignoreDependency(INFRASTRUCTURE_MODULE, DescribedPredicate.alwaysTrue())
                    .ignoreDependency(DescribedPredicate.alwaysTrue(), INFRASTRUCTURE_MODULE);
}
