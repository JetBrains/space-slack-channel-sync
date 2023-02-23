package org.jetbrains.spaceSlackSync.storage.postgres.impl

import org.jetbrains.exposed.sql.*
import org.jetbrains.spaceSlackSync.storage.MessageInfo
import org.jetbrains.spaceSlackSync.storage.Storage
import org.jetbrains.spaceSlackSync.storage.postgres.DB
import org.jetbrains.spaceSlackSync.storage.postgres.StorageImpl

class MessagesStorageImpl(db: Database) : StorageImpl(db), Storage.Messages {
    override suspend fun getInfoBySpaceMsg(slackTeamId: String, spaceMessageId: String): MessageInfo? {
        return tx {
            DB.SpaceToSlackMessage
                .select {
                    (DB.SpaceToSlackMessage.slackTeamId eq slackTeamId) and (DB.SpaceToSlackMessage.spaceMessageId eq spaceMessageId)
                }
                .map {
                    MessageInfo(
                        slackMessageId = it[DB.SpaceToSlackMessage.slackMessageId],
                        spaceMessageId = it[DB.SpaceToSlackMessage.spaceMessageId],
                        deleted = it[DB.SpaceToSlackMessage.deleted] == true,
                    )
                }
                .firstOrNull()
        }
    }

    override suspend fun getInfoBySlackMsg(slackTeamId: String, slackMessageId: String): MessageInfo? {
        return tx {
            DB.SpaceToSlackMessage
                .select {
                    (DB.SpaceToSlackMessage.slackTeamId eq slackTeamId) and (DB.SpaceToSlackMessage.slackMessageId eq slackMessageId)
                }
                .map {
                    MessageInfo(
                        slackMessageId = it[DB.SpaceToSlackMessage.slackMessageId],
                        spaceMessageId = it[DB.SpaceToSlackMessage.spaceMessageId],
                        deleted = it[DB.SpaceToSlackMessage.deleted] == true,
                    )
                }
                .firstOrNull()
        }
    }

    override suspend fun setSlackMsgBySpaceMsg(
        slackTeamId: String,
        slackMessageId: String,
        spaceMessageId: String
    ) {
        tx {
            DB.SpaceToSlackMessage.insertIgnore {
                it[DB.SpaceToSlackMessage.slackTeamId] = slackTeamId
                it[DB.SpaceToSlackMessage.slackMessageId] = slackMessageId
                it[DB.SpaceToSlackMessage.spaceMessageId] = spaceMessageId
            }
        }
    }

    override suspend fun markAsDeletedBySpaceMessageId(slackTeamId: String, spaceMessageId: String) {
        tx {
            DB.SpaceToSlackMessage.update(
                where = {
                    (DB.SpaceToSlackMessage.slackTeamId eq slackTeamId) and (DB.SpaceToSlackMessage.spaceMessageId eq spaceMessageId)
                }, body = {
                    it[deleted] = true
                }
            )
        }
    }

    override suspend fun markAsDeletedBySlackMessageId(slackTeamId: String, slackMessageId: String) {
        tx {
            DB.SpaceToSlackMessage.update(
                where = {
                    (DB.SpaceToSlackMessage.slackTeamId eq slackTeamId) and (DB.SpaceToSlackMessage.slackMessageId eq slackMessageId)
                }, body = {
                    it[deleted] = true
                }
            )
        }
    }
}
