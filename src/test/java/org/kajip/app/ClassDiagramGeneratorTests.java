package org.kajip.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.kajip.app.greeting.domain.Greeting;

class ClassDiagramGeneratorTests {

  interface FixtureContract {}

  static class FixtureBase {}

  static final class FixtureChild extends FixtureBase implements FixtureContract {}

  private static Path mainClassesDir() throws URISyntaxException {
    return Path.of(Greeting.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  @Test
  void rendersMainClassesWithMembers() throws URISyntaxException {
    assertThat(ClassDiagramGenerator.render(mainClassesDir()))
        .contains("```mermaid")
        .contains("class Greeting {")
        .contains("<<record>>")
        .contains("class GreetingRepository {")
        .contains("<<interface>>")
        .contains("~greetings() List~Greeting~");
  }

  @Test
  void rendersFieldReferencesBetweenMainClasses() throws URISyntaxException {
    assertThat(ClassDiagramGenerator.render(mainClassesDir()))
        .contains("GreetingController --> GreetingRepository : repository");
  }

  @Test
  void omitsRecordMethodsGeneratedByTheCompiler() throws URISyntaxException {
    assertThat(ClassDiagramGenerator.render(mainClassesDir()))
        .doesNotContain("hashCode")
        .doesNotContain("equals(");
  }

  @Test
  void rendersInheritanceAndImplementation() throws URISyntaxException {
    Path testClassesDir =
        Path.of(
            ClassDiagramGeneratorTests.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());

    assertThat(ClassDiagramGenerator.render(testClassesDir))
        .contains("FixtureBase <|-- FixtureChild")
        .contains("FixtureContract <|.. FixtureChild");
  }

  @Test
  void excludesTestClasses() throws URISyntaxException {
    assertThat(ClassDiagramGenerator.render(mainClassesDir()))
        .doesNotContain("ClassDiagramGenerator");
  }

  @Test
  void committedDiagramIsUpToDate() throws IOException, URISyntaxException {
    assertThat(Files.readString(Path.of("doc/autogen/class-diagram.md")))
        .as("doc/autogen/class-diagram.md is stale; run ./gradlew generateDocs")
        .isEqualTo(ClassDiagramGenerator.render(mainClassesDir()));
  }
}
