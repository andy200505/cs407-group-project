package com.cs407.chatgplaylist.data

import android.content.Context
import androidx.room.*

@Entity(
    indices = [Index(
        value = ["userUID"], unique = true
    )]
)
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Int = 0, val userUID: String = ""
)

@Entity
data class Playlist(
    @PrimaryKey(autoGenerate = true) val playlistId: Int = 0,
    val userId: Int,
    val title: String
)

@Entity(
    foreignKeys = [ForeignKey(
        entity = Playlist::class,
        parentColumns = ["playlistId"],
        childColumns = ["playlistId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Song(
    @PrimaryKey(autoGenerate = true) val songId: Int = 0,
    val playlistId: Int,
    val title: String,
    val artist: String
)

data class PlaylistWithSongs(
    @Embedded val playlist: Playlist,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "playlistId"
    )
    val songs: List<Song>
)

@Dao
interface UserDao {
    @Query("SELECT * FROM User WHERE userUID = :uid")
    suspend fun getByUID(uid: String): User?

    @Insert
    suspend fun insert(user: User)
}

@Dao
interface PlaylistDao {
    @Transaction
    @Query("SELECT * FROM Playlist WHERE userId = :userId")
    suspend fun getPlaylistsWithSongs(userId: Int): List<PlaylistWithSongs>

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Insert
    suspend fun insertSongs(songs: List<Song>)

    @Transaction
    suspend fun insertPlaylistWithSongs(playlist: Playlist, songs: List<Song>): Long {
        val playlistId = insertPlaylist(playlist)
        insertSongs(songs.map { it.copy(playlistId = playlistId.toInt()) })
        return playlistId
    }
}

@Dao
interface DeleteDao {
    @Query("DELETE FROM Playlist WHERE userId = :userId")
    suspend fun deleteAllPlaylistsForUser(userId: Int)

    @Query("DELETE FROM User WHERE userId = :userId")
    suspend fun deleteUser(userId: Int)

    @Transaction
    suspend fun deleteUserAndPlaylists(userId: Int) {
        deleteAllPlaylistsForUser(userId)
        deleteUser(userId)
    }
}

@Database(entities = [User::class, Playlist::class, Song::class], version = 1)
abstract class PlaylistDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun deleteDao(): DeleteDao

    companion object {
        @Volatile
        private var INSTANCE: PlaylistDatabase? = null

        fun getDatabase(context: Context): PlaylistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlaylistDatabase::class.java,
                    "playlist_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}