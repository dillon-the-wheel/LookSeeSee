pluginManagement {
    repositories {
        // Aliyun mirrors first: google()/mavenCentral()/gradlePluginPortal() are slow or
        // blocked from mainland China, even over a VPN (DNS interference, not just routing).
        // These mirror the same artifacts from Chinese infrastructure. Safe to remove the
        // three "maven {...}" blocks below if you're not behind the Great Firewall.
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}

rootProject.name = "LookSeeSee"
include(":app")
