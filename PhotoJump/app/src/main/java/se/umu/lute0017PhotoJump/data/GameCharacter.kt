package se.umu.lute0017PhotoJump.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gameCharacter_table")
data class GameCharacter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val imagePath: String
)