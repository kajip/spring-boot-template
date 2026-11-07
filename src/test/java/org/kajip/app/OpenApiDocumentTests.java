package org.kajip.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "springdoc.writer-with-default-pretty-printer=true",
      "springdoc.writer-with-order-by-keys=true"
    })
@AutoConfigureMockMvc
class OpenApiDocumentTests {

  private static final Path DOCUMENT = Path.of("doc/autogen/openapi.json");

  @Autowired private MockMvc mockMvc;

  private String currentDocument() throws Exception {
    return mockMvc
            .perform(get("/v3/api-docs"))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8)
        + "\n";
  }

  @Test
  void documentsTheEndpoints() throws Exception {
    assertThat(currentDocument()).contains("\"/greetings\"").contains("\"/\"");
  }

  @Test
  void committedDocumentIsUpToDate() throws Exception {
    String current = currentDocument();
    if (Boolean.getBoolean("openapi.write")) {
      writeDocument(current);
    }
    assertThat(Files.readString(DOCUMENT))
        .as("doc/autogen/openapi.json is stale; run ./gradlew generateDocs")
        .isEqualTo(current);
  }

  private static void writeDocument(String document) throws IOException {
    Files.createDirectories(DOCUMENT.getParent());
    Files.writeString(DOCUMENT, document);
  }
}
