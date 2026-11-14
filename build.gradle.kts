plugins {
    java
}

group = "red.ria"
version = "1.3.1"

// 在配置阶段取出版本号，避免在任务执行阶段访问 project（Gradle 10 将不再允许）。
val pluginVersion = version.toString()

// 默认面向 Paper 26.1.2 编译；可用 -PpaperApiVersion=26.3.build.249-stable 切换到其他 API 版本。
val paperApiVersion = providers.gradleProperty("paperApiVersion")
    .getOrElse("26.1.2.build.74-stable")

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")

    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.test {
    useJUnitPlatform()
}


