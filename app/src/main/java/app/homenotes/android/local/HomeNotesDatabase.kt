package app.homenotes.android.local

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "notes",
    indices = [
        Index("account_key"),
        Index(value = ["account_key", "remote_id"], unique = true),
        Index(value = ["account_key", "recurrence_parent_local_id"]),
    ],
)
data class LocalNoteEntity(
    @PrimaryKey @ColumnInfo(name = "local_id") val localId: String,
    @ColumnInfo(name = "account_key") val accountKey: String,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    val title: String,
    val content: String,
    val category: String,
    @ColumnInfo(name = "deadline_millis") val deadlineMillis: Long?,
    @ColumnInfo(name = "start_at_millis") val startAtMillis: Long?,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Long?,
    @ColumnInfo(name = "repeat_rule") val repeatRule: String,
    @ColumnInfo(name = "coin_count") val coinCount: Int,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "recurrence_parent_local_id") val recurrenceParentLocalId: String? = null,
    @ColumnInfo(name = "is_provisional") val isProvisional: Boolean = false,
)

@Entity(
    tableName = "note_checklist_items",
    primaryKeys = ["note_local_id", "position"],
    foreignKeys = [ForeignKey(
        entity = LocalNoteEntity::class,
        parentColumns = ["local_id"],
        childColumns = ["note_local_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("note_local_id")],
)
data class LocalChecklistItemEntity(
    @ColumnInfo(name = "note_local_id") val noteLocalId: String,
    val position: Int,
    val text: String,
    @ColumnInfo(name = "is_checked") val isChecked: Boolean,
)

data class LocalNoteWithChecklist(
    @androidx.room.Embedded val note: LocalNoteEntity,
    @androidx.room.Relation(
        parentColumn = "local_id",
        entityColumn = "note_local_id",
    )
    val checklist: List<LocalChecklistItemEntity>,
)

@Entity(
    tableName = "home_cards",
    indices = [Index("account_key"), Index(value = ["account_key", "remote_id"], unique = true)],
)
data class LocalHomeCardEntity(
    @PrimaryKey @ColumnInfo(name = "local_id") val localId: String,
    @ColumnInfo(name = "account_key") val accountKey: String,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    val title: String,
    val section: String,
    val note: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "home_card_fields",
    primaryKeys = ["card_local_id", "position"],
    foreignKeys = [ForeignKey(
        entity = LocalHomeCardEntity::class,
        parentColumns = ["local_id"], childColumns = ["card_local_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("card_local_id")],
)
data class LocalHomeFieldEntity(
    @ColumnInfo(name = "card_local_id") val cardLocalId: String,
    val position: Int,
    val key: String,
    val value: String,
)

@Entity(
    tableName = "home_card_links",
    primaryKeys = ["card_local_id", "position"],
    foreignKeys = [ForeignKey(
        entity = LocalHomeCardEntity::class,
        parentColumns = ["local_id"], childColumns = ["card_local_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("card_local_id")],
)
data class LocalHomeLinkEntity(
    @ColumnInfo(name = "card_local_id") val cardLocalId: String,
    val position: Int,
    val url: String,
)

data class LocalHomeCardWithChildren(
    @androidx.room.Embedded val card: LocalHomeCardEntity,
    @androidx.room.Relation(parentColumn = "local_id", entityColumn = "card_local_id")
    val fields: List<LocalHomeFieldEntity>,
    @androidx.room.Relation(parentColumn = "local_id", entityColumn = "card_local_id")
    val links: List<LocalHomeLinkEntity>,
)

@Entity(tableName = "character_projection")
data class LocalCharacterEntity(
    @PrimaryKey @ColumnInfo(name = "account_key") val accountKey: String,
    val name: String,
    val level: Int,
    val xp: Int,
    @ColumnInfo(name = "xp_to_next") val xpToNext: Int,
    @ColumnInfo(name = "has_server_snapshot") val hasServerSnapshot: Boolean,
    @ColumnInfo(name = "snapshot_json") val snapshotJson: String? = null,
)

@Entity(tableName = "character_stats", primaryKeys = ["account_key", "stat_key"])
data class LocalCharacterStatEntity(
    @ColumnInfo(name = "account_key") val accountKey: String,
    @ColumnInfo(name = "stat_key") val statKey: String,
    val position: Int,
    val name: String,
    val description: String,
    val value: Int,
)

@Entity(tableName = "character_skills", primaryKeys = ["account_key", "skill_key"])
data class LocalCharacterSkillEntity(
    @ColumnInfo(name = "account_key") val accountKey: String,
    @ColumnInfo(name = "skill_key") val skillKey: String,
    val position: Int,
    val name: String,
    val level: Int,
    val progress: Double,
)

@Entity(tableName = "inventory_items", primaryKeys = ["account_key", "item_id"])
data class LocalInventoryItemEntity(
    @ColumnInfo(name = "account_key") val accountKey: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    val position: Int,
    val name: String,
    val description: String,
    val icon: String,
    // Слот экипировки (HEAD/BODY/...) или null для ненадеваемых предметов.
    val slot: String?,
    val rarity: String,
    // Бонусы к характеристикам, сериализованные в JSON (List<ItemBonus>).
    @ColumnInfo(name = "bonuses_json") val bonusesJson: String,
    // Слот, в который предмет надет сейчас, или null.
    @ColumnInfo(name = "equipped_slot") val equippedSlot: String?,
)

/**
 * Кэш глобального каталога событий фокус-таймера (GET /events). Каталог общий для всех
 * аккаунтов, поэтому account_key нет. Item/новый навык хранятся как JSON (Gson).
 */
@Entity(tableName = "focus_events")
data class LocalFocusEventEntity(
    @PrimaryKey @ColumnInfo(name = "event_key") val eventKey: String,
    @ColumnInfo(name = "event_type") val eventType: String,
    val weight: Int,
    @ColumnInfo(name = "text_ru") val textRu: String,
    @ColumnInfo(name = "text_en") val textEn: String,
    @ColumnInfo(name = "character_xp") val characterXp: Int,
    @ColumnInfo(name = "skill_xp") val skillXp: Int,
    @ColumnInfo(name = "item_json") val itemJson: String?,
    @ColumnInfo(name = "new_skill_json") val newSkillJson: String?,
)

@Entity(tableName = "wallet")
data class LocalWalletEntity(
    @PrimaryKey @ColumnInfo(name = "account_key") val accountKey: String,
    @ColumnInfo(name = "earned_coins") val earnedCoins: Int = 0,
    @ColumnInfo(name = "spent_coins") val spentCoins: Int = 0,
    @ColumnInfo(name = "available_coins") val availableCoins: Int = 0,
    @ColumnInfo(name = "legacy_spend_migrated") val legacySpendMigrated: Boolean = false,
)

@Entity(
    tableName = "outbox",
    indices = [Index(value = ["account_key", "created_at", "id"]), Index("local_id")],
)
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "account_key") val accountKey: String,
    val kind: String,
    @ColumnInfo(name = "local_id") val localId: String?,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "operation_id") val operationId: String,
    val payload: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val attempts: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    val blocked: Boolean = false,
)

@Entity(tableName = "sync_state")
data class LocalSyncStateEntity(
    @PrimaryKey @ColumnInfo(name = "account_key") val accountKey: String,
    val status: String,
    @ColumnInfo(name = "pending_count") val pendingCount: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "last_synced_at") val lastSyncedAt: Long? = null,
)

@Dao
abstract class OfflineDao {
    @Transaction
    @Query("SELECT * FROM notes WHERE account_key = :accountKey ORDER BY updated_at DESC")
    abstract fun observeNotes(accountKey: String): Flow<List<LocalNoteWithChecklist>>

    @Transaction
    @Query("SELECT * FROM notes WHERE account_key = :accountKey ORDER BY updated_at DESC")
    abstract suspend fun notes(accountKey: String): List<LocalNoteWithChecklist>

    @Transaction
    @Query("SELECT * FROM notes WHERE local_id = :localId AND account_key = :accountKey")
    abstract suspend fun note(accountKey: String, localId: String): LocalNoteWithChecklist?

    @Transaction
    @Query("SELECT * FROM notes WHERE remote_id = :remoteId AND account_key = :accountKey")
    abstract suspend fun noteByRemoteId(accountKey: String, remoteId: String): LocalNoteWithChecklist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putNote(note: LocalNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putChecklist(items: List<LocalChecklistItemEntity>)

    @Query("DELETE FROM note_checklist_items WHERE note_local_id = :localId")
    abstract suspend fun clearChecklist(localId: String)

    @Query("DELETE FROM notes WHERE local_id = :localId AND account_key = :accountKey")
    abstract suspend fun deleteNote(accountKey: String, localId: String)

    @Query("UPDATE notes SET remote_id = :remoteId, is_provisional = 0 WHERE local_id = :localId")
    abstract suspend fun bindNoteRemoteId(localId: String, remoteId: String)

    @Transaction
    open suspend fun replaceNote(note: LocalNoteEntity, items: List<LocalChecklistItemEntity>) {
        putNote(note)
        clearChecklist(note.localId)
        if (items.isNotEmpty()) putChecklist(items)
    }

    @Transaction
    @Query("SELECT * FROM home_cards WHERE account_key = :accountKey ORDER BY updated_at DESC")
    abstract fun observeCards(accountKey: String): Flow<List<LocalHomeCardWithChildren>>

    @Transaction
    @Query("SELECT * FROM home_cards WHERE account_key = :accountKey ORDER BY updated_at DESC")
    abstract suspend fun cards(accountKey: String): List<LocalHomeCardWithChildren>

    @Transaction
    @Query("SELECT * FROM home_cards WHERE local_id = :localId AND account_key = :accountKey")
    abstract suspend fun card(accountKey: String, localId: String): LocalHomeCardWithChildren?

    @Transaction
    @Query("SELECT * FROM home_cards WHERE remote_id = :remoteId AND account_key = :accountKey")
    abstract suspend fun cardByRemoteId(accountKey: String, remoteId: String): LocalHomeCardWithChildren?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putCard(card: LocalHomeCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putFields(fields: List<LocalHomeFieldEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putLinks(links: List<LocalHomeLinkEntity>)

    @Query("DELETE FROM home_card_fields WHERE card_local_id = :localId")
    abstract suspend fun clearFields(localId: String)

    @Query("DELETE FROM home_card_links WHERE card_local_id = :localId")
    abstract suspend fun clearLinks(localId: String)

    @Query("DELETE FROM home_cards WHERE local_id = :localId AND account_key = :accountKey")
    abstract suspend fun deleteCard(accountKey: String, localId: String)

    @Query("UPDATE home_cards SET remote_id = :remoteId WHERE local_id = :localId")
    abstract suspend fun bindCardRemoteId(localId: String, remoteId: String)

    @Transaction
    open suspend fun replaceCard(
        card: LocalHomeCardEntity,
        fields: List<LocalHomeFieldEntity>,
        links: List<LocalHomeLinkEntity>,
    ) {
        putCard(card)
        clearFields(card.localId)
        clearLinks(card.localId)
        if (fields.isNotEmpty()) putFields(fields)
        if (links.isNotEmpty()) putLinks(links)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putCharacter(character: LocalCharacterEntity)

    @Query("SELECT * FROM character_projection WHERE account_key = :accountKey")
    abstract suspend fun character(accountKey: String): LocalCharacterEntity?

    @Query("SELECT * FROM character_stats WHERE account_key = :accountKey ORDER BY position")
    abstract suspend fun characterStats(accountKey: String): List<LocalCharacterStatEntity>

    @Query("SELECT * FROM character_skills WHERE account_key = :accountKey ORDER BY position")
    abstract suspend fun characterSkills(accountKey: String): List<LocalCharacterSkillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putCharacterStats(stats: List<LocalCharacterStatEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putCharacterSkills(skills: List<LocalCharacterSkillEntity>)

    @Query("DELETE FROM character_stats WHERE account_key = :accountKey")
    abstract suspend fun clearCharacterStats(accountKey: String)

    @Query("DELETE FROM character_skills WHERE account_key = :accountKey")
    abstract suspend fun clearCharacterSkills(accountKey: String)

    @Transaction
    open suspend fun replaceCharacter(
        character: LocalCharacterEntity,
        stats: List<LocalCharacterStatEntity>,
        skills: List<LocalCharacterSkillEntity>,
        wallet: LocalWalletEntity,
    ) {
        putCharacter(character)
        clearCharacterStats(character.accountKey)
        clearCharacterSkills(character.accountKey)
        if (stats.isNotEmpty()) putCharacterStats(stats)
        if (skills.isNotEmpty()) putCharacterSkills(skills)
        putWallet(wallet)
    }

    @Query("SELECT * FROM inventory_items WHERE account_key = :accountKey ORDER BY position")
    abstract suspend fun inventoryItems(accountKey: String): List<LocalInventoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putInventoryItems(items: List<LocalInventoryItemEntity>)

    @Query("DELETE FROM inventory_items WHERE account_key = :accountKey")
    abstract suspend fun clearInventory(accountKey: String)

    @Transaction
    open suspend fun replaceInventory(accountKey: String, items: List<LocalInventoryItemEntity>) {
        clearInventory(accountKey)
        if (items.isNotEmpty()) putInventoryItems(items)
    }

    @Query("SELECT * FROM focus_events")
    abstract suspend fun focusEvents(): List<LocalFocusEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putFocusEvents(events: List<LocalFocusEventEntity>)

    @Query("DELETE FROM focus_events")
    abstract suspend fun clearFocusEvents()

    @Transaction
    open suspend fun replaceFocusEvents(events: List<LocalFocusEventEntity>) {
        clearFocusEvents()
        if (events.isNotEmpty()) putFocusEvents(events)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putWallet(wallet: LocalWalletEntity)

    @Query("SELECT * FROM wallet WHERE account_key = :accountKey")
    abstract suspend fun wallet(accountKey: String): LocalWalletEntity?

    @Insert
    abstract suspend fun enqueue(operation: OutboxEntity): Long

    @Query("SELECT * FROM outbox WHERE account_key = :accountKey ORDER BY created_at, id")
    abstract suspend fun outbox(accountKey: String): List<OutboxEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM outbox WHERE account_key = :accountKey AND local_id = :localId)")
    abstract suspend fun hasPending(accountKey: String, localId: String): Boolean

    @Query("DELETE FROM outbox WHERE id = :id")
    abstract suspend fun removeOutbox(id: Long)

    @Query("DELETE FROM outbox WHERE account_key = :accountKey AND local_id = :localId")
    abstract suspend fun removeOutboxForLocal(accountKey: String, localId: String)

    @Query("UPDATE outbox SET attempts = attempts + 1, last_error = :error, blocked = :blocked WHERE id = :id")
    abstract suspend fun failOutbox(id: Long, error: String?, blocked: Boolean)

    @Query("SELECT COUNT(*) FROM outbox WHERE account_key = :accountKey")
    abstract suspend fun pendingCount(accountKey: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putSyncState(state: LocalSyncStateEntity)

    @Query("SELECT * FROM sync_state WHERE account_key = :accountKey")
    abstract fun observeSyncState(accountKey: String): Flow<LocalSyncStateEntity?>
}

@Database(
    entities = [
        LocalNoteEntity::class, LocalChecklistItemEntity::class,
        LocalHomeCardEntity::class, LocalHomeFieldEntity::class, LocalHomeLinkEntity::class,
        LocalCharacterEntity::class, LocalCharacterStatEntity::class, LocalCharacterSkillEntity::class,
        LocalInventoryItemEntity::class,
        LocalFocusEventEntity::class,
        LocalWalletEntity::class, OutboxEntity::class, LocalSyncStateEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class HomeNotesDatabase : RoomDatabase() {
    abstract fun offlineDao(): OfflineDao

    companion object {
        const val DATABASE_NAME = "homenotes.db"

        // v1 -> v2: таблица предметов инвентаря и экипировки.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `inventory_items` (" +
                        "`account_key` TEXT NOT NULL, " +
                        "`item_id` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, " +
                        "`icon` TEXT NOT NULL, " +
                        "`slot` TEXT, " +
                        "`rarity` TEXT NOT NULL, " +
                        "`bonuses_json` TEXT NOT NULL, " +
                        "`equipped_slot` TEXT, " +
                        "PRIMARY KEY(`account_key`, `item_id`))"
                )
            }
        }

        // v2 -> v3: кэш каталога событий фокус-таймера.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `focus_events` (" +
                        "`event_key` TEXT NOT NULL, " +
                        "`event_type` TEXT NOT NULL, " +
                        "`weight` INTEGER NOT NULL, " +
                        "`text_ru` TEXT NOT NULL, " +
                        "`text_en` TEXT NOT NULL, " +
                        "`character_xp` INTEGER NOT NULL, " +
                        "`skill_xp` INTEGER NOT NULL, " +
                        "`item_json` TEXT, " +
                        "`new_skill_json` TEXT, " +
                        "PRIMARY KEY(`event_key`))"
                )
            }
        }

        fun create(context: Context): HomeNotesDatabase = Room.databaseBuilder(
            context.applicationContext,
            HomeNotesDatabase::class.java,
            DATABASE_NAME,
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }
}
