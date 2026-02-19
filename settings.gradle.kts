pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    versionCatalogs {
        create("libs") {
            from(files("mobile/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "mrs-team26-Pekari"
include(":mobile:app")
project(":mobile:app").projectDir = file("mobile/app")
include(":mobile")
project(":mobile").projectDir = file("mobile")
