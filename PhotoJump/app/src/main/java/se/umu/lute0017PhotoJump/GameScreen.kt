package se.umu.lute0017PhotoJump

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.io.File
import kotlin.random.Random
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import android.os.Parcelable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.parcelize.Parcelize

val PLAYER_SIZE_DP = 30.dp
val PLATFORM_WIDTH_DP = 60.dp
val PLATFORM_HEIGHT_DP = 12.dp
var SCORE = 0f
const val GRAVITY = 800f
const val TILT_SENSITIVITY = 250f
const val JUMP_STRENGTH = -800f

@Composable
fun GameScreen(
    xValueValue: Float,
    //accelerometervärdet som matas in från MainActivity
    selectedCharacterUri: String?,
    //selectedCharacterUri är nullable för att användaren inte behöver välja en
    bottomPadding: Dp
    //Subtraheras från screenheight
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    //rememberSaveable sparar över config changes. Ex. om mobilen roteras
    var isPaused by rememberSaveable  { mutableStateOf(false) }

    val context = LocalContext.current
    var highScore by rememberSaveable { mutableStateOf(0f) }

    //Körs en gång för att hämta current highscore
    LaunchedEffect(Unit) {
        highScore = HighScoreManager.loadHighScore(context)
    }

    //Pausar spelet när användaren klickar på en annan composable
    //Hade kunnat gjorts med state hoisting men det här är enklare
    DisposableEffect(Unit) {
        onDispose {
            //När composablen inte längre är på skärmen körs detta
            isPaused = true
            //När isPaused ändrar värdet sker det en recomposition och skärmen som visas ändras
        }
    }

    // Behöver pixels för spelet, bland annat för att placera plattformar
    val bottomPaddingPx = with(density) { bottomPadding.toPx() }
    //storleken på navbaren konverteras till pixlar
    val totalScreenHeightPx = with(density) { screenHeight.toPx() }
    //den subtraheras från hela skärmstorleken
    val screenHeightPx = totalScreenHeightPx - bottomPaddingPx
    //Bara skärmen ovanför navbaren används nu. Det gör att inga platformar skapas
    //inom navbaren som användaren inte kan se.

    val screenWidthPx = with(density) { screenWidth.toPx() }

    //vanliga xValueValue som matas in från mainActivity recompositionar composablen när den ändras
    //Problemet är att detta inte sker inne i LaunchedEffect gameloopen.
    //Värdet ändras men det når inte in dit eftersom LaunchedEffect körs en gång när composablen skapas
    //inte varje gång det sker en recomposition.
    //genom rememberUpdatedState finns det alltid en referens till det nya värdet tillgängligt
    //Även innuti gameloopen
    val updatedXValue by rememberUpdatedState(newValue = xValueValue)

    //Skapa spelaren och platform-listan. Spara med rememberSaveable
    var character by rememberSaveable  { mutableStateOf(CharacterState(screenWidthPx, screenHeightPx, 0f, 0f, screenHeightPx, screenWidthPx)) }
    var platforms by rememberSaveable { mutableStateOf<List<PlatformState>>(emptyList()) }
    //Populera platforms listan
    platforms = managePlatforms(platforms, screenHeightPx, screenWidthPx)
    //managePlatforms fyller på listan tills plattformarna tar upp hela skärmen.
    //Därför kan den användas när listan är helt tom och även senare för att fylla på
    //med nya plattformar.

    LaunchedEffect(Unit) {
        //LaunchedEffect körs en gång när composable skapas
        //Mäter tidsåtgången mellan fpsen för att ha samma rörelse oavsett fps
        var lastFrameTimeNanos = System.nanoTime()
        //Själva gameloopen
        while (true) {
            val currentFrameTimeNanos = withFrameNanos { frameTime ->
                frameTime
            }
            //frametime används för att få deltaTime
            //deltaTime används för att få lika rörelse oavsett fpsen på skärmen användaren
            //spelar på. Storleken av rörelsen anpassas så att spelaren rör sig mindre mellan frames
            //när det är höga fps och mer när det är låga fps.
            //withFrameNanos tar in en lambda funktion
            //vi returnerar bara frameTime direkt i detta fall

            if (isPaused) {
                //När spelet är pausad återställs delta time
                //så att inte gubben hoppar enormt högt när användaren unpausar
                lastFrameTimeNanos = currentFrameTimeNanos
                //Hoppa över resten av koden i loopen
                //Hoppar därmed tillbaka hit till if(isPaused)
                //Inte värt att köra resten av koden om spelet ändå är pausat
                continue
            }

            //Beräkna tidsåtgången mellan framsen
            //det värdet används för att justera rörelsen till fpsen senare
            val deltaTime = (currentFrameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
            lastFrameTimeNanos = currentFrameTimeNanos
            //när tidsåtgången till förra framen har mätts återställs räknaren
            //då mäter den till nästa frame igen

            //Måste göra en kopia av karaktären för att få korrekt recomposition när dess värde ändras
            //Annars sker det inte alltid recompositions när spelaren flyttar på sig
            //Genom att senare skriva över hela character med newCharacter
            //Sker det garanterat en recomposition då
            var newCharacter = character.copy()
            //genom .copy() blir det en ny objekt referens. Compose märker när objekt skrivs över
            //med nya referenser och gör en recomposition

            // Här används deltaTime för att justera spelarens rörelse till fpsen
            //det alltid nya och uppdaterade updatedXValue används här för att flytta spelaren
            //Beroende på värdet flyttas spelaren lite eller mycket
            newCharacter.update(deltaTime, updatedXValue)

            var platformOffset = 0f
            if (newCharacter.y < screenHeightPx / 2) {
                //Om spelaren är mer än halvvägs upp skärmen ska spelet "scrolla"
                //Genom det ser användaren alltid nya platformar och det blir bra flyt i spelet
                //Eftersom det mätts uppfrån och ner är spelaren mer än halvvägs upp skärmen om
                //dess y värde är mindre än hälften av skärmens höjd
                //Spelarens x och y värden utgår från högre vänstra hörnet
                platformOffset = (screenHeightPx / 2) - newCharacter.y
                newCharacter.y += platformOffset
                //"Scrollen" är bara platformarna som flyttas neråt.
                //När spelaren är mer än havlvägs upp skärmen kan den inte röra sig högre längre
                //Dess rörelse i y led nollställs genom platformOffset

                SCORE += platformOffset
                //Score är bara hur mycket spelaren har kommit över mitten av skärmen

                //Highscore logik
                if (SCORE > highScore) {
                    highScore = SCORE // Uppdatera värdet så det visas direkt. Triggrar en recomposition
                    HighScoreManager.saveHighScore(context, highScore)
                }
            }

            collisionChecker(newCharacter, platforms, density)
            //Skickar in en referens till newCharacter och redigerar den inne i funktion
            //Behöver tack vare detta inte returnera någonting

            //Måste kopiera över plattformarna från gamla platforms
            var updatedPlatforms = platforms.map { it.copy(y = it.y + platformOffset) }
            //Samtidigt flyttas dom neråt lika mycket som spelaren är ovanför mitten av skärmen
            //Loopar igenom varje platform och skapar en kopia. Alla värden förutom y är samma
            //Sedan redigeras kopian av platformen med passande offset och det sparas i updatePlatforms
            //Det skapar illusionen att spelaren åker uppåt

            //Fyller på med nya plattformar och tar bort dom gamla
            updatedPlatforms = managePlatforms(updatedPlatforms, screenHeightPx, screenWidthPx)

            //Genom att skriva över objekten sker det en recomposition
            character = newCharacter
            platforms = updatedPlatforms

            //Kolla om spelaren dog
            if(character.y > screenHeightPx){
                //Spelarens y är större än skärmens höjd. Det betyder att den är under skärmens kant
                //Då är karaktären död
                SCORE = 0f
                //Töm listan och populera med nya plattformar
                platforms = emptyList()
                platforms = managePlatforms(platforms, screenHeightPx, screenWidthPx)
                //Placera spelaren i mitten av skärmen
                character = CharacterState(
                    x = screenWidthPx / 2,
                    y = screenHeightPx / 2,
                    yVelocity = 0f,
                    xVelocity = 0f,
                    screenHeight = screenHeightPx,
                    screenWidth = screenWidthPx
                )
            }
        }
    }

    // Rita alla objekt
    //Här sker det många recompositions eftersom värden ändras ofta
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        //character innehåller positionen av karaktären
        MyCharacter(character, characterUri = selectedCharacterUri)

        //ritar alla plattfomar
        platforms.forEach { platform ->
            Platform(platform)
        }

        Text(
            text = "HIGH SCORE: ${highScore.toInt()}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        Text(
            text = "${SCORE.toInt()}",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        )

        IconButton(
            onClick = { isPaused = !isPaused },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) "Play" else "Pause",
                modifier = Modifier.fillMaxSize()
            )
        }

    }
}

