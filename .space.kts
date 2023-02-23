/**
* JetBrains Space Automation
* This Kotlin-script file lets you automate build activities
* For more info, see https://www.jetbrains.com/help/space/automation.html
*/

job("Build and push Docker") {
    startOn {
        gitPush { enabled = false }
    }
    buildAndZip()
    pushToDocker()
}

job ("Build and Deploy") {
    startOn {
        gitPush { enabled = false }
    }
    buildAndZip()
    pushToDocker()
    kubeDeploy()
}

fun Job.buildAndZip() {
    container("gradle:8.0-jdk11") {
        kotlinScript { api ->
            api.gradlew("distZip")
            api.fileShare().put(java.io.File("./space-slack-sync/build/distributions/space-slack-sync.zip"), "space-slack-sync.zip")
        }
    }
}

fun Job.pushToDocker() {
    docker {
        beforeBuildScript {
            content = "mkdir docker && cp /mnt/space/share/space-slack-sync.zip docker/space-slack-sync.zip"
        }
        build {
            context = "docker"
            file = "./space-slack-sync/dockerfile"
            labels["vendor"] = "JetBrains"
        }
        push("registry.jetbrains.team/p/s2s/docker/space-slack-channel-sync") {
            tags("\$JB_SPACE_EXECUTION_NUMBER")
        }
    }
}

fun Job.kubeDeploy() {
    container("jbsre/k8s-handle@sha256:3119b11f952ce59b50e00c3c24da326976cfb138c3c456b6b63ea3145f285e1c") {
        // K8S_TOKEN environment variable is set here, while the rest of the environment variables
        // can be found in `/deploy/config.yaml`
        env["K8S_TOKEN"] = Secrets("slack-tunnel-cloud-k8s-token")
        workDir = "./space-slack-sync/deploy"
        shellScript {
            content = """
                k8s-handle deploy --section space-slack-channel-sync --sync-mode
            """
        }
    }
}
