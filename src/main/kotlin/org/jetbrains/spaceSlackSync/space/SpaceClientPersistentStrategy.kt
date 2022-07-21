package org.jetbrains.spaceSlackSync.space

import org.jetbrains.spaceSlackSync.db
import space.jetbrains.api.runtime.SpaceAppInstance
import space.jetbrains.api.runtime.helpers.SpaceAppInstanceStorage

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
}
