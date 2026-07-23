package gr.panoramapolihnitou.app.ui.screens.pages

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import gr.panoramapolihnitou.app.ui.components.PanoramaTopBar
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors

private val sections = listOf(
    "1. Αποδοχή Όρων" to listOf(
        "Χρησιμοποιώντας την εφαρμογή και τον ιστότοπο «Πανόραμα Πολιχνίτου», αποδέχεστε πλήρως τους παρόντες όρους χρήσης. Αν διαφωνείτε με οποιονδήποτε από αυτούς, παρακαλούμε να μη χρησιμοποιείτε τις υπηρεσίες μας."
    ),
    "2. Πνευματική Ιδιοκτησία" to listOf(
        "Όλο το περιεχόμενο που δημοσιεύεται στην εφαρμογή και τον ιστότοπο — άρθρα, φωτογραφίες, γραφικά, βίντεο και κάθε άλλο υλικό — αποτελεί πνευματική ιδιοκτησία του «Πανόραμα Πολιχνίτου» ή τρίτων που έχουν παραχωρήσει τα σχετικά δικαιώματα.",
        "Απαγορεύεται ρητά η αναπαραγωγή, διανομή, μετάδοση ή οποιαδήποτε άλλη χρήση του περιεχομένου χωρίς προηγούμενη γραπτή άδεια."
    ),
    "3. Ευθύνη Περιεχομένου" to listOf(
        "Καταβάλλουμε κάθε δυνατή προσπάθεια για την εγκυρότητα και ακρίβεια των πληροφοριών που δημοσιεύουμε. Ωστόσο, δεν φέρουμε ευθύνη για τυχόν λάθη, παραλείψεις ή ανακρίβειες στο περιεχόμενο."
    ),
    "4. Σύνδεσμοι Τρίτων" to listOf(
        "Η εφαρμογή μπορεί να περιλαμβάνει συνδέσμους σε εξωτερικούς ιστότοπους τρίτων. Δεν φέρουμε καμία ευθύνη για το περιεχόμενο ή τις πρακτικές απορρήτου αυτών των ιστότοπων."
    ),
    "5. Απόρρητο & Δεδομένα" to listOf(
        "Η εφαρμογή αποθηκεύει τοπικά στη συσκευή σας μόνο τα άρθρα που αποθηκεύετε (σελιδοδείκτες). Δεν συλλέγουμε ή αποστέλλουμε προσωπικά δεδομένα χωρίς τη ρητή συγκατάθεσή σας."
    ),
    "6. Τροποποίηση Όρων" to listOf(
        "Διατηρούμε το δικαίωμα να τροποποιούμε τους παρόντες όρους οποτεδήποτε. Η συνέχιση χρήσης της εφαρμογής μετά από αλλαγές συνεπάγεται αποδοχή των νέων όρων."
    ),
    "7. Εφαρμοστέο Δίκαιο" to listOf(
        "Οι παρόντες όροι διέπονται από το ελληνικό δίκαιο. Για οποιαδήποτε διαφορά αρμόδια είναι τα Δικαστήρια της Μυτιλήνης."
    )
)

class TermsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Column(Modifier.fillMaxSize()) {
            PanoramaTopBar(title = "Όροι Χρήσης", showBack = true, onBack = { navigator.pop() })
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)
            ) {
                // Hero
                Column(
                    Modifier.fillMaxWidth().background(PanoramaColors.primary).padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, tint = PanoramaColors.textWhite, modifier = Modifier.size(48.dp))
                    Text("Όροι Χρήσης", color = PanoramaColors.textWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text("Τελευταία ενημέρωση: Ιανουάριος 2024", color = PanoramaColors.textWhite, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }

                sections.forEach { (title, paras) ->
                    Row(
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(width = 4.dp, height = 18.dp).background(PanoramaColors.primary))
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PanoramaColors.text, modifier = Modifier.padding(start = 8.dp))
                    }
                    paras.forEach { p ->
                        Text(p, style = MaterialTheme.typography.bodyLarge, color = PanoramaColors.text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                }

                // Contact box
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .background(PanoramaColors.primary.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Ερωτήσεις;", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PanoramaColors.primary)
                    Text(
                        "Για οποιαδήποτε απορία σχετικά με τους όρους χρήσης, επικοινωνήστε μαζί μας στο info@panoramapolihnitou.gr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PanoramaColors.text
                    )
                }
            }
        }
    }
}
