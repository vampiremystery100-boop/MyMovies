package com.example.mymovies

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

private val categories = listOf(
    "Horror",
    "Comedy",
    "Sci-Fi",
    "Action",
    "Thriller",
    "Web Series"
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MyMoviesApp()
            }
        }
    }
}

@Composable
fun MyMoviesApp() {

    val context = LocalContext.current

    var selectedCategory by remember {
        mutableStateOf("Horror")
    }

    var playingFile by remember {
        mutableStateOf<File?>(null)
    }

    var refresh by remember {
        mutableIntStateOf(0)
    }

    val movieFiles = remember(selectedCategory, refresh) {
        val folder = File(
            context.filesDir,
            "movies/$selectedCategory"
        )

        folder.listFiles()
            ?.filter { it.isFile }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri != null) {

            val folder = File(
                context.filesDir,
                "movies/$selectedCategory"
            )

            if (!folder.exists()) {
                folder.mkdirs()
            }

            val originalName = getFileName(context, uri)
                ?: "Movie_${System.currentTimeMillis()}.mp4"

            val cleanName = originalName
                .replace(Regex("[^A-Za-z0-9._ -]"), "_")

            var destination = File(folder, cleanName)

            if (destination.exists()) {
                destination = File(
                    folder,
                    "${System.currentTimeMillis()}_$cleanName"
                )
            }

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->

                    destination.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                refresh++

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (playingFile != null) {

        VideoPlayerScreen(
            file = playingFile!!,
            onBack = {
                playingFile = null
            }
        )

    } else {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Text(
                text = "My Movies",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                items(categories) { category ->

                    if (category == selectedCategory) {

                        Button(
                            onClick = {
                                selectedCategory = category
                            }
                        ) {
                            Text(category)
                        }

                    } else {

                        OutlinedButton(
                            onClick = {
                                selectedCategory = category
                            }
                        ) {
                            Text(category)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    launcher.launch(arrayOf("video/*"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("＋ Add Video")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = selectedCategory,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (movieFiles.isEmpty()) {

                Text(
                    text = "No videos added yet."
                )

            } else {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    items(
                        movieFiles,
                        key = { it.absolutePath }
                    ) { file ->

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {

                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )

                                Row(
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {

                                    Button(
                                        onClick = {
                                            playingFile = file
                                        }
                                    ) {
                                        Text("▶ Play")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            file.delete()
                                            refresh++
                                        }
                                    ) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(
    file: File,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val player = remember(file) {

        ExoPlayer.Builder(context)
            .build()
            .apply {

                setMediaItem(
                    MediaItem.fromUri(
                        Uri.fromFile(file)
                    )
                )

                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) {

        onDispose {
            player.release()
        }
    }

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Button(
            onClick = onBack,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("← Back")
        }

        AndroidView(
            factory = { viewContext ->

                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                    PlayerView(viewContext).apply {
    this.player = player
    useController = true
                        
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
