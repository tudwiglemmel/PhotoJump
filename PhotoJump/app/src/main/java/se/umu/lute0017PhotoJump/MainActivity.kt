package se.umu.lute0017PhotoJump

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import se.umu.lute0017PhotoJump.data.GameCharacterViewModel
import se.umu.lute0017PhotoJump.ui.theme.PhotoJumpTheme

// Ärver från ComponentActivity och följer SensorEventListener interfacet.
class MainActivity : ComponentActivity(), SensorEventListener {
    private val viewModel: GameCharacterViewModel by viewModels()
    //Avänds för room database apiet

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    //tilldelas senare när sensorn kopplas upp

    private var xValue = mutableStateOf(0f)
    // Värdet som används för spelet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // Börja koppla upp sensorn

        enableEdgeToEdge()
        setContent {
            PhotoJumpTheme {
                MainScreen(xValue.value, viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Koppla upp lyssnaren
        // Kolla om det ens finns an accelerometer i enheten.
        // Annars kan det bli en nullPointerException
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        // Lyssa inte för att spara batteri när appen pausas
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?){
        // Event är nullable. Kolla om event inte är null.
        // Och om det är rätt typ av sensor innan det tilldelas
        if (event != null && event.sensor != null) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                //Uppdateras vare gång användaren rör på telefonen
                xValue.value = event.values[0]
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //Behövs inte i detta fal
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(xValueValue: Float, viewModel: GameCharacterViewModel) {
    val navController = rememberNavController()
    //NavControllern som har hand om navigationen, rememberNavController gör att den sparas
    //Håller koll på vilken skärm användaren är på och backstacken
    //Används av navHost och navBar för att visa rätt skärm och kolla
    //vilken skärm användaren är på just nu

    val screens = listOf(Screen.Camera, Screen.Characters, Screen.Game)
    // Alla screen composables som finns i hela appen
    // Sparas i Screen filen för att ha allt på samma ställe och göra mainActivity enklare

    var selectedCharacterUri by rememberSaveable { mutableStateOf<String?>(null) }
    //Pekar till den valda karaktärens fil
    //Sätts genom state hoisting från CharacterScreen
    //Nullable eftersom ingen karaktär behöver vara vald
    //RememberSaveable eftersom rotationer är configuration changes och inte recompositions
    //Därmed sparas inte valet när mobilen roteras om det inte är saveable
    //Initial värde är null eftersom igen karaktär väljs från början

    Scaffold(
        bottomBar = {
            //Navbaren som låter användaren navigera genom appen.
            //Befinner sig längst nere på skärmen och är alltid synlig

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            //Används för att få rätt ikon i navbaren att bli markerad. Det visar för
            //användaren vart den är just nu
            //Får alltid nyaste värdet genom by delegering från navControllern

            val currentRoute = navBackStackEntry?.destination?.route
            //Tar ut strängen där användaren är just nu ur navBackStackEntry
            //Nullable för att inte krascha innan compose är helt initialized

            NavigationBar {
                //Skapa en knapp för varje skärm. Smidigt eftersom fler skärmar
                //innebär inga större förändringar här
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        //Visar vilken knapp i navbaren användaren är på just nu
                        //genom att jämföra skärmens route med den användaren är på nu
                        onClick = {
                            //Säger åt navcontrollern vart den ska när knappen trycks
                            navController.navigate(screen.route) {
                                // Om användaren klickar på en navbar ikon töms stacken
                                // annars hade bakåtknappen hoppat ner i applikationen
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                    //Sparar data från skärmen när den stängs så att det kan
                                    //återskapas när användaren återvänder
                                }
                                launchSingleTop = true
                                // Låter inte flera instanser av samma skärm skapas
                                //Om användarne är på game-skärmen och trycker på den igen
                                //händer ingenting
                                restoreState = true
                                //Återställer data när användaren återvänder till skärmar
                            }
                        }
                    )
                }
            }
        },
        content = { innerPadding: PaddingValues ->
            //Själva innehållet på skärmen, som camera, character och game skärmen
            //Befinner sig ovanför navbaren på skärmen. Padding säkerställer att den inte
            //överlappar med navbaren
            NavHost(
                navController = navController,
                startDestination = Screen.Characters.route,
                modifier = Modifier,
                //Parametrar som navhost behöver för att kunna fungera som navcontrollern
                //Skickar inte in padding här eftersom alla paddingen fungerar inte som tänkt
                //med alla skärmar. Istället skickas den individuellt till den som behöver den.
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(500)
                    ) + fadeIn(animationSpec = tween(500))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(500)
                    ) + fadeOut(animationSpec = tween(500))
                }
                //Egna animiationer
                //Dessa gör att spelet pausas tidigare och därmed att bollen inte
                //förlyttar sig för mycket själv. Ser även coolare ut än standardanimationerna
            ) {
                composable(Screen.Camera.route) {
                    //berättar vad som ska visas när en viss route väljs i navbaren
                    //Routen, ex "characters" kollas upp här och det som är inom måsvingarna visas
                    //Skickar in parametrar som skärmarna behöver
                    //Varje composable() står för en route och en skärm, riktigt smidigt för navigering
                    CameraScreen(
                        navController = navController,
                        context = LocalContext.current,
                        viewModel = viewModel
                    )
                    //navControllern används för att hoppa tillbaka om användaren avbryter handlingen
                    //context används för att spara bilder
                }
                composable(Screen.Characters.route) {
                    CharactersScreen(
                        modifier = Modifier.padding(innerPadding),
                        selectedCharacterImageUri = selectedCharacterUri,
                        //Variabeln skickas ner i funktionen
                        onCharacterSelected = { newUri ->
                            selectedCharacterUri = newUri
                        },
                        //State hoisting. För att skicka vilken karaktär som valdes till gamescreen
                        //skickas den upp hit och sedan ner i gamescreen.
                        //Detta görs genom en callback function. När en karaktär väljs skickar funktionen
                        //i characterscreen tillbaka upp hit vad som valdes genom ett funktionsanrop.
                        //newUri matas in i characterScreen och här tilldelas selectedCharacterUri dess värde
                        //Då sker en recomposition och gamescreen får det nya värdet
                        viewModel = viewModel
                        //Databasen för att ta bort och lägga till karaktärer
                    )
                }
                composable(Screen.Game.route) {
                    GameScreen(
                        xValueValue = xValueValue,
                        selectedCharacterUri = selectedCharacterUri,
                        bottomPadding = innerPadding.calculateBottomPadding()
                        //För att inte skapa platformar inom navbaren
                    )
                    //Vald gubbe skickas ner i gamescreen
                }
            }
        }

    )
}



