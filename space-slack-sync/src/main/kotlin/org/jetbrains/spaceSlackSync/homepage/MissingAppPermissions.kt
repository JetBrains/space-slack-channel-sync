package org.jetbrains.spaceSlackSync.homepage

import kotlinx.serialization.Serializable
import org.jetbrains.spaceSlackSync.routing.SpaceTokenInfo
import space.jetbrains.api.runtime.resources.permissions
import space.jetbrains.api.runtime.types.*

class MissingAppPermissions(spaceTokenInfo: SpaceTokenInfo) : ServiceBase(spaceTokenInfo) {
    suspend fun getMissingAppPermissions(): MissingAppPermissionsResponse {
        val requiredPermissions = listOf(PermissionIdentifier.ViewMemberProfiles)

        val missingPermissions = requiredPermissions.filterNot { permission ->
            appSpaceClient.permissions.checkPermission(
                principal = PrincipalIn.Application(ApplicationIdentifier.Me),
                uniqueRightCode = permission,
                target = GlobalPermissionTarget,
            )
        }.map { "global:$it" }

        val hasPermissionsToApprove = userSpaceClient.permissions.checkPermission(
            principal = PrincipalIn.Profile(ProfileIdentifier.Me),
            uniqueRightCode = PermissionIdentifier.GrantPermissionsToOtherMembers,
            target = GlobalPermissionTarget,
        )

        return MissingAppPermissionsResponse(
            missingPermissions = missingPermissions.joinToString(" ").takeIf { it.isNotEmpty() },
            hasPermissionsToApprove = hasPermissionsToApprove
        )
    }
}

@Serializable
data class MissingAppPermissionsResponse(
    val missingPermissions: String?,
    val hasPermissionsToApprove: Boolean,
)
