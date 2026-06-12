plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    `maven-publish`
}

group = "io.github.furka.pigeon"
version = "1.0.0"

val ktor_version = "2.3.5"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-websockets:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.furka.pigeon"
            artifactId = "pigeon-bot"
            version = "1.0.0"

            from(components["kotlin"])
            
            pom {
                name.set("pigeon-bot")
                description.set("A Kotlin library for building chat bots on the Pigeon Messenger")
                url.set("https://github.com/PigeonMessage/pigeon-bot-kotlin")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("BenimFurka")
                        name.set("BenimFurka")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/PigeonMessage/pigeon-bot-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com/PigeonMessage/pigeon-bot-kotlin.git")
                    url.set("https://github.com/PigeonMessage/pigeon-bot-kotlin")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/PigeonMessage/pigeon-bot-kotlin")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}