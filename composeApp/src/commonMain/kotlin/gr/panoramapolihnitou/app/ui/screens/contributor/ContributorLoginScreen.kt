package gr.panoramapolihnitou.app.ui.screens.contributor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import gr.panoramapolihnitou.app.data.local.Contributor
import gr.panoramapolihnitou.app.data.remote.ContributorConfig
import gr.panoramapolihnitou.app.di.AppGraph
import gr.panoramapolihnitou.app.ui.components.PanoramaTopBar
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors
import kotlinx.coroutines.launch

/**
 * Entry to the contributor area. The code is checked by the backend — the app
 * itself holds no password — and on success the contributor's name and roles
 * are remembered so this screen is skipped next time.
 */
class ContributorLoginScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        var code by remember { mutableStateOf("") }
        var showCode by remember { mutableStateOf(false) }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        fun unlock() {
            val entered = code.trim()
            if (entered.isEmpty() || busy) return
            busy = true
            error = null
            scope.launch {
                runCatching { AppGraph.contributorApi.verify(entered) }
                    .onSuccess { response ->
                        AppGraph.contributorSession.save(
                            Contributor(code = entered, name = response.name ?: "Συνεργάτης", roles = response.roles)
                        )
                        navigator.replace(ContributorHomeScreen())
                    }
                    .onFailure { error = it.contributorMessage() }
                busy = false
            }
        }

        Column(Modifier.fillMaxSize()) {
            PanoramaTopBar(title = "Συνεργάτες", showBack = true, onBack = { navigator.pop() })
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
                ContributorHero(
                    icon = Icons.Outlined.Lock,
                    title = "Περιοχή συνεργατών",
                    subtitle = "Για ανταποκριτές και συνεργάτες του Πανοράματος"
                )

                if (!ContributorConfig.isConfigured) {
                    ContributorNotice("Η περιοχή συνεργατών δεν έχει ενεργοποιηθεί ακόμη σε αυτή την έκδοση της εφαρμογής.")
                    return@Column
                }

                Text(
                    "Εισάγετε τον κωδικό πρόσβασης που σας έδωσε η σύνταξη για να στέλνετε φωτογραφίες, ηχογραφήσεις και κείμενα ή να μεταδίδετε ζωντανά.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PanoramaColors.text,
                    modifier = Modifier.padding(16.dp)
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; error = null },
                    label = { Text("Κωδικός πρόσβασης") },
                    singleLine = true,
                    enabled = !busy,
                    isError = error != null,
                    visualTransformation = if (showCode) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { unlock() }),
                    trailingIcon = {
                        IconButton(onClick = { showCode = !showCode }) {
                            Icon(
                                if (showCode) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showCode) "Απόκρυψη κωδικού" else "Εμφάνιση κωδικού"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { unlock() },
                    enabled = code.isNotBlank() && !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = PanoramaColors.primary),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp)
                ) {
                    if (busy) {
                        CircularProgressIndicator(color = PanoramaColors.textWhite, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Είσοδος")
                    }
                }
                Text(
                    "Δεν έχετε κωδικό; Επικοινωνήστε με τη σύνταξη στο ${ContributorConfig.MAIL_TO}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PanoramaColors.textSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
