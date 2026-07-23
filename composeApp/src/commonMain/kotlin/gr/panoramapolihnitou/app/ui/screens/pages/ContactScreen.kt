package gr.panoramapolihnitou.app.ui.screens.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import gr.panoramapolihnitou.app.ui.components.PanoramaTopBar
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors

class ContactScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current

        Column(Modifier.fillMaxSize()) {
            PanoramaTopBar(title = "Επικοινωνία", showBack = true, onBack = { navigator.pop() })
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)
            ) {
                // Hero
                Column(
                    Modifier.fillMaxWidth().background(PanoramaColors.primary).padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Email, contentDescription = null, tint = PanoramaColors.textWhite, modifier = Modifier.size(48.dp))
                    Text("Επικοινωνήστε μαζί μας", color = PanoramaColors.textWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text("Είμαστε στη διάθεσή σας για οποιαδήποτε απορία", color = PanoramaColors.textWhite, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }

                SectionTitle("Στοιχεία Επικοινωνίας")
                ContactRow(Icons.Filled.Email, "Email", "info@panoramapolihnitou.gr") {
                    uriHandler.openUri("mailto:info@panoramapolihnitou.gr")
                }
                ContactRow(Icons.Filled.Public, "Ιστότοπος", "www.panoramapolihnitou.gr") {
                    uriHandler.openUri("https://www.panoramapolihnitou.gr")
                }
                ContactRow(Icons.Filled.LocationOn, "Τοποθεσία", "Πολιχνίτος, Λέσβος", null)

                SectionTitle("Κοινωνικά Δίκτυα")
                ContactRow(Icons.Filled.ThumbUp, "Facebook", "facebook.com/panoramapolihnitou") {
                    uriHandler.openUri("https://www.facebook.com/panoramapolihnitou")
                }

                SectionTitle("Για Δημοσιογράφους & Συνεργάτες")
                Text(
                    "Αν ενδιαφέρεστε να συνεργαστείτε μαζί μας ή να υποβάλετε ένα ρεπορτάζ, στείλτε μας email στη διεύθυνση που αναφέρεται παραπάνω.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PanoramaColors.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Text(
                    "Απαντάμε σε όλα τα μηνύματα εντός 24-48 ωρών.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PanoramaColors.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(width = 4.dp, height = 18.dp).background(PanoramaColors.primary))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PanoramaColors.text,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ContactRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).background(PanoramaColors.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PanoramaColors.primary, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = PanoramaColors.textSecondary)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = PanoramaColors.text)
        }
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = PanoramaColors.textLight)
        }
    }
}
