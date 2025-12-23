pluginManagement {
    repositories {
        maven {
            // GTNH 官方 Maven 仓库（用于插件和依赖）
            name = "GTNH Maven"
            url = uri("https://nexus.gtnewhorizons.com/repository/public/")
            mavenContent {
                includeGroup("com.gtnewhorizons")
                includeGroupByRegex("com\\.gtnewhorizons\\..+")
            }
        }
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    id("com.gtnewhorizons.gtnhsettingsconvention") version("1.0.43")
}

// 👇 关键：引入本地 Torcherino 项目，并设置依赖替换 👇
includeBuild("Torcherino-GTNH") {
    dependencySubstitution {
        substitute(module("com.github.czqwq:Torcherino")).using(project(":"))
    }
}
