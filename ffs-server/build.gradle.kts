import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.kotlin.serialization.get().pluginId)
    id(libs.plugins.sqldelight.get().pluginId)
    application
    id(libs.plugins.kotlinx.benchmark.get().pluginId)
}

sourceSets.create("benchmark")

dependencies {
    implementation(project(":ffs-shared:env"))
    implementation(project(":ffs-shared:rule"))
    implementation(project(":ffs-shared:sse"))

    implementation(libs.bundles.ktor.server)

    implementation(libs.sqldelight.driver.sqlite)
    implementation(libs.sqldelight.coroutines.extensions)
    implementation(libs.kotlinx.datetime)
    implementation(libs.bouncycastle)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlinx.benchmark.runtime)

    val benchmarkImplementation by configurations.getting
    benchmarkImplementation(sourceSets.main.get().let { it.output + it.compileClasspath })
    benchmarkImplementation(libs.kotlinx.benchmark.runtime)
}

application {
    mainClass.set("ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.ExperimentalStdlibApi")
    languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
    languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
}

sqldelight {
    database("Database") {
        packageName = "doist.ffs"
    }
}

benchmark {
    configurations.named("main") {
        warmups = 2
        iterations = 3
        iterationTime = 5
        reportFormat = "csv"
        advanced("nativeGCAfterIteration", "true")
    }

    targets {
        register("benchmark")
    }
}
