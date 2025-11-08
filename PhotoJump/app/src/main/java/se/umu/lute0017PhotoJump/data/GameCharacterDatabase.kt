package se.umu.lute0017PhotoJump.data

import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room

@Database(entities = [GameCharacter::class], version = 1, exportSchema = false)
abstract class GameCharacterDatabase: RoomDatabase() {

    abstract fun characterDao(): GameCharacterDao

    companion object{
        @Volatile
        private var INSTANCE: GameCharacterDatabase? = null

        fun getDatabase(context: Context): GameCharacterDatabase{
            val tempInstance = INSTANCE
            if (tempInstance != null){
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameCharacterDatabase::class.java,
                    "character_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }

}