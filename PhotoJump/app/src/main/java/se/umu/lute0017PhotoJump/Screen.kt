package se.umu.lute0017PhotoJump

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Games
import androidx.compose.ui.graphics.vector.ImageVector

//Sealed gör att inga nya grejer kan läggas till sen. Bara sakerna som deklareras inne
//i klassen.
//alla object är instanser av screen klassen.
//Single source of truth för alla navigationsdestinationer
//Bättre att ha det separerat ut än att ha det inne i någon klass.
//Vanlig pattern för navigering i appar

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Characters : Screen("characters", "Characters", Icons.Filled.AccountBox)
    object Camera : Screen("camera", "Camera", Icons.Filled.CameraAlt)
    object Game : Screen("game", "Game", Icons.Filled.Games)
}