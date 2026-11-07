package org.kajip.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringBootTemplateApplicationTests {

  // Starting the context is the assertion: it fails when a bean definition is missing.
  @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
  @Test
  void contextLoads() {}
}
