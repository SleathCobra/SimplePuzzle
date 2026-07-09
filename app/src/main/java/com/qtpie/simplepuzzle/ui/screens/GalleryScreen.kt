package com.qtpie.simplepuzzle.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qtpie.simplepuzzle.model.PuzzleInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    puzzles: List<PuzzleInfo>,
    completedCount: Int,
    totalCount: Int,
    onPuzzleSelect: (PuzzleInfo) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Space", "Fantasy", "Nature")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PUZZLE GALLERY", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Completed: $completedCount / $totalCount Puzzles",
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { completedCount.toFloat() / totalCount },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                color = Color.Black.copy(alpha = 0.2f)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(selectedCategory),
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    edgePadding = 0.dp,
                    divider = {},
                    indicator = {}
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        Tab(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            text = {
                                Box(
                                    modifier = Modifier
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) Color.White else Color.Transparent)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        category,
                                        color = if (isSelected) Color(0xFF5A4FCF) else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val filteredPuzzles = if (selectedCategory == "All") puzzles else puzzles.filter { it.category == selectedCategory }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredPuzzles) { puzzle ->
                    PuzzleCard(puzzle = puzzle, onClick = { if (!puzzle.isLocked) onPuzzleSelect(puzzle) })
                }
                
                // Add a "Coming Soon" item if needed
                item {
                    ComingSoonCard()
                }
            }
        }
    }
}

@Composable
fun PuzzleCard(puzzle: PuzzleInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(4.dp)
                .border(
                    width = if (puzzle.isCompleted) 3.dp else 0.dp,
                    color = if (puzzle.isCompleted) Color(0xFFFFD700).copy(alpha = 0.8f) else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Image(
                painter = painterResource(id = puzzle.imageResId),
                contentDescription = puzzle.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
                alpha = if (puzzle.isLocked) 0.5f else 1.0f
            )

            if (puzzle.isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }

            if (puzzle.isCompleted) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Completed",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (puzzle.isLocked) "Locked" else puzzle.name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        if (puzzle.isCompleted) {
            Text("✓ Complete", color = Color(0xFF00FF00), fontSize = 12.sp)
        }
    }
}

@Composable
fun ComingSoonCard() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.3f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Coming Soon", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
    }
}
