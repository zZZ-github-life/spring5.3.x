plugins {
    id("java")
}

group = "org.springframework"
version = "5.3.28-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
	api(project(":spring-context"))
	api(project(":spring-beans"))
	api(project(":spring-core"))
	api(project(":spring-aop"))
	api(project(":spring-aspects"))
	api(project(":spring-jdbc"))

	optional("javax.annotation:javax.annotation-api:1.3.2")
	testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}