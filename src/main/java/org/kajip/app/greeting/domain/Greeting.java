package org.kajip.app.greeting.domain;

import org.springframework.data.annotation.Id;

public record Greeting(@Id Long id, String message) {}
