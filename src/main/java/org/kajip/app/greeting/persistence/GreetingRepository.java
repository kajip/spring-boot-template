package org.kajip.app.greeting.persistence;

import org.kajip.app.greeting.domain.Greeting;
import org.springframework.data.repository.ListCrudRepository;

public interface GreetingRepository extends ListCrudRepository<Greeting, Long> {}
