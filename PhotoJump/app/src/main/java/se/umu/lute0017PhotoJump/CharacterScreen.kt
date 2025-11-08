package se.umu.lute0017PhotoJump

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import se.umu.lute0017PhotoJump.data.GameCharacter
import se.umu.lute0017PhotoJump.data.GameCharacterViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(
    modifier: Modifier = Modifier,
    selectedCharacterImageUri: String?,
    //Valda karaktären. Är nullable eftersom ingen character kommer vara vald från början
    onCharacterSelected: (String?) -> Unit,
    //Lambda funktionen som skickar tillbaka upp vilken spelare som valdes.
    //Även nullable eftersom användaren kan välja ingen spelare
    viewModel: GameCharacterViewModel
    //Databasen
)  {
    val characters by viewModel.readAllCharacters.observeAsState(initial = emptyList())
    //Listan av alla karaktärer. Fylls på direkt från vår room databas.
    //Blir automatiskt sorterade genom databasen.
    //När ändringar i databasen sker uppdateras characters automatiskt.
    //Single source of truth för karaktärerna.
    //Senare filtreras dom men denna lista redigeras aldrig
    val context = LocalContext.current

    //Används för att filtrera characters
    var searchQuery by rememberSaveable { mutableStateOf("") }

    //skapar en ny lista med de filtrerade karaktärerna
    val filteredCharacters = remember(searchQuery, characters) {
        if (searchQuery.isBlank()) {
            characters
        } else {
            characters.filter { character ->
                character.name.contains(searchQuery, ignoreCase = true)
            }
            //.filter går igenom alla element i characters.
            // character -> är som iteratorn i en for loop.
            //sen körs .contains på stringen som kollar om den innehåller searchQuery
        }
    }
    //remember är praktiskt eftersom den inte körs varje gång en recomposition sker
    //istället körs den bara när värdet av searchQuery eller characters ändras.
    //det gör det mer prestandavänligt än att filtrera varje gång det sker en recomposition

    //Används för att avmarkera sökfältet och stänga tangentbordet efter sökning
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = { newQuery ->
                        searchQuery = newQuery
                    },
                    //när en ny sökquery matas in sätter vi vår egen query till dess värde
                    //det gör att remember() märker att värdet har ändras och kör en ny filtrering
                    onSearch = {
                        //Direkt när användaren skriver in någonting sker filtreringen och därmed
                        //själva sökandet. Därför ska sökknappen bara stänga ner sökandet i vårat fall
                        //och avsluta hela sökprocessen
                        keyboardController?.hide()
                        //stänger tangetbordet eftersom sökningen avslutas med sökknappen
                        focusManager.clearFocus()
                        //Tar bort cursorn från inmatningsfältet. Ska inte va där om inget tangetbort finns
                        //eftersom det kan vara förvirrande för användaren

                        //Kanske inte bästa sättet att göra detta på. Att ta tag i flera systemgrejer och
                        //använda sökfältet som det egentligen inte ska användas. Men fungerar utmärkt för
                        //denna enklare applikation. Annars hade kanske ett OutlinedTextField fungerat bra.
                    },
                    placeholder = { Text("Search by name...") },
                    expanded = false,
                    onExpandedChange = {},
                    trailingIcon = {
                        //om någonting har matats in visas en knapp för att snabbt tömma sökfältet
                        if(searchQuery != "") {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Edit"
                                )
                            }
                        }
                    }
                )
            },
            expanded = false,
            onExpandedChange = {
                //Vi gör ingenting när användaren klickar på sökfältet
                //Genom detta agerar sökfältet som ett enkelt inmatningsfönster
            }
        ) {
            //behövs inte i vårat enkla fall
            //visar annars saker som sökresultat
            //men våra sökresultat består av de filtrerade karaktärerna nedan
        }


        if (filteredCharacters.isEmpty()) {
            //Om listan karaktärer är tom visas ett meddelande istället.
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "No characters found.\nGo to the Camera tab to create one!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                //LazyColumn gör det enkelt och prestandaefficient att ha många element i en lista
                modifier = Modifier.weight(1f)
                //weight gör att den tar upp all plats som finns kvar efter navbaren har composats
            ) {
                //Detta sker för varje karaktär som vi får från databasen
                items(filteredCharacters) { gameCharacter ->
                    CharacterCard(
                        gameCharacter = gameCharacter,
                        isSelected = selectedCharacterImageUri == gameCharacter.imagePath,
                        //Kollar om karaktären är selected genom att jämföra paths
                        onSelectedChange = { isSelected ->
                            //isSelected skickas upp från characterCard via onSelectedChange
                            //värdet ändras inne i characterCard
                            //Beroende på dess värde ändras vad som sker när switchen i
                            //characterCard trycks.
                            if (isSelected) {
                                //Om switchen är checked
                                onCharacterSelected(gameCharacter.imagePath)
                                //Använder state hoisting funktionen för att skicka uppåt vilken
                                //karaktär som valdes
                            } else {
                                onCharacterSelected(null)
                                //Om karaktären avmarkerades skickas att ingen är markerad uppåt
                            }
                        },
                        onDelete = {
                            if (selectedCharacterImageUri == gameCharacter.imagePath) {
                                onCharacterSelected(null)
                            }
                            //Meddela att ingen karaktär är markerad om den markerade tas bort
                            deleteCharacterAndImage(gameCharacter, context, viewModel)
                            //Allt in en funktion för enklare kod
                        },
                        onEdit = { updatedCharacter ->
                            viewModel.updateCharacter(updatedCharacter)
                        }
                        //Tar in den redigerade gubben och sparar den
                        //updateCharacter är en @update funktion
                        //fördelen med detta är att skulle fler saker läggas till karaktärerna
                        //ex: storlek. Kräver detta ingen ny updateCharacter funktion.

                    )
                }
            }
        }
    }
}

