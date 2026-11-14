pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FarmSaathi"

include(":app")
include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:database")
include(":core:datastore")
include(":core:audio")
include(":core:media")
include(":feature:home")
include(":feature:farm")
include(":feature:diary")
include(":feature:finance")
include(":feature:operations")
include(":feature:reports")
