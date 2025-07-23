plugins {
    scala
    java
}

group = "com.dici"
version = "0.1"

java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

val javafxVersion = "15-ea+5"
val javafxClassifier = "linux"

val mockitoAgent = configurations.create("mockitoAgent")
val mockitoLib = "org.mockito:mockito-core:5.18.0"

dependencies {
    implementation("org.scala-lang:scala-library:2.12.20")
    
    implementation("com.fasterxml.jackson.core:jackson-core:2.19.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.19.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava:2.19.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")

    implementation("com.google.guava:guava:33.4.8-jre")
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("io.github.resilience4j:resilience4j-all:2.3.0")
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    // JavaFX modules
    listOf("base", "controls", "graphics", "web", "swing").forEach {
        implementation("org.openjfx:javafx-$it:$javafxVersion:$javafxClassifier")
    }
    implementation("org.controlsfx:controlsfx:8.0.6_20")

    implementation("org.apache.logging.log4j:log4j-api:2.13.3")
    implementation("org.apache.logging.log4j:log4j-core:2.13.3")

    // we also need Hamcrest and AssertJ to compile test utils. Ideally we should instead split the repos in two (or even more, to have more themes) or use Java modules (could be the better idea),
    // but my main goal is to save useful code somewhere, not making the package easy to consume
    implementation("org.hamcrest:hamcrest:2.2")
    implementation("org.assertj:assertj-core:3.27.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.0.0")
    testImplementation("org.scalactic:scalactic_2.12:3.1.2")
    testImplementation("org.scalatest:scalatest_2.12:3.1.2")

    testImplementation(mockitoLib)
    mockitoAgent(mockitoLib) { isTransitive = false }
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")
}

tasks.test {
    systemProperty("log4j.configurationFile", "$projectDir/src/test/resources/log4j2-test.xml")
    jvmArgs!!.add("-javaagent:${mockitoAgent.asPath}")

    useJUnitPlatform()
}

