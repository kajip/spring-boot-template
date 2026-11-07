package org.kajip.app;

import org.springframework.data.repository.ListCrudRepository;

interface GreetingRepository extends ListCrudRepository<Greeting, Long> {}
