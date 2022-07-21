package org.jetbrains.spaceSlackSync.homepage

import org.jetbrains.spaceSlackSync.routing.SpaceTokenInfo
import space.jetbrains.api.runtime.SpaceAuth
import space.jetbrains.api.runtime.SpaceClient

abstract class ServiceBase(val spaceTokenInfo: SpaceTokenInfo) {
    val userSpaceClient = SpaceClient(
        spaceHttpClient,
        spaceTokenInfo.spaceAppInstance.spaceServer.serverUrl,
        spaceTokenInfo.spaceAccessToken
    )

    val appSpaceClient = SpaceClient(spaceHttpClient, spaceTokenInfo.spaceAppInstance, SpaceAuth.ClientCredentials())
}
