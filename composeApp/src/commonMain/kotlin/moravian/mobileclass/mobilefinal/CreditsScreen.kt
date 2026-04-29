package moravian.mobileclass.mobilefinal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mobilefinal.composeapp.generated.resources.Res
import mobilefinal.composeapp.generated.resources.credits_member_braden
import mobilefinal.composeapp.generated.resources.credits_member_finn
import mobilefinal.composeapp.generated.resources.credits_source_button
import mobilefinal.composeapp.generated.resources.credits_source_note
import mobilefinal.composeapp.generated.resources.credits_source_url
import mobilefinal.composeapp.generated.resources.credits_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreditsScreen() {
    val uriHandler = LocalUriHandler.current
    val sourceUrl = stringResource(Res.string.credits_source_url)

    Column(
        modifier = Modifier.fillMaxSize().safeContentPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.credits_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(Res.string.credits_member_braden),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.credits_member_finn),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.credits_source_note),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = sourceUrl,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = { uriHandler.openUri(sourceUrl) },
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(stringResource(Res.string.credits_source_button))
        }
    }
}

