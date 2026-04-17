// Root build – pins third-party plugin versions for subprojects (apply false = available but not active here).
plugins {
    id("org.springframework.boot") version "3.5.12"        apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}

subprojects {
    repositories {
        mavenCentral()
    }
}
