package org.jetbrains.spaceSlackSync.space

import org.jetbrains.spaceSlackSync.db
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.SpaceAppInstance
import space.jetbrains.api.runtime.helpers.SpaceAppInstanceStorage
import java.lang.invoke.MethodHandles

val spaceAppInstanceStorage = object : SpaceAppInstanceStorage {

    override suspend fun loadAppInstance(clientId: String): SpaceAppInstance? {
        return db.spaceAppInstances.getById(clientId)?.let {
            SpaceAppInstance(
                clientId = it.clientId,
                clientSecret = it.clientSecret,
                spaceServerUrl = it.spaceServer.serverUrl,
            )
        }
    }

    override suspend fun saveAppInstance(appInstance: SpaceAppInstance) {
        db.spaceAppInstances.save(appInstance)
    }

    override suspend fun removeAppInstance(clientId: String) {
        log.info("Application uninstalled, clientId: $clientId")
        db.spaceAppInstances.delete(clientId)
    }
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