//Skapar nya plattformar och tar bort dom gamla som har åkt under skärmen
fun managePlatforms(
    currentPlatforms: List<PlatformState>,
    screenHeightPx: Float,
    screenWidthPx: Float
): List<PlatformState> {
    //kommer hålla dom nya plattformarna
    val updatedPlatforms = mutableListOf<PlatformState>()

    //highestY används senare för att generera nya plattformar ovanför den högsta plattformen.
    //Om det finns inga än måste hela skärmen fyllas med plattformar, då är alla dessa ovanför
    //skärmens botten.
    var highestY = screenHeightPx

    //plattformarna som inte är på skärmen längre kopieras inte till updatedPlatforms
    for (platform in currentPlatforms) {
        if (platform.y < screenHeightPx) {
            //Kopiera över platformarna som fortfarande finns på skärmen till nya listan
            updatedPlatforms.add(platform)

            //Här hittar vi platformen som är längst upp på skärmen
            //Om det finns plattformar hittas den högsta här
            if (platform.y < highestY) {
                highestY = platform.y
            }
        }
    }

    //highestY kommer minskas efter varje plattform har lagts till eftersom värdet utgår uppifrån
    while (highestY > 0) {
        val randomX = Random.nextFloat() * screenWidthPx
        val nextY = highestY - (Random.nextFloat() * 150f + 80f)
        //placera plattformen ett slumpmässigt avstånd inom ett intervall ovanför högsta plattformen
        //vi får en siffra mellon 0 och 1 och skalar det upp för att placera plattformen
        //variationen bestämmer avståndet mellan plattformarna
        //den blir minst 0f+80f högre och max 150f+80f högre än förra

        updatedPlatforms.add(PlatformState(x = randomX, y = nextY))

        //Genom detta kommer nästa plattform att skapas ovanför sista vi la till
        //förra plattformen blir den nya högsta
        highestY = nextY
    }
    //loopen körs tills hela skärmen är fylld med plattformar

    return updatedPlatforms
}

