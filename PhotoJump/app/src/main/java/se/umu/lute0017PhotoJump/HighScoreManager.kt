package se.umu.lute0017PhotoJump

import android.content.Context

//Hanterar hämtning och sparande av highscore
object HighScoreManager {
    fun saveHighScore(context: Context, score: Float) {
        //Använder context för att spara highscore på rätt ställe
        //Preferences är enkla key value pairs som sparas.
        val prefs = context.getSharedPreferences("DoodleJumpPrefs", Context.MODE_PRIVATE)
        //prefs.edit() för att ändra värden
        val editor = prefs.edit()
        editor.putFloat("high_score", score)
        editor.apply()
    }

    fun loadHighScore(context: Context): Float {
        val prefs = context.getSharedPreferences("DoodleJumpPrefs", Context.MODE_PRIVATE)
        return prefs.getFloat("high_score", 0f)
        //Om det inte finns någon highscore lagrad returna 0
        //Exempelvis vid en ny installation
    }
}