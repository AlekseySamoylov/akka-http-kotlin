plugins {
    java
    kotlin("jvm") version "1.3.72"
}

group = "com.alekseysamoylov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val akkaVersion = "2.6.6"
val akkaHttpVersion = "10.1.12"
val jupiterVersion = "5.5.2"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.typesafe.akka:akka-actor_2.13:$akkaVersion")
    implementation("com.typesafe.akka:akka-stream_2.13:$akkaVersion")
    implementation("com.typesafe.akka:akka-http_2.13:$akkaHttpVersion")
    implementation("com.typesafe.akka:akka-http-spray-json_2.13:$akkaHttpVersion")
    implementation("com.typesafe.akka:akka-slf4j_2.13:$akkaVersion")
    implementation("org.slf4j:slf4j-simple:1.7.30")

    implementation("com.github.swagger-akka-http:swagger-akka-http_2.13:2.1.0")
    implementation("io.swagger.core.v3:swagger-annotations:2.1.2")
    implementation("io.swagger.core.v3:swagger-jaxrs2:2.1.2")
    implementation("io.swagger:swagger-annotations:1.6.1")
    implementation("javax.ws.rs:javax.ws.rs-api:2.0.1")

    testImplementation("com.typesafe.akka:akka-testkit_2.13:$akkaVersion")
    testImplementation("com.typesafe.akka:akka-http-testkit_2.13:$akkaHttpVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}
