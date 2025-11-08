package se.umu.lute0017PhotoJump.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameCharacterViewModel(application: Application): AndroidViewModel(application) {

    private val repository: GameCharacterRepository
    val readAllCharacters: LiveData<List<GameCharacter>>

    init {
        val characterDao = GameCharacterDatabase.getDatabase(application).characterDao()
        repository = GameCharacterRepository(characterDao)
        readAllCharacters = repository.readAllCharacters
    }

    fun addCharacter(gameCharacter: GameCharacter) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addCharacter(gameCharacter)

        }
    }

    fun deleteCharacter(gameCharacter: GameCharacter){
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCharacter(gameCharacter)
        }
    }

    fun updateCharacter(gameCharacter: GameCharacter) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCharacter(gameCharacter)
        }
    }

}