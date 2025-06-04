package com.example.mutlabocsnotes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class NoteDatabase : RoomDatabase()  {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class NoteRepository(private val noteDao: NoteDao) {

    suspend fun getAllNotes(): List<Note> = noteDao.getAll()

    suspend fun insert(note: Note): Long = noteDao.insertNote(note)

    suspend fun update(note: Note): Int = noteDao.updateNote(note)

    suspend fun delete(note: Note): Int = noteDao.deleteNote(note)
}