@Composable
fun CharacterCard(
    gameCharacter: GameCharacter,
    isSelected: Boolean = false,
    onSelectedChange: (Boolean) -> Unit,
    //State hoisting för att skicka till characterScreen om karaktären är vald eller ej
    //Beroende på värdet isSelected har ändras då vad som händer när switch ändrar sitt värde
    onDelete: () -> Unit,
    //Även en lambda funktion eftersom vad som tas bort är specifikt till varje characterCard
    //Därför kan inte en enda delete funktion användas.
    //Om man hade velat göra så hade character behövt skickas ner hit också
    onEdit: (GameCharacter) -> Unit
    //Skickar tillbaka uppåt den nya redigerade karaktären

) {
    //För att visa ett dialogfönster
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }

    var name = gameCharacter.name

    var newName by rememberSaveable(name) { mutableStateOf(name) }
    //rememberSaveable(name) betyder att värdet återställs varje gång name ändrar sitt värde
    //genom det kommer det redigerade namnet inte vara samma för olika karaktärer
    //annars hade newName återanvänts och två olika karaktärer hade kunnat haft samma
    //placeholder name i redigeringsvyn
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row (
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit"
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Switch(
                        checked = isSelected,
                        //isSelected bestämmer om switch är markerad eller avmarkerad
                        onCheckedChange = onSelectedChange
                        //När värdet ändras skickas det tillbaka upp till characterscreen
                    )

                    IconButton(onClick = { showDeleteDialog = true }) {
                        //När delete knappen trycks visas dialogen
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    if (showDeleteDialog) {
                        //Visas eftersom showDialog värdeändring utlöser en recomposition
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            title = { Text(text = "Delete Character") },
                            text = { Text("Are you sure you want to delete $name?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        onDelete()
                                        showDeleteDialog = false
                                    }
                                ) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showDeleteDialog = false }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    if (showRenameDialog){
                        AlertDialog(
                            icon = {Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit"
                            )},
                            onDismissRequest = { showRenameDialog = false },
                            title = { Text(text = "Rename Character") },
                            text = {
                                OutlinedTextField(
                                    value = newName,
                                    onValueChange = { newName = it },
                                    //När värdet ändras tar vi det som står i inputten (it)
                                    //och sparar det i newName
                                    label = { Text("New Name") },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (newName.isNotBlank()) {
                                            //id och imagePath är samma om man kopierar
                                            //bara namnet ändras till det nya.
                                            //det använder @update funktionen för att skriva över
                                            //värden på en karaktär som redan finns.
                                            val updatedCharacter = gameCharacter.copy(name = newName)
                                            onEdit(updatedCharacter)
                                            //den redigerade karaktären skickas uppåt
                                            //där skickas den till databasen och sparas
                                            showRenameDialog = false
                                        } else {
                                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text("Confirm")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showRenameDialog = false }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                }
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                    //Visa som cirkel eftersom karaktären i spelet kommer vara en cirkel
                contentAlignment = Alignment.Center
            ) {
                Image(
                    //Hämta bilden från url som matades med coil
                    //remember gör att den inte laddas om vid varje recomposition
                    //detta är bra för prestandan
                    painter = rememberAsyncImagePainter(model = File(gameCharacter.imagePath)),
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

private fun deleteCharacterAndImage(
    gameCharacter: GameCharacter,
    context: Context,
    viewModel: GameCharacterViewModel
){
    try {
        //Ta bort själva bilden
        val file = File(gameCharacter.imagePath)
        if (file.exists()) {
            file.delete()
        }
        else{
            throw Exception("Image does not exist")
        }

        var deletedName = gameCharacter.name
        viewModel.deleteCharacter(gameCharacter)
        //Ta bort karaktären ur databasen
        Toast.makeText(context, "'${deletedName}' deleted", Toast.LENGTH_SHORT).show()
    }
    catch (e: Exception){
        Toast.makeText(
            context,
            "Error deleting character and/or image: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
    //Try-catch så att antingen båda eller ingen tas bort.
    //För att undvika lösa bilder eller databasinlägg som inte har korresponderade data kopplat
}