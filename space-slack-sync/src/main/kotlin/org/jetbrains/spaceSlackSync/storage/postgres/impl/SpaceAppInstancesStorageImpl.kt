package org.jetbrains.spaceSlackSync.storage.postgres.impl

import io.ktor.http.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.spaceSlackSync.decrypted
import org.jetbrains.spaceSlackSync.encrypted
import org.jetbrains.spaceSlackSync.storage.Storage
import org.jetbrains.spaceSlackSync.storage.postgres.DB
import org.jetbrains.spaceSlackSync.storage.postgres.StorageImpl
import space.jetbrains.api.runtime.SpaceAppInstance
import java.time.LocalDateTime

class SpaceAppInstancesStorageImpl(db: Database) : StorageImpl(db), Storage.SpaceAppInstances {
    override suspend fun save(spaceAppInstance: SpaceAppInstance) {
        tx {
            val domain = Url(spaceAppInstance.spaceServer.serverUrl).host
            DB.SpaceAppInstance.deleteWhere {
                (DB.SpaceAppInstance.clientId eq spaceAppInstance.clientId) or (DB.SpaceAppInstance.domain eq domain)
            }
            DB.SpaceAppInstance.insert {
                it[created] = LocalDateTime.now()
                it[clientId] = spaceAppInstance.clientId
                it[clientSecret] = ExposedBlob(spaceAppInstance.clientSecret.encrypted())
                it[orgUrl] = spaceAppInstance.spaceServer.serverUrl
                it[DB.SpaceAppInstance.domain] = domain
            }
        }
    }

    override suspend fun getById(clientId: String, slackTeamId: String?): SpaceAppInstance? {
        return tx {
            if (slackTeamId != null) {
                (DB.SpaceAppInstance innerJoin DB.Slack2Space)
                    .slice(DB.SpaceAppInstance.columns)
                    .select { (DB.SpaceAppInstance.clientId eq clientId) and (DB.Slack2Space.slackTeamId eq slackTeamId) }
                    .firstOrNull()?.toSpaceOrg()
            } else {
                DB.SpaceAppInstance
                    .select { DB.SpaceAppInstance.clientId eq clientId }
                    .firstOrNull()?.toSpaceOrg()
            }
        }
    }

    override suspend fun getByIds(clientIds: List<String>): List<SpaceAppInstance> {
        return tx {
            DB.SpaceAppInstance
                .select { DB.SpaceAppInstance.clientId inList clientIds }
                .map { it.toSpaceOrg() }
        }
    }

    override suspend fun getByDomain(domain: String, slackTeamId: String): SpaceAppInstance? {
        return tx {
            (DB.SpaceAppInstance innerJoin DB.Slack2Space)
                .slice(DB.SpaceAppInstance.columns)
                .select { (DB.SpaceAppInstance.domain eq domain) and (DB.Slack2Space.slackTeamId eq slackTeamId) }
                .map { it.toSpaceOrg() }
                .firstOrNull()
        }
    }

    override suspend fun delete(clientId: String) {
        tx {
            DB.SpaceAppInstance.deleteWhere {
                DB.SpaceAppInstance.clientId eq clientId
            }
        }
    }

    private fun ResultRow.toSpaceOrg() =
        SpaceAppInstance(
            clientId = this[DB.SpaceAppInstance.clientId],
            clientSecret = this[DB.SpaceAppInstance.clientSecret].bytes.decrypted(),
            spaceServerUrl = this[DB.SpaceAppInstance.orgUrl],
        )
}
