package gr.panoramapolihnitou.app.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import gr.panoramapolihnitou.app.data.model.Category
import gr.panoramapolihnitou.app.di.AppGraph
import gr.panoramapolihnitou.app.resources.Res
import gr.panoramapolihnitou.app.resources.logo
import gr.panoramapolihnitou.app.ui.screens.articles.ArticleListScreen
import gr.panoramapolihnitou.app.ui.screens.bookmarks.BookmarksScreen
import gr.panoramapolihnitou.app.ui.screens.home.HomeScreen
import gr.panoramapolihnitou.app.ui.screens.pages.ContactScreen
import gr.panoramapolihnitou.app.ui.screens.pages.TermsScreen
import gr.panoramapolihnitou.app.ui.screens.pages.WhoWeAreScreen
import gr.panoramapolihnitou.app.ui.screens.search.SearchScreen
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors
import org.jetbrains.compose.resources.painterResource

private val White35 = Color(0x59FFFFFF)
private val White30 = Color(0x4DFFFFFF)
private val White12 = Color(0x1FFFFFFF)
private val White08 = Color(0x14FFFFFF)
private val White80 = Color(0xCCFFFFFF)
private val White60 = Color(0x99FFFFFF)
private val White40 = Color(0x66FFFFFF)

@Composable
fun AppDrawer(navigator: Navigator, onClose: () -> Unit) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var loadingCats by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        runCatching { AppGraph.repository.getCategories() }
            .onSuccess { categories = it }
        loadingCats = false
    }

    fun go(screen: Screen, resetToRoot: Boolean = false) {
        onClose()
        if (resetToRoot) navigator.popUntilRoot() else navigator.push(screen)
    }

    Column(
        Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(PanoramaColors.drawerBg)
    ) {
        // Header
        Column(
            Modifier
                .fillMaxWidth()
                .background(PanoramaColors.primaryDark)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "Πανόραμα Πολιχνίτου",
                modifier = Modifier.width(160.dp).height(50.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "panoramapolihnitou.gr",
                color = White40,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        }

        LazyColumn(Modifier.weight(1f)) {
            // Main navigation
            item {
                DrawerItem(Icons.Outlined.Home, "Αρχική") { go(HomeScreen(), resetToRoot = true) }
                DrawerItem(Icons.Outlined.BookmarkBorder, "Αποθηκευμένα") { go(BookmarksScreen()) }
                DrawerItem(Icons.Outlined.Search, "Αναζήτηση") { go(SearchScreen()) }
                Separator()
            }
            // Static pages
            item {
                SectionLabel("ΠΛΗΡΟΦΟΡΙΕΣ")
                DrawerItem(Icons.Outlined.Info, "Ποιοι Είμαστε") { go(WhoWeAreScreen()) }
                DrawerItem(Icons.Outlined.MailOutline, "Επικοινωνία") { go(ContactScreen()) }
                DrawerItem(Icons.AutoMirrored.Outlined.Article, "Όροι Χρήσης") { go(TermsScreen()) }
                Separator()
            }
            // Categories (collapsible)
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "ΚΑΤΗΓΟΡΙΕΣ",
                        color = White35,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Icon(
                        if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0x80FFFFFF)
                    )
                }
            }
            if (expanded) {
                if (loadingCats) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PanoramaColors.drawerAccent)
                        }
                    }
                } else {
                    items(categories, key = { it.id }) { cat ->
                        CategoryRow(cat) { go(ArticleListScreen(cat.id, cat.name)) }
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }

        // Footer
        Column(
            Modifier
                .fillMaxWidth()
                .background(PanoramaColors.drawerBg)
                .padding(16.dp)
        ) {
            Text(
                "© 2026 Πανόραμα Πολιχνίτου",
                color = White30,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(32.dp)
                .background(White08, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PanoramaColors.drawerAccent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            color = PanoramaColors.textWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = White30,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun CategoryRow(category: Category, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 32.dp, end = 16.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).background(PanoramaColors.drawerAccent, CircleShape))
        Spacer(Modifier.width(12.dp))
        Text(
            category.name,
            color = White80,
            fontSize = 14.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .background(White12, CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("${category.count}", color = White60, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = White35,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun Separator() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(1.dp)
            .background(White08)
    )
}
