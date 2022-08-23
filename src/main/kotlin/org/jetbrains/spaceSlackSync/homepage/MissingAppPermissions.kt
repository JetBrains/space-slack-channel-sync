package org.jetbrains.spaceSlackSync.homepage

import kotlinx.serialization.Serializable
import org.jetbrains.spaceSlackSync.routing.SpaceTokenInfo
import space.jetbrains.api.runtime.resources.permissions
import space.jetbrains.api.runtime.types.ApplicationIdentifier
import space.jetbrains.api.runtime.types.GlobalPermissionTarget
import space.jetbrains.api.runtime.types.PrincipalIn
import space.jetbrains.api.runtime.types.ProfileIdentifier

class MissingAppPermissions(spaceTokenInfo: SpaceTokenInfo) : ServiceBase(spaceTokenInfo) {
    suspend fun getMissingAppPermissions(): MissingAppPermissionsResponse {
        val requiredPermissions = listOf("Profile.View")

        val missingPermissions = requiredPermissions.filterNot { permissionCode ->
            appSpaceClient.permissions.checkPermission(
                principal = PrincipalIn.Application(ApplicationIdentifier.Me),
                uniqueRightCode = permissionCode,
                target = GlobalPermissionTarget,
            )
        }.map { "global:$it" }

        val hasPermissionsToApprove = userSpaceClient.permissions.checkPermission(
            principal = PrincipalIn.Profile(ProfileIdentifier.Me),
            uniqueRightCode = "Superadmin",
            target = GlobalPermissionTarget,
        )

        return MissingAppPermissionsResponse(missingPermissions.joinToString(" ").takeIf { it.isNotEmpty() }, hasPermissionsToApprove)
    }
}

@Serializable
data class MissingAppPermissionsResponse(
    val missingPermissions: String?,
    val hasPermissionsToApprove: Boolean,
)
