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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()

        maven("https://maven.pkg.jetbrains.space/public/p/space/maven")

        /**
         * Repository used by `node` plugin to download Node.js.
         * See also: https://github.com/node-gradle/gradle-node-plugin/blob/master/docs/faq.md#is-this-plugin-compatible-with-centralized-repositories-declaration
         */
        ivy {
            name = "Node.js"
            setUrl("https://nodejs.org/dist/")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("org.nodejs", "node")
            }
        }
    }
}

include("space-slack-sync")
include("dev-tools")
