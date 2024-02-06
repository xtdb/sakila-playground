plugins {
    `java-library`
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://repo.clojars.org")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")}

val defaultJvmArgs = listOf(
    "--add-opens=java.base/java.nio=ALL-UNNAMED",
    "-Dio.netty.tryReflectionSetAccessible=true",
    "-Darrow.memory.debug.allocator=false",
)

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

dependencies {
    testImplementation("com.xtdb", "xtdb-api", "2.0.0-SNAPSHOT")
    testImplementation("com.xtdb", "xtdb-http-client-jvm", "2.0.0-SNAPSHOT")
    testImplementation(kotlin("script-runtime"))
}
