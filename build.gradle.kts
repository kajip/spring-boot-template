plugins {
	java
	pmd
	jacoco
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.diffplug.spotless") version "8.11.0"
	id("info.solidsoft.pitest") version "1.19.0"
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
	implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("com.tngtech.archunit:archunit-junit5:1.5.1") {
		// Its JUnit 1.x dependency otherwise pulls the launcher below the version Spring Boot uses.
		exclude(group = "org.junit.platform")
	}
	testImplementation("io.github.classgraph:classgraph:6.0.2")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	// Test scope only: the document is generated in tests and production does not serve it.
	testImplementation("org.springdoc:springdoc-openapi-starter-webmvc-api:3.2.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
	environment("SPRING_DATASOURCE_URL", "jdbc:postgresql://db:5432/app_test")
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

pitest {
	pitestVersion = "1.30.2"
	junit5PluginVersion = "1.2.3"
	targetClasses = setOf("org.kajip.app.*")
	excludedClasses = setOf("org.kajip.app.*Application")
	timestampedReports = false
	mutationThreshold = 80
	// PIT starts its own JVMs, so the test task's environment does not reach them.
	jvmArgs = listOf("-Dspring.datasource.url=jdbc:postgresql://db:5432/app_test")
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

val classDiagram = "doc/autogen/class-diagram.md"

val generateClassDiagram = tasks.register<JavaExec>("generateClassDiagram") {
	group = "documentation"
	description = "Generates the class diagram from the compiled main classes."
	classpath = sourceSets.test.get().runtimeClasspath
	mainClass = "org.kajip.app.ClassDiagramGenerator"
	args(sourceSets.main.get().java.classesDirectory.get().asFile.path, classDiagram)
}

val openApiDocument = "doc/autogen/openapi.json"

val generateOpenApi = tasks.register<Test>("generateOpenApi") {
	group = "documentation"
	description = "Generates the OpenAPI document from the running application context."
	testClassesDirs = sourceSets.test.get().output.classesDirs
	classpath = sourceSets.test.get().runtimeClasspath
	filter { includeTestsMatching("*OpenApiDocumentTests.committedDocumentIsUpToDate") }
	systemProperty("openapi.write", "true")
	outputs.upToDateWhen { false }
}

tasks.test {
	mustRunAfter(generateClassDiagram, generateOpenApi)
	// Tests compare these files with the code, so editing one alone must rerun them.
	inputs.files(classDiagram, openApiDocument)
}

// Tests apply the Flyway migrations to the database that tbls reads.
val generateSchemaDoc = tasks.register<Exec>("generateSchemaDoc") {
	group = "documentation"
	description = "Generates the ER diagram and table definitions with tbls."
	dependsOn(tasks.test)
	commandLine("tbls", "doc", "--rm-dist")
}

val verifySchemaDoc = tasks.register<Exec>("verifySchemaDoc") {
	group = "verification"
	description = "Fails when the generated schema documents differ from the database."
	dependsOn(tasks.test)
	mustRunAfter(generateSchemaDoc)
	// tbls diff only compares the Markdown documents, so check schema.json separately.
	commandLine(
		"sh", "-c",
		"tbls diff && tbls out -t json -o build/tmp/schema.json " +
			"&& cmp build/tmp/schema.json doc/autogen/schema/schema.json"
	)
}

tasks.register("generateDocs") {
	group = "documentation"
	description = "Regenerates the diagrams, schema documents and OpenAPI document under doc/autogen/."
	dependsOn(generateClassDiagram, generateOpenApi, generateSchemaDoc)
}

tasks.check {
	dependsOn(tasks.jacocoTestCoverageVerification, tasks.pitest, typos, verifySchemaDoc)
}
