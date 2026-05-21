package com.zufar.icedlatte.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.mapstruct.Mapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
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
                            "..email..", "..filestorage..",
                            "..icedlatte.security..", "..icedlatte.user.."
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
                    return pkg.startsWith("com.zufar.icedlatte.common")
                            || pkg.startsWith("com.zufar.icedlatte.ratelimit")
                            || pkg.startsWith("com.zufar.icedlatte.openapi");
                }
            };

    /**
     * API packages (named interfaces) should not depend on repositories, entities, or converters.
     * This ensures module boundaries expose only clean contracts (interfaces, records, DTOs).
     * Currently enforced for api packages that have been fully refactored.
     */
    @ArchTest
    static final ArchRule order_api_should_not_depend_on_order_implementation =
            noClasses()
                    .that().resideInAPackage("..order.api..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..order.repository..", "..order.entity..", "..order.converter.."
                    );

    @ArchTest
    static final ArchRule product_api_should_not_depend_on_product_implementation =
            noClasses()
                    .that().resideInAPackage("..product.api..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..product.repository..",
                            "..product.entity..",
                            "..product.converter..",
                            "..product.specification..",
                            "..product.service.."
                    );

    @ArchTest
    static final ArchRule cart_api_should_not_depend_on_cart_implementation =
            noClasses()
                    .that().resideInAPackage("..cart.api..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..cart.repository..",
                            "..cart.entity..",
                            "..cart.converter..",
                            "..cart.service.."
                    );

    @ArchTest
    static final ArchRule review_api_should_not_depend_on_review_implementation =
            noClasses()
                    .that().resideInAPackage("..review.api..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..review.repository..",
                            "..review.entity..",
                            "..review.converter..",
                            "..review.service..",
                            "..review.ai.."
                    );

    @ArchTest
    static final ArchRule user_api_should_not_depend_on_user_implementation =
            noClasses()
                    .that().resideInAPackage("..user.api..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..user.repository..",
                            "..user.entity..",
                            "..user.converter..",
                            "..user.service.."
                    );

    @ArchTest
    static final ArchRule security_api_should_not_depend_on_security_implementation =
            noClasses()
                    .that().resideInAPackage("..security.api..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..security.config..",
                            "..security.repository..",
                            "..security.entity..",
                            "..security.service..",
                            "..security.exception.."
                    );

    @ArchTest
    static final ArchRule ratelimit_api_should_not_depend_on_ratelimit_implementation =
            noClasses()
                    .that().resideInAPackage("..ratelimit.api..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..ratelimit.configuration..",
                            "..ratelimit.dto..",
                            "..ratelimit.filter..",
                            "..ratelimit.util.."
                    );

    @ArchTest
    static final ArchRule api_packages_should_not_contain_spring_implementation_beans =
            noClasses()
                    .that().resideInAPackage("..api..")
                    .should().beAnnotatedWith(Service.class)
                    .orShould().beAnnotatedWith(Component.class)
                    .orShould().beAnnotatedWith(Repository.class)
                    .orShould().beAnnotatedWith(Configuration.class)
                    .orShould().beAnnotatedWith(Controller.class)
                    .orShould().beAnnotatedWith(RestController.class)
                    .orShould().beAnnotatedWith(Mapper.class);

    @ArchTest
    static final ArchRule public_module_api_packages_should_not_depend_on_generated_openapi_dtos =
            noClasses()
                    .that().resideInAnyPackage(
                            "..cart.api..",
                            "..cart.api.dto..",
                            "..filestorage.api..",
                            "..filestorage.api.dto..",
                            "..order.api..",
                            "..order.api.dto..",
                            "..product.api..",
                            "..product.api.dto..",
                            "..review.api..",
                            "..security.api..",
                            "..security.api.dto..",
                            "..user.api..",
                            "..user.api.dto.."
                    )
                    .and().resideOutsideOfPackage("..openapi..")
                    .should().dependOnClassesThat().resideInAnyPackage("..openapi.dto..");

    @ArchTest
    static final ArchRule security_module_should_not_depend_on_user_implementation =
            noClasses()
                    .that().resideInAPackage("..security..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..user.repository..",
                            "..user.entity..",
                            "..user.converter..",
                            "..user.service.."
                    );

    @ArchTest
    static final ArchRule non_user_modules_should_not_depend_on_user_implementation =
            noClasses()
                    .that().resideOutsideOfPackage("..user..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..user.repository..",
                            "..user.entity..",
                            "..user.converter..",
                            "..user.service.."
                    );

    @ArchTest
    static final ArchRule non_security_modules_should_not_depend_on_security_implementation =
            noClasses()
                    .that().resideOutsideOfPackage("..security..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..security.config..",
                            "..security.repository..",
                            "..security.entity..",
                            "..security.service.."
                    );

    /**
     * Non-order modules must not depend on order implementation services.
     * Spring Modulith already enforces this, but an explicit ArchUnit rule gives clearer failure messages.
     */
    @ArchTest
    static final ArchRule non_order_modules_should_not_depend_on_order_services =
            noClasses()
                    .that().resideOutsideOfPackage("..order..")
                    .should().dependOnClassesThat().resideInAPackage("..order.service..");

    @ArchTest
    static final ArchRule order_services_should_not_depend_on_user_repositories =
            noClasses()
                    .that().resideInAPackage("..order.service..")
                    .should().dependOnClassesThat().resideInAnyPackage("..user.repository..");

    @ArchTest
    static final ArchRule order_services_should_not_depend_on_delivery_address_entity =
            noClasses()
                    .that().resideInAPackage("..order.service..")
                    .should().dependOnClassesThat().haveSimpleName("DeliveryAddressEntity");

    @ArchTest
    static final ArchRule order_module_should_not_depend_on_user_address_entity =
            noClasses()
                    .that().resideInAPackage("..order..")
                    .should().dependOnClassesThat().resideInAPackage("..user.entity..");

    @ArchTest
    static final ArchRule review_module_should_not_depend_on_user_entities =
            noClasses()
                    .that().resideInAPackage("..review..")
                    .should().dependOnClassesThat().resideInAPackage("..user.entity..");

    @ArchTest
    static final ArchRule non_product_modules_should_not_depend_on_product_entities =
            noClasses()
                    .that().resideOutsideOfPackage("..product..")
                    .should().dependOnClassesThat().resideInAnyPackage("..product.entity..");

    @ArchTest
    static final ArchRule non_product_modules_should_not_depend_on_product_converters =
            noClasses()
                    .that().resideOutsideOfPackage("..product..")
                    .should().dependOnClassesThat().resideInAnyPackage("..product.converter..");

    @ArchTest
    static final ArchRule non_product_modules_should_not_depend_on_product_services =
            noClasses()
                    .that().resideOutsideOfPackage("..product..")
                    .should().dependOnClassesThat().resideInAnyPackage("..product.service..");

    @ArchTest
    static final ArchRule non_cart_modules_should_not_depend_on_cart_services =
            noClasses()
                    .that().resideOutsideOfPackage("..cart..")
                    .should().dependOnClassesThat().resideInAnyPackage("..cart.service..");

    @ArchTest
    static final ArchRule non_payment_modules_should_not_depend_on_payment_services =
            noClasses()
                    .that().resideOutsideOfPackage("..payment..")
                    .should().dependOnClassesThat().resideInAnyPackage("..payment.service..");

    @ArchTest
    static final ArchRule non_ratelimit_modules_should_not_depend_on_ratelimit_implementation =
            noClasses()
                    .that().resideOutsideOfPackage("..ratelimit..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..ratelimit.configuration..",
                            "..ratelimit.dto..",
                            "..ratelimit.filter..",
                            "..ratelimit.util.."
                    );

    /**
     * Checks that core business feature modules do not form dependency cycles.
     * Infrastructure modules (common, ratelimit, openapi) are excluded from this ArchUnit rule.
     * Spring Modulith still verifies all closed module boundaries directly.
     */
    @ArchTest
    static final ArchRule feature_packages_should_be_free_of_cycles =
            slices()
                    .matching("com.zufar.icedlatte.(*)..")
                    .should().beFreeOfCycles()
                    .ignoreDependency(INFRASTRUCTURE_MODULE, DescribedPredicate.alwaysTrue())
                    .ignoreDependency(DescribedPredicate.alwaysTrue(), INFRASTRUCTURE_MODULE);
}
