package org.kajip.app;

import org.springframework.data.annotation.Id;

record Greeting(@Id Long id, String message) {}
