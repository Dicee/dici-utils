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

dependencies {
    implementation("org.scala-lang:scala-library:2.12.20")
    
    implementation("com.google.guava:guava:33.4.8-jre")
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    // JavaFX modules
    listOf("base", "controls", "graphics", "web", "swing").forEach {
        implementation("org.openjfx:javafx-$it:$javafxVersion:$javafxClassifier")
    }
    implementation("org.controlsfx:controlsfx:8.0.6_20")

    implementation("org.apache.logging.log4j:log4j-api:2.13.3")
    implementation("org.apache.logging.log4j:log4j-core:2.13.3")

    // we also need it to compile test utils. Ideally we should instead split the repos in two (or even more, to have more themes) or use Java modules (could be the better idea),
    // but my main goal is to save useful code somewhere, not making the package easy to consume
    implementation("org.hamcrest:hamcrest:2.2") 
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.0.0")
    testImplementation("org.scalactic:scalactic_2.12:3.1.2")
    testImplementation("org.scalatest:scalatest_2.12:3.1.2")
}

tasks.test {
    useJUnitPlatform()
}

