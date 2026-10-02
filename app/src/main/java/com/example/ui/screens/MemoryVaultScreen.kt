package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocalMemory
import com.example.ui.components.MemoryCard
import com.example.ui.theme.AegisBackground
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisEmerald
import com.example.ui.theme.AegisIndigo
import com.example.ui.theme.AegisOutline
import com.example.ui.theme.AegisRose
import com.example.ui.theme.AegisSurface
import com.example.ui.theme.AegisSurfaceCard

@Composable
fun MemoryVaultScreen(
    memories: List<LocalMemory>,
    onAddMemory: (String, String, String) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onRequestSensitiveAction: (() -> Unit) -> Unit,
    onSummarizeMemoryNow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf("ALL", "PREFERENCES", "ROUTINES", "FREQUENT_APPS", "CONTACTS", "HABITS", "NOTES")

    val filteredMemories = memories.filter { mem ->
        val matchesCategory = (selectedCategory == "ALL") || (mem.category.equals(selectedCategory, ignoreCase = true))
        val matchesSearch = searchQuery.isBlank() ||
                mem.title.contains(searchQuery, ignoreCase = true) ||
                mem.detail.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AegisBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AegisCyan,
                contentColor = Color(0xFF00363D),
                modifier = Modifier.testTag("add_memory_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Memory")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AegisSurface)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AegisIndigo.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Memory Vault",
                            tint = AegisIndigo
                        )
                    }
                    Column {
                        Text(
                            text = "LOCAL MEMORY VAULT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "${memories.size} Knowledge Points • On-Device Encrypted",
                            style = MaterialTheme.typography.labelSmall,
                            color = AegisEmerald
                        )
                    }
                }

                Button(
                    onClick = onSummarizeMemoryNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisIndigo.copy(alpha = 0.2f),
                        contentColor = AegisIndigo
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("auto_summarize_memory_button")
                ) {
                    Text("Auto-Summarize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search learned routines, habits, contacts...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AegisCyan) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("memory_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AegisCyan,
                    unfocusedBorderColor = AegisOutline,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = AegisSurfaceCard,
                    unfocusedContainerColor = AegisSurfaceCard
                )
            )

            // Category filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) AegisCyan.copy(alpha = 0.25f) else Color(0xFF131D31))
                            .border(1.dp, if (isSelected) AegisCyan else AegisOutline, RoundedCornerShape(16.dp))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("category_$cat")
                    ) {
                        Text(
                            text = cat.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) AegisCyan else Color.LightGray
                        )
                    }
                }
            }

            // Memory List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredMemories.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .border(1.dp, AegisOutline, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = AegisSurfaceCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = AegisCyan,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Memories in This Category",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Your assistant learns habits and routines through conversation, or you can manually store preferences via the + button.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(filteredMemories, key = { it.id }) { mem ->
                    MemoryCard(
                        category = mem.category,
                        title = mem.title,
                        detail = mem.detail,
                        confidence = mem.confidence,
                        isPinned = mem.isPinned,
                        onDelete = {
                            onRequestSensitiveAction {
                                onDeleteMemory(mem.id)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var cat by remember { mutableStateOf("PREFERENCES") }
        var title by remember { mutableStateOf("") }
        var detail by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = AegisSurface,
            title = {
                Text("Store in Local Memory", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Topic / Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_memory_title"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisCyan,
                            unfocusedBorderColor = AegisOutline,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = detail,
                        onValueChange = { detail = it },
                        label = { Text("Detail / Knowledge") },
                        modifier = Modifier.fillMaxWidth().testTag("add_memory_detail"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisCyan,
                            unfocusedBorderColor = AegisOutline,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddMemory(cat, title, detail)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisCyan, contentColor = Color(0xFF00363D)),
                    modifier = Modifier.testTag("add_memory_save_button")
                ) {
                    Text("Save to Memory", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddDialog = false }
                ) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }
}
