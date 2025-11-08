package se.umu.lute0017PhotoJump.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface GameCharacterDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCharacter(gameCharacter: GameCharacter)

    @Query("SELECT * FROM gameCharacter_table ORDER BY id ASC")
    fun readAllGameCharacters() : LiveData<List<GameCharacter>>

    @Delete
    suspend fun deleteCharacter(gameCharacter: GameCharacter)

    @Update
    suspend fun updateCharacter(gameCharacter: GameCharacter)
}