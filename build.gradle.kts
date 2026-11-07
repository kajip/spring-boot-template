plugins {
	java
	pmd
	jacoco
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.diffplug.spotless") version "8.11.0"
}

group = "org.kajip"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
}

spotless {
	java {
		googleJavaFormat("2.0.0")
	}
}

pmd {
	toolVersion = "7.29.0"
	isConsoleOutput = true
	ruleSets = emptyList()
	ruleSetFiles = files("config/pmd/ruleset.xml")
}

jacoco {
	toolVersion = "0.8.16"
}

// The application entry point only delegates to Spring Boot, so it is not worth covering.
tasks.withType<JacocoReportBase>().configureEach {
	classDirectories.setFrom(
		sourceSets.main.get().output.asFileTree.matching { exclude("**/*Application.class") }
	)
}

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.test)
	violationRules {
		rule {
			limit {
				counter = "LINE"
				minimum = "0.80".toBigDecimal()
			}
		}
	}
}

tasks.test {
	finalizedBy(tasks.jacocoTestReport)
}

val typos = tasks.register<Exec>("typos") {
	group = "verification"
	description = "Checks the project for typos. The typos binary is provided by the dev container."
	commandLine("typos")
}

tasks.check {
	dependsOn(tasks.jacocoTestCoverageVerification, typos)
}
