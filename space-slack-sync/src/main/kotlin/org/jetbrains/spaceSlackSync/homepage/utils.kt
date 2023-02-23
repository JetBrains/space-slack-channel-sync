package org.jetbrains.spaceSlackSync.homepage

import space.jetbrains.api.runtime.resources.permissions
import space.jetbrains.api.runtime.types.*

suspend fun ServiceBase.userHasAdminPermission(spaceChannelId: String): Boolean {
    return userSpaceClient.permissions.checkPermission(
        PrincipalIn.Profile(ProfileIdentifier.Me),
        PermissionIdentifier.ManageChannels,
        ChannelPermissionTarget(ChannelIdentifier.Id(spaceChannelId))
    ) || userSpaceClient.permissions.checkPermission(
        PrincipalIn.Profile(ProfileIdentifier.Me),
        PermissionIdentifier.GrantPermissionsToOtherMembers,
        GlobalPermissionTarget
    )
}
