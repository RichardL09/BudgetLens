package org.umn.utsmap.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import org.umn.utsmap.ui.theme.Outfit
import org.umn.utsmap.ui.theme.Inter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.umn.utsmap.R
import org.umn.utsmap.ui.theme.BudgetLensTheme

private data class IntroPage(val title: Int, val description: Int, val icon: Int, val accent: Color)
private val pages = listOf(
    IntroPage(R.string.intro_transactions, R.string.intro_transactions_body, R.drawable.ic_intro_transactions, Color(0xFF16A34A)),
    IntroPage(R.string.intro_budget, R.string.intro_budget_body, R.drawable.ic_intro_budget, Color(0xFFF4B942)),
    IntroPage(R.string.intro_split, R.string.intro_split_body, R.drawable.ic_intro_split, Color(0xFFFF6B6B)),
    IntroPage(R.string.intro_scan, R.string.intro_scan_body, R.drawable.ic_intro_scan, Color(0xFF16A34A)),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 24.dp)) {
                TextButton(onClick = onFinished, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Text(stringResource(R.string.skip), color = Color(0xFF94A3B8), fontSize = 13.sp, fontFamily = Inter, fontWeight = FontWeight.Medium)
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
                val page = pages[index]
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                            .heightIn(min = maxHeight).padding(horizontal = 24.dp).padding(top = maxHeight * 0.244f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        Box(
                            Modifier.size(180.dp)
                                .figmaCardShadow(page.accent)
                                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)), RoundedCornerShape(40.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(40.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            IntroArtwork(index)
                        }
                        Spacer(Modifier.height(40.dp))
                        val description = stringResource(R.string.intro_page_position, index + 1, pages.size)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.semantics { contentDescription = description }) {
                            pages.indices.forEach { dot ->
                                Box(Modifier.height(6.dp).width(if (dot == index) 24.dp else 6.dp)
                                    .background(if (dot == index) page.accent else Color(0x6694A3B8), CircleShape))
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                        Text(stringResource(page.title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
                            fontFamily = Outfit, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(page.description), modifier = Modifier.widthIn(max = 326.dp), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                GradientButton(
                    onClick = {
                        if (pager.currentPage == pages.lastIndex) onFinished()
                        else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    },
                    enabled = !pager.isScrollInProgress,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text(stringResource(if (pager.currentPage == pages.lastIndex) R.string.get_started else R.string.next))
                }
                Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                    if (pager.currentPage == pages.lastIndex) {
                        TextButton(onClick = onFinished) { Text(stringResource(R.string.already_have_account)) }
                    }
                }
            }
        }
    }
}

@Preview(name = "Onboarding Light", widthDp = 402, heightDp = 874)
@Preview(name = "Onboarding Dark", widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingPreview() {
    BudgetLensTheme { OnboardingScreen(onFinished = {}) }
}
