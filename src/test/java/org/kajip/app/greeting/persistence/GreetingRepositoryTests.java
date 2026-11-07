package org.kajip.app.greeting.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.kajip.app.greeting.domain.Greeting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GreetingRepositoryTests {

  @Autowired private GreetingRepository repository;

  @Test
  void migrationSeedsInitialGreeting() {
    assertThat(repository.findAll()).extracting(Greeting::message).contains("Hello, World!");
  }

  @Test
  void savedGreetingCanBeReadBack() {
    Greeting saved = repository.save(new Greeting(null, "Good morning"));

    assertThat(repository.findById(saved.id())).contains(saved);
  }
}
