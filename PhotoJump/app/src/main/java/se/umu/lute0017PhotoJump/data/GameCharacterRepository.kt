package se.umu.lute0017PhotoJump.data

import androidx.lifecycle.LiveData

class GameCharacterRepository(private val gameCharacterDao: GameCharacterDao) {

    val readAllCharacters: LiveData<List<GameCharacter>> = gameCharacterDao.readAllGameCharacters()

    suspend fun addCharacter(gameCharacter: GameCharacter){
        gameCharacterDao.addCharacter(gameCharacter)
    }

    suspend fun deleteCharacter(gameCharacter: GameCharacter){
        gameCharacterDao.deleteCharacter(gameCharacter)
    }

    suspend fun updateCharacter(gameCharacter: GameCharacter) {
        gameCharacterDao.updateCharacter(gameCharacter)
    }

}