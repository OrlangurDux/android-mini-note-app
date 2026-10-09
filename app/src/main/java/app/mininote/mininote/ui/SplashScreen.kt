package app.mininote.mininote.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import app.mininote.mininote.R

/** Полноэкранный арт из MiniNoteAppPack/Splash; ориентация/планшет выбираются ресурсными квалификаторами splash_art. */
@Composable
fun SplashScreen() {
    Image(
        painter = painterResource(R.drawable.splash_art),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
}
