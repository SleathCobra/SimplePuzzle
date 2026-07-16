package com.qtpie.simplepuzzle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qtpie.simplepuzzle.core.learning.EvidenceStatus
import com.qtpie.simplepuzzle.core.learning.PersonalSkillSummary
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningSummaryScreen(
    summaries: List<PersonalSkillSummary>,
    onBack: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MY LEARNING", color = Color.White, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        if (summaries.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = Color(0xFFFFE066),
                )
                Spacer(Modifier.height(16.dp))
                Text("No learning evidence yet", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Play Jigsaw Math to begin a private, on-device practice summary.",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 16.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(contentType = "explanation") {
                    Text(
                        text = "These summaries describe recent practice on this device. They are not grades or diagnoses.",
                        color = Color.White.copy(alpha = 0.86f),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
                items(
                    items = summaries,
                    key = { it.skillId.value },
                    contentType = { "skill-summary" },
                ) { summary ->
                    SkillSummaryCard(summary)
                }
            }
        }
    }
}

@Composable
private fun SkillSummaryCard(summary: PersonalSkillSummary) {
    val presentation = statusPresentation(summary.status)
    val lastPracticed = remember(summary.lastPracticedAtEpochMillis) {
        summary.lastPracticedAtEpochMillis?.let { millis ->
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
        } ?: "Not yet recorded"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = presentation.label },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(summary.skillTitle, color = Color(0xFF201A4D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = presentation.color.copy(alpha = 0.16f)) {
                Text(
                    presentation.label,
                    color = presentation.color,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recent evidence: ${summary.evidenceCount}", color = Color(0xFF554F72), fontSize = 14.sp)
                Text("Item variety: ${summary.distinctTemplateCount}", color = Color(0xFF554F72), fontSize = 14.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text("Last practiced: $lastPracticed", color = Color(0xFF554F72), fontSize = 14.sp)
            if (summary.reviewSuggested) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "A little more guided practice may be useful.",
                    color = Color(0xFF9A4A22),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private data class StatusPresentation(val label: String, val color: Color)

private fun statusPresentation(status: EvidenceStatus): StatusPresentation = when (status) {
    EvidenceStatus.INSUFFICIENT_EVIDENCE -> StatusPresentation("Keep practicing", Color(0xFF5A4FCF))
    EvidenceStatus.DEVELOPING -> StatusPresentation("Developing", Color(0xFF6A4BBC))
    EvidenceStatus.PRACTICED_RECENTLY -> StatusPresentation("Practiced recently", Color(0xFF16835B))
    EvidenceStatus.REVIEW_SUGGESTED -> StatusPresentation("Review suggested", Color(0xFFB45B2A))
}
