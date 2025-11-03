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
        // ✅ MPAndroidChart 라이브러리를 위한 저장소 주소 추가 (코틀린 문법으로 수정)
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "NeckFree"
include(":app")