fun collisionChecker(player: CharacterState, platforms: List<PlatformState>, density: Density) {
    platforms.forEach { platform ->
        val platformWidthPx = with(density) { PLATFORM_WIDTH_DP.toPx() }
        val platformHeightPx = with(density) { PLATFORM_HEIGHT_DP.toPx() }
        val playerSizePx = with(density) { PLAYER_SIZE_DP.toPx() }
        //Måste konvertera dp till pixels för att använda dom för att kolla kollisionen
        //Eftersom faktiska pixlarna varierar på enheter baserat på upplösningen och skärmstorlek
        //görs detta med density. Density vet densiteten av skärmen
        //med with() syntaxen får alla värden inom måsvingarna contexten av det inom parenteserna
        //toPx() är specifikt till density. Genom with() syntaxen kan den enkelt användas

        if (player.yVelocity > 0){
            //Större än noll eftersom spelaren faller när yVelocity är mer än noll
            //Eftersom det utgår uppifrån
            //Viktigt eftersom karaktären annars studsar direkt när den kolliderar med en platform
            //Då uppnår den för höga hastigheter
            if(player.y + playerSizePx >= platform.y && player.y + playerSizePx <= platform.y + platformHeightPx){
                //kollision i y-led
                if ((player.x + (playerSizePx / 2) )> platform.x - platformWidthPx / 2 && (player.x - (playerSizePx / 2)) < platform.x + platformWidthPx / 2){
                    //kollision i x-led
                    //Kollar om karaktären har landat på en plattform. Om ja studsar den uppåt
                    player.yVelocity = JUMP_STRENGTH
                }
            }
        }

    }
}

//Måste vara parcelizeable för att kunna spara med rememberSaveable
@Parcelize
data class CharacterState(
    var x: Float,
    var y: Float,
    var yVelocity: Float = 0f,
    var xVelocity: Float = 0f,
    var screenHeight: Float,
    var screenWidth: Float
) : Parcelable {
    fun update(deltaTime: Float, accelX: Float) {
        //Graden gravitationen och accelerometervärdet påverkar karaktären beror på deltaTime
        //deltaTime faktoriseras in här
        yVelocity += GRAVITY * deltaTime
        y += yVelocity * deltaTime

        //Negativt värde för att flippa inputten. Annars blir vänster till höger
        xVelocity = TILT_SENSITIVITY * -accelX
        x += xVelocity * deltaTime

        //För screen-wrapping
        if (x < 0f){
            x = screenWidth
        }
        if (x > screenWidth){
            x = 0f
        }

    }
}

@Composable
fun MyCharacter(
    state: CharacterState,
    characterUri: String?
) {
    val density = LocalDensity.current
    val offsetX = with(density){
        state.x.toDp() - (PLAYER_SIZE_DP / 2)
        //minus PLAYER_SIZE_DP för att få x att sitta precis i mitten av spelaren
    }
    val offsetY = with(density){
        state.y.toDp()
    }
    //Tar density som context för att konvertera till dp
    //Eftersom dp används för att faktiskt placera karaktären på skärmen

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            //offset används för att placera karaktären på skärmen
            //Det utgår från övre vänstra hörnet
            .size(PLAYER_SIZE_DP)
            .clip(CircleShape)
        //Croppar spelaren till att vara en boll
    ) {
        if (characterUri != null) {
            //Om en karaktär/bild har valts
            Image(
                //Hämta spelarens ikon från fil med coil
                painter = rememberAsyncImagePainter(model = File(characterUri)),
                contentDescription = "Player Character",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            //Annars visas en enkel röd boll istället
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red)
            )
        }
    }
}

@Parcelize
data class PlatformState(
    val x: Float,
    val y: Float,
): Parcelable

@Composable
fun Platform(state: PlatformState) {
    val density = LocalDensity.current
    with(density) {
        Box(
            modifier = Modifier
                .offset(x = state.x.toDp() - (PLATFORM_WIDTH_DP / 2), y = state.y.toDp())
                .size(width = PLATFORM_WIDTH_DP, height = PLATFORM_HEIGHT_DP)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1EA922))
        )
    }
    //Använder density för att konvertera pixlarna i state till dp
    //då kan de användas för att placera plattformen på skärmen
}