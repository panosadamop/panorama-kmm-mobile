package gr.panoramapolihnitou.app.ui.screens.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import gr.panoramapolihnitou.app.ui.components.NetworkImage
import gr.panoramapolihnitou.app.ui.components.PanoramaTopBar
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors

// The .avif derivative at this path isn't decodable by Coil on Android/iOS
// without an extra codec; the site also serves the original JPEG at this URL.
private const val BANNER = "https://panoramapolihnitou.gr/wp-content/uploads/2024/10/kolaz.jpg"

private val paragraphs = listOf(
    "Υπήρχε από παλιά η διάθεσή μας για τη δημιουργία μιας ιστοσελίδας (site). Τον τελευταίο χρόνο, όμως, διαμορφώθηκε σε πρόθεση και τούτο επειδή:",
    "– Για πάνω από τρία χρόνια δουλέψαμε εντατικά και υπεύθυνα για το χωριό, μέσω του συλλόγου «Πολιχνιατών Αθήνας». Ήρθαμε σε επαφή με πάρα πολλούς ανθρώπους, υπηρεσίες, ιδρύματα, φορείς, συλλόγους.",
    "– Η εμπειρία της σύνταξης της εφημερίδας του χωριού μας, «ΠΟΛΙΧΝΙΑΤΙΚΟΣ ΛΟΓΟΣ», μας έδωσε τη δυνατότητα επικοινωνίας με πολλούς συγχωριανούς και όχι μόνο. Εκτός από τα αμιγώς προβλήματα του χωριού μας σχολιάσαμε και αναφερθήκαμε και σε πολλά άλλα σημαντικά θέματα του νησιού αλλά και γενικότερα. Η εμπειρία αυτή ήταν, κυρίως αυτή, που μας ώθησε στη δημιουργία και ενεργοποίηση αυτής της ιστοσελίδας.",
    "– Ζούμε σε μία εποχή περισσότερο ηλεκτρονική πια. Όλοι σχεδόν έχουν κάποια σχέση, κυρίως οι νεότεροι, με τα μέσα κοινωνικής δικτύωσης και η κλασσική ενημέρωση από την εφημερίδα μειώνεται.",
    "– Τον ελεύθερο χρόνο μας αποφασίσαμε να τον μοιραζόμαστε με φίλους και συγχωριανούς δημιουργικά και παραγωγικά.",
    "Έτσι ανοίγουμε ένα παραθυράκι, μία χαραμάδα με πολύ μεράκι και όρεξη.",
    "Οι προθέσεις μας και οι στόχοι μας συνοπτικά περιγράφονται ως εξής:",
    "– Φιλοδοξούμε να φτιάξουμε ένα χώρο υγιούς προβληματισμού και πολιτισμού, γιατί πιστεύουμε πως είναι και το κύριο ζητούμενο στις μέρες μας, ένα κύτταρο πολιτισμικής αναφοράς του τόπου μας και όχι μόνο.",
    "– Πρόθεσή μας είναι να διαμορφώσουμε ένα χώρο ελεύθερης και πολυφωνικής κοινωνικής ενημέρωσης, σε θέματα του χωριού, του νησιού αλλά και γενικότερα, φιλοξενώντας ενυπόγραφα υπεύθυνα και πολιτισμένα τους προβληματισμούς και τις απόψεις όσων επιθυμούν.",
    "– Να μοιραστούμε εμπειρίες, μνήμες, καταστάσεις προγενέστερες αλλά και σύγχρονες. Ζούμε σε μία εποχή, που απαιτεί να μιλάμε, να λέμε τη γνώμη μας στοχαστικά κι ελεύθερα.",
    "– Ξεκάθαρη και απαραβίαστη αρχή μας είναι πως είμαστε υπέρ της δημιουργικής, παραγωγικής σκέψης, που δεν αναπαράγει τον αντικοινωνικό κομματισμό, την ιδεοληψία, τον διχασμό, τον φανατισμό και τη συναισθηματική μιζέρια.",
    "– Θέλουμε να συνδράμουμε, στο μέτρο των δυνατοτήτων μας, στην επίλυση των προβλημάτων του χωριού μας, το οποίο έχει την ανάγκη όλων μας και περισσότερο όσων έχουν τον τρόπο, τα μέσα, τη δύναμη και το κύρος.",
    "– Θα αρχίζουμε την καταγραφή της Ιστορίας του χωριού μας. Πρέπει να μάθουμε «από πού κρατάει η σκούφια μας». Απαιτεί βέβαια χρόνο και μελέτη.",
    "Η ιστοσελίδα μας δεν θα έχει τη μορφή των επαγγελματικής ιστοσελίδας, με τα ατελείωτα θέματα και τον επιβαρυντικό φόρτο των ειδήσεων. Τα θέματα, τα σχόλια, η πληροφορία, η ενημέρωση για την επικαιρότητα αλλά και για θέματα γενικότερου ενδιαφέροντος, θα είναι τόσα, όσα χρειάζεται. Όμως, δεν θα έχει να ζηλέψει σε τίποτα από τις επαγγελματικές ιστοσελίδες ως «στήσιμο».",
    "Πιστεύουμε απόλυτα πως θα προκαλέσουμε το ενδιαφέρον και την περιέργεια πολλών, έτσι ώστε να γίνουμε μία πολύ μεγάλη και όμορφη συντροφιά, με την πάροδο του χρόνου.",
    "Εννοείται πώς όσα άρθρα ή πληροφορίες δεν υπογράφονται από εμάς τους ίδιους δε σημαίνει πως συμφωνούμε κιόλας με τα γραφόμενα σ' αυτά. Ίσως να είναι κείμενα που «κεντρίζουν», για να παραχθεί σκέψη και λόγος.",
    "«Αυτόν που δε συμμετέχει στα κοινά δεν τον θεωρούμε φιλήσυχο αλλά άχρηστο πολίτη» (τον μηδέν τῶν δε μετέχοντα οὐκ ἀπράγμονα ἀλλ' ἀχρεῖον νομίζομεν). Το είπε ο Περικλής, το έγραψε ο Θουκυδίδης, προσωπικότητες όχι του παρελθόντος αλλά του παρόντος και, κυρίως, του μέλλοντος."
)

class WhoWeAreScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Column(Modifier.fillMaxSize()) {
            PanoramaTopBar(title = "Ποιοι Είμαστε", showBack = true, onBack = { navigator.pop() })
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.fillMaxWidth()) {
                    NetworkImage(
                        url = BANNER,
                        contentDescription = "Πολιχνίτος",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp).height(180.dp)
                    )
                }
                Text(
                    "Ποιοι Είμαστε",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PanoramaColors.primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                paragraphs.forEach { p ->
                    Text(
                        p,
                        style = MaterialTheme.typography.bodyLarge,
                        color = PanoramaColors.text,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                Box(Modifier.height(24.dp))
            }
        }
    }
}
