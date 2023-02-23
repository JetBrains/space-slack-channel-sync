import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.spaceSlackSync.storage.SlackTeam
import org.jetbrains.spaceSlackSync.storage.Storage
import org.jetbrains.spaceSlackSync.storage.postgres.DB
import org.jetbrains.spaceSlackSync.storage.postgres.StorageImpl
import java.time.LocalDateTime

class SlackTeamsStorageImpl(db: Database) : StorageImpl(db), Storage.SlackTeams {
    override suspend fun getForSpaceOrg(spaceAppClientId: String): List<SlackTeam> {
        return tx {
            (DB.SlackTeams innerJoin DB.Slack2Space innerJoin DB.SpaceAppInstance)
                .slice(DB.SlackTeams.columns)
                .select {
                    (DB.SpaceAppInstance.clientId eq spaceAppClientId) and (DB.SlackTeams.tokenInvalid eq false)
                }
                .map { it.toSlackTeam() }
        }
    }

    override suspend fun getById(teamId: String, spaceAppClientId: String?): SlackTeam? {
        return tx {
            if (spaceAppClientId != null) {
                (DB.SlackTeams innerJoin DB.Slack2Space)
                    .slice(DB.SlackTeams.columns)
                    .select { (DB.SlackTeams.id eq teamId) and (DB.Slack2Space.spaceAppClientId eq spaceAppClientId) }
                    .firstOrNull()?.toSlackTeam()
            } else {
                DB.SlackTeams.select { DB.SlackTeams.id eq teamId }.firstOrNull()?.toSlackTeam()
            }
        }
    }

    override suspend fun getByDomain(domain: String): SlackTeam? {
        return tx {
            DB.SlackTeams.select { DB.SlackTeams.domain eq domain }.firstOrNull()?.toSlackTeam()
        }
    }

    override suspend fun updateDomain(teamId: String, newDomain: String) {
        tx {
            DB.SlackTeams.update(
                where = { DB.SlackTeams.id eq teamId },
                body = { it[domain] = newDomain }
            )
        }
    }

    override suspend fun updateTokens(
        teamId: String,
        accessToken: ByteArray,
        refreshToken: ByteArray?,
        accessTokenExpiresAt: LocalDateTime
    ) {
        tx {
            DB.SlackTeams.update(
                where = { DB.SlackTeams.id eq teamId },
                body = {
                    it[DB.SlackTeams.accessToken] = ExposedBlob(accessToken)
                    it[DB.SlackTeams.accessTokenExpiresAt] = accessTokenExpiresAt
                    if (refreshToken != null) {
                        it[DB.SlackTeams.refreshToken] = ExposedBlob(refreshToken)
                    }
                }
            )
        }
    }

    override suspend fun createOrUpdate(
        teamId: String,
        domain: String,
        spaceAppClientId: String,
        accessToken: ByteArray,
        refreshToken: ByteArray,
        accessTokenExpiresAt: LocalDateTime
    ) {
        tx {
            val teamExists = DB.SlackTeams.select { DB.SlackTeams.id eq teamId }.forUpdate().any()
            if (teamExists) {
                DB.SlackTeams.update(
                    where = { DB.SlackTeams.id eq teamId },
                    body = {
                        it[DB.SlackTeams.accessToken] = ExposedBlob(accessToken)
                        it[DB.SlackTeams.accessTokenExpiresAt] = accessTokenExpiresAt
                        it[DB.SlackTeams.refreshToken] = ExposedBlob(refreshToken)
                        it[DB.SlackTeams.tokenInvalid] = false
                    }
                )
            } else {
                DB.SlackTeams.insert {
                    it[DB.SlackTeams.id] = teamId
                    it[DB.SlackTeams.domain] = domain
                    it[DB.SlackTeams.created] = LocalDateTime.now()
                    it[DB.SlackTeams.accessToken] = ExposedBlob(accessToken)
                    it[DB.SlackTeams.accessTokenExpiresAt] = accessTokenExpiresAt
                    it[DB.SlackTeams.refreshToken] = ExposedBlob(refreshToken)
                }
            }

            DB.Slack2Space.insertIgnore {
                it[slackTeamId] = teamId
                it[DB.Slack2Space.spaceAppClientId] = spaceAppClientId
            }
        }
    }

    override suspend fun disconnectFromSpaceOrg(teamId: String, spaceAppClientId: String) {
        tx {
            DB.Slack2Space.deleteWhere { (DB.Slack2Space.slackTeamId eq teamId) and (DB.Slack2Space.spaceAppClientId eq spaceAppClientId) }
        }
    }

    override suspend fun markTokenAsInvalid(teamId: String) {
        tx {
            DB.SlackTeams.update(
                where = {
                    DB.SlackTeams.id eq teamId
                },
                body = {
                    it[tokenInvalid] = true
                }
            )
        }
    }


    private fun ResultRow.toSlackTeam() =
        SlackTeam(
            id = this[DB.SlackTeams.id].value,
            domain = this[DB.SlackTeams.domain],
            appAccessToken = this[DB.SlackTeams.accessToken].bytes,
            appRefreshToken = this[DB.SlackTeams.refreshToken].bytes,
            accessTokenExpiresAt = this[DB.SlackTeams.accessTokenExpiresAt]
        )
}