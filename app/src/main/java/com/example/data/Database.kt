package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// --- Entities ---

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val avatarColorOrdinal: Int, // Index of color in Theme for avatars
    val statusText: String,
    val isAiBot: Boolean = false,
    val isNearbyMesh: Boolean = false
)

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val chatId: String,
    val title: String,
    val lastMessageText: String,
    val lastMessageTime: Long,
    val isGroup: Boolean,
    val isWifiP2p: Boolean,
    val e2eKey: String = "WhatsAppDefaultE2EKey123"
)

@Entity(tableName = "messages")
data class DbMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val contentEncrypted: String,
    val timestamp: Long,
    val isMine: Boolean,
    val isWifiBroadcast: Boolean = false,
    val isAiResponse: Boolean = false,
    val isEncrypted: Boolean = true
)

// --- DAOs ---

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<Contact>)
    
    @Query("UPDATE contacts SET isNearbyMesh = :isNearby WHERE id = :id")
    suspend fun updateMeshStatus(id: String, isNearby: Boolean)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY lastMessageTime DESC")
    fun getAllChatsFlow(): Flow<List<Chat>>

    @Query("SELECT * FROM chats WHERE chatId = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): Chat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat)

    @Update
    suspend fun updateChat(chat: Chat)

    @Query("DELETE FROM chats WHERE chatId = :chatId")
    suspend fun deleteChat(chatId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<DbMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: DbMessage)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)
}

// --- Database Class ---

@Database(entities = [Contact::class, Chat::class, DbMessage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whatsapp_connect_db"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            val contactDao = db.contactDao()
            val chatDao = db.chatDao()
            val messageDao = db.messageDao()

            // Seed Contacts
            val initialContacts = listOf(
                Contact("ai_assistant", "Devil AI Assistant", "Ollama Generative AI", 0, "Online | Ask me anything!", isAiBot = true),
                Contact("sarah_a", "Sarah Abbott", "+1 (555) 732-8491", 1, "Hey there! I am using samvixo."),
                Contact("peter_p", "Peter Parker", "+1 (555) 231-0982", 2, "With great power... comes offline chat!"),
                Contact("mesh_node_alpha", "Mesh Node Alpha (P2P)", "Offline Broadcast Node", 3, "Broadcasting on Local Mesh", isNearbyMesh = true)
            )
            contactDao.insertAll(initialContacts)

            // Keys for encryption demo
            val sarahKey = "SarahSecretKey789"
            val groupKey = "FamilyGroupSuperSecretKey55"

            // Seed Chats
            chatDao.insertChat(Chat(
                "ai_assistant",
                "Devil AI Assistant",
                "Hi there! I am powered by Devil AI & Ollama. Ask me any question!",
                System.currentTimeMillis() - 1000 * 60 * 60 * 2, // 2 hours ago
                isGroup = false,
                isWifiP2p = false,
                e2eKey = "None - API Direct"
            ))

            chatDao.insertChat(Chat(
                "sarah_a",
                "Sarah Abbott",
                EncryptionUtils.encrypt("Hey! Are you coming to the park?", sarahKey),
                System.currentTimeMillis() - 1000 * 60 * 30, // 30 mins ago
                isGroup = false,
                isWifiP2p = false,
                e2eKey = sarahKey
            ))

            chatDao.insertChat(Chat(
                "family_group",
                "Family Group 🏡",
                EncryptionUtils.encrypt("Dinner is ready! 🍕", groupKey),
                System.currentTimeMillis() - 1000 * 60 * 5, // 5 mins ago
                isGroup = true,
                isWifiP2p = false,
                e2eKey = groupKey
            ))

            // Seed Messages
            messageDao.insertMessage(DbMessage(
                chatId = "ai_assistant",
                senderId = "ai_assistant",
                senderName = "Devil AI Assistant",
                contentEncrypted = "Hi there! I am powered by Devil AI & Ollama. Ask me any question!",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
                isMine = false,
                isAiResponse = true,
                isEncrypted = false
            ))

            // Sarah Chat Message List (E2EE)
            messageDao.insertMessage(DbMessage(
                chatId = "sarah_a",
                senderId = "sarah_a",
                senderName = "Sarah Abbott",
                contentEncrypted = EncryptionUtils.encrypt("Hi! Good morning!", sarahKey),
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60,
                isMine = false,
                isEncrypted = true
            ))
            messageDao.insertMessage(DbMessage(
                chatId = "sarah_a",
                senderId = "me",
                senderName = "Me",
                contentEncrypted = EncryptionUtils.encrypt("Good morning Peter. How's it going?", sarahKey),
                timestamp = System.currentTimeMillis() - 1000 * 60 * 45,
                isMine = true,
                isEncrypted = true
            ))
            messageDao.insertMessage(DbMessage(
                chatId = "sarah_a",
                senderId = "sarah_a",
                senderName = "Sarah Abbott",
                contentEncrypted = EncryptionUtils.encrypt("Hey! Are you coming to the park?", sarahKey),
                timestamp = System.currentTimeMillis() - 1000 * 60 * 30,
                isMine = false,
                isEncrypted = true
            ))

            // Group Chat Messages
            messageDao.insertMessage(DbMessage(
                chatId = "family_group",
                senderId = "mom",
                senderName = "Mom",
                contentEncrypted = EncryptionUtils.encrypt("Is everyone coming home for dinner?", groupKey),
                timestamp = System.currentTimeMillis() - 1000 * 60 * 15,
                isMine = false,
                isEncrypted = true
            ))
            messageDao.insertMessage(DbMessage(
                chatId = "family_group",
                senderId = "me",
                senderName = "Me",
                contentEncrypted = EncryptionUtils.encrypt("Yes, I'll be there in 15 mins!", groupKey),
                timestamp = System.currentTimeMillis() - 1000 * 60 * 10,
                isMine = true,
                isEncrypted = true
            ))
            messageDao.insertMessage(DbMessage(
                chatId = "family_group",
                senderId = "mom",
                senderName = "Mom",
                contentEncrypted = EncryptionUtils.encrypt("Excellent, dinner is ready! 🍕", groupKey),
                timestamp = System.currentTimeMillis() - 1000 * 60 * 5,
                isMine = false,
                isEncrypted = true
            ))
        }
    }
}
