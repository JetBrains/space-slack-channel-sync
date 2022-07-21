rootProject.name = "space-slack-sync"

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "docker-compose") {
                useModule("com.avast.gradle:gradle-docker-compose-plugin:0.14.0")
            }
        }
    }
}
