package org.kajip.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

// ArchUnit runs the @ArchTest fields as tests, which PMD does not recognize.
@SuppressWarnings("PMD.TestClassWithoutTestCases")
@AnalyzeClasses(packages = "org.kajip.app", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

  // Anchored to this project: "..web.." alone would also match org.springframework.web.
  private static final String WEB = "org.kajip.app..web..";
  private static final String DOMAIN = "org.kajip.app..domain..";
  private static final String PERSISTENCE = "org.kajip.app..persistence..";

  @ArchTest
  static final ArchRule FEATURE_CLASSES_BELONG_TO_A_LAYER =
      classes()
          .that()
          .resideOutsideOfPackage("org.kajip.app")
          .should()
          .resideInAnyPackage(WEB, DOMAIN, PERSISTENCE);

  @ArchTest
  static final ArchRule ONLY_THE_ENTRY_POINT_IS_IN_THE_ROOT_PACKAGE =
      classes()
          .that()
          .resideInAPackage("org.kajip.app")
          .should()
          .haveSimpleNameEndingWith("Application");

  @ArchTest
  static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_OTHER_LAYERS =
      noClasses()
          .that()
          .resideInAPackage(DOMAIN)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(WEB, PERSISTENCE);

  @ArchTest
  static final ArchRule PERSISTENCE_DOES_NOT_DEPEND_ON_WEB =
      noClasses()
          .that()
          .resideInAPackage(PERSISTENCE)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(WEB);

  @ArchTest
  static final ArchRule CONTROLLERS_RESIDE_IN_WEB =
      classes().that().areAnnotatedWith(RestController.class).should().resideInAPackage(WEB);

  @ArchTest
  static final ArchRule REPOSITORIES_RESIDE_IN_PERSISTENCE =
      classes().that().areAssignableTo(Repository.class).should().resideInAPackage(PERSISTENCE);

  @ArchTest
  static final ArchRule FEATURES_ARE_FREE_OF_CYCLES =
      slices().matching("org.kajip.app.(*)..").should().beFreeOfCycles();

  @ArchTest
  static final ArchRule NO_FIELD_INJECTION =
      GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

  @ArchTest
  static final ArchRule NO_STANDARD_STREAMS =
      GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
}
