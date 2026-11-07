package org.kajip.app.greeting.web;

import java.util.List;
import org.kajip.app.greeting.domain.Greeting;
import org.kajip.app.greeting.persistence.GreetingRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class GreetingController {

  private final GreetingRepository repository;

  GreetingController(GreetingRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/greetings")
  List<Greeting> greetings() {
    return repository.findAll();
  }
}
