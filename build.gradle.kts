import de.undercouch.gradle.tasks.download.Download
import java.nio.charset.StandardCharsets

plugins {
    java
    checkstyle
    idea
    alias(libs.plugins.runPaper)
    alias(libs.plugins.downloadTask)
    alias(libs.plugins.licenser)
}

group = "me.machinemaker"
version = "0.1.0"

repositories {
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    compileOnly(libs.paper)
    compileOnly(libs.protocolLib)
}

inline fun <reified T : Task> TaskContainer.registering(noinline configuration: T.() -> Unit) = registering(T::class, configuration)
val cache = layout.buildDirectory.dir("cache")

tasks {
    val downloadProtocolLib by registering<Download> {
        src("https://ci.dmulloy2.net/job/ProtocolLib/lastSuccessfulBuild/artifact/target/ProtocolLib.jar")
        dest(cache)
    }

    val downloadLuckPerms by registering<Download> {
        src("https://download.luckperms.net/1460/bukkit/loader/LuckPerms-Bukkit-5.4.52.jar")
        dest(cache)
    }

    runServer {
        dependsOn(downloadProtocolLib, downloadLuckPerms)
        minecraftVersion("1.19.2")
        pluginJars(*downloadProtocolLib.get().outputFiles.toTypedArray())
        pluginJars(*downloadLuckPerms.get().outputFiles.toTypedArray())
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}

checkstyle {
    configDirectory.set(rootProject.file(".checkstyle"))
    isShowViolations = true
    toolVersion = "10.3"
}

license {
    header = rootProject.file("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
}


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}


tasks {
    test {
        useJUnitPlatform()
    }

    processResources {
        filteringCharset = StandardCharsets.UTF_8.name()
    }

    javadoc {
        options.encoding = StandardCharsets.UTF_8.name()
    }

    compileJava {
        options.encoding = StandardCharsets.UTF_8.name()
    }

    jar {
        from(rootProject.projectDir) {
            into("META-INF")
            include("LICENSE")
        }
    }
}

idea {
    module {
        isDownloadSources = true
    }
}

