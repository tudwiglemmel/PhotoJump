package se.umu.lute0017PhotoJump

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import se.umu.lute0017PhotoJump.data.GameCharacterViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import se.umu.lute0017PhotoJump.data.GameCharacter

@Composable
fun CameraScreen(navController: NavController, context: Context, viewModel: GameCharacterViewModel) {
    var imageBitmap by rememberSaveable { mutableStateOf<Bitmap?>(null) }
    var characterName by rememberSaveable { mutableStateOf("") }
    var nameIsError by rememberSaveable { mutableStateOf(false) }
    //Alla variabler är rememberSaveable så att de överlever configuration changes
    //imageBitmap börjar som null eftersom inget värde har satts än.
    //det är nullable eftersom det används för att bestämma vilken skärm som ska visas
    //Om det är null visas camera knappen
    //Om det har ett värde har en bild tagits och användaren ska namnge karaktären istället

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
        //Använder preview och får bitmap eftersom gubben ändå kommer bara vara lågupplöst.
        //Onödigt att spara en högupplösta versionen då.
        //Bilden ska inte sparas innan användaren namnger gubben och trycker på spara
        //Genom att spara bilden i en preview kan den bara sättas till null om användaren
        //skulle navigera bort. Smidigare än att temporärt spara bilden och sedan behöva ta
        //bort den.
    ) { capturedBitmap ->
        if (capturedBitmap != null) {
            imageBitmap = capturedBitmap
        }
    }
    //Sista parametern av rememberLauncherForActivityResult är en lambda.
    //Därför kan man skriva den med {} utanför () där man vanligtvis har parametrar.
    //Där definieras vad som ska ske när en bild har tagits.
    //I vårt fall kollar vi att bilden inte är null, dvs. att användaren inte avbröt handlingen
    //Och sedan tilldelas vår bitmap denna bild

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap == null) {
            InitialView(onOpenCameraClick = { cameraLauncher.launch(null) })
            //Visa första vyn om ingen bild har tagits än
            //Skickar med cameraLauncher så att vi kan koppla upp "Open Camera" knappen därinne
            //Flyttade ut composable för att göra koden enklare och mer läsbart.
        } else{
            //Om en bild har tagits kommer vi hit och ska namnge och spara den
            //Hela denna kod hade även kunnat flyttas ut i en separat composable men detta är enklare
            //Alla värden som den composable hade behövt från CameraScreen hade gjort det lite krångligt
            //Används även bara en enda gång så inte super illa att ha det inline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Name Your Character",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                // Visa bilden
                Image(
                    bitmap = imageBitmap!!.asImageBitmap(),
                    //imageBitmap!! eftersom det är garanterat inte null här
                    //!! konverterar det nullable värdet till ett som är icke-nullable
                    contentDescription = "Character Preview",
                    modifier = Modifier
                        .size(200.dp)
                        .padding(8.dp)
                )
                OutlinedTextField(
                    // Här matar användaren in karaktärens namn
                    value = characterName,
                    onValueChange = {
                        characterName = it
                        //it är värdet användaren har matat in i textfönstret.
                        nameIsError = false
                        //När användaren har matat in ett namn kan spelaren sparas
                    },
                    label = { Text("Character Name") },
                    singleLine = true,
                    isError = nameIsError,
                    supportingText = {
                        if (nameIsError) {
                            Text("Name cannot be empty")
                        }
                        //Visar bara supportingText när nameIsError är true.
                        //Visas inte i början eftersom värdet först kan bli true när "Save" trycks
                        //Om fältet när knappen trycks är tom blir nameIsError true, en recomposition sker och
                        //supportingText visas.
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        imageBitmap = null
                        characterName = ""
                        nameIsError = false
                        //Återställer allting
                        //Hoppar även tillbaka till första skärmen eftersom imageBitmap är null nu
                        //då blir första if-satsen true och skärmen med "open camera" knappen visas
                        //Låter användaren ta en ny bild. navController.popBackStack() hade kunnat användas
                        //Men då hade användaren skickats till skärmen de var på förut och inte varit kvar på
                        //cameraScreen.
                    }) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        if (characterName.isBlank()) {
                            //Kolla om användaren har matat in något
                            nameIsError = true
                        } else {
                            //Skapa ett random namn för bilden
                            val imageFileName = "${UUID.randomUUID()}.png"
                            val imageUri = saveBitmapToFile(context, imageBitmap!!, imageFileName)
                            //returvärdet används både för att koppla rätt bild till karaktären
                            //men även för att kolla om det gick att spara bilden
                            //Om bilden inte kunde sparas avbryts hela sparandet.

                            if (imageUri != null) {
                                //Bara spara karaktären om bilen kunde sparas
                                val character = GameCharacter(name = characterName.trim(), imagePath = imageUri)
                                viewModel.addCharacter(character)
                                //Sparas med room databasen
                                Toast.makeText(context, "Character '$characterName' saved!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                                //Gå tillbaka till karaktärskärmen när allt är klar
                            } else {
                                Toast.makeText(context, "Failed to save character image", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text("Save")
                    }

                }
            }

        }
    }
}

@Composable
private fun InitialView(onOpenCameraClick: () -> Unit) {
    //Första skärmen som visas när användaren öppnar CameraScreen
    //Tar in en funktion som kopplas till "Open Camera" knappen för att starta kameran
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Create Character",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Take a picture to create a custom character",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Button(onClick = onOpenCameraClick) {
            //Här används funktionen som skickas in som parameter
            Text("Open Camera")
        }
    }
}

//Sparar bilden med namnet användaren matade in
private fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): String? {
    val picturesDir = context.getExternalFilesDir(null)
    //Använder context för att hämta de externa filerna

    if (picturesDir == null) {
        Toast.makeText(context, "Unable to access storage", Toast.LENGTH_LONG).show()
        return null
    }
    //Om det inte gick att komma åt lagringen

    val file = File(picturesDir, fileName)
    //Skapar den tomma filen först innan den innehåller något
    //Skapas i externa privata utrymmet för applikationen och nämns efter det givna filnamnet

    try {
        FileOutputStream(file).use { out ->
            //.use öppnar och stänger filen inom dessa {}
            //smidigt eftersom om det blir något fel med operationen kommer filen stängas ändå
            //är som with open i python

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            //Konvertera bitmap till en png med 100 procent kvalitet.
            //Skrivs till fil via filströmmen "out" som går till filen vi skapade innan
        }

        return file.absolutePath
        //Skickas tillbaka så att vi kan koppla bilden till inlägget i databasen

    } catch (e: Exception) {
        Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_LONG).show()
        // Om det uppstår något fel sparas inte karaktären i databasen eftersom null returns
        return null
    }
    //Det kan uppstå fel när filer sparas, ex: ingen lagring kvar i enheten eller ogiltigt namn
    //därför try-catch blocket
}