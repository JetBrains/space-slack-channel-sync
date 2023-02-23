package org.jetbrains.spaceSlackSync.storage.postgres

import SlackTeamsStorageImpl
import io.ktor.http.*
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.spaceSlackSync.storage.*
import org.jetbrains.spaceSlackSync.storage.postgres.impl.*

class PostgresStorage(private val db: Database) : Storage {
    @Suppress("RemoveRedundantQualifierName")
    override val slackTeams = SlackTeamsStorageImpl(db)
    override val spaceAppInstances = SpaceAppInstancesStorageImpl(db)
    override val syncedChannels = SyncedChannelsStorageImpl(db)
    override val messages = MessagesStorageImpl(db)
    override val slackTeamCache = SlackTeamCacheStorageImpl(db)
}

abstract class StorageImpl(protected val db: Database) {
    protected fun <T> tx(statement: (Transaction) -> T): T =
        transaction(db, statement)
}
