package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.DocEntity
import com.example.viewmodel.DocViewModel
import kotlinx.coroutines.launch

data class ChatMessage(val isUser: Boolean, val text: String)

@Composable
fun AIChatPanel(
    draftContent: String,
    onContentChange: (String) -> Unit,
    onClose: () -> Unit,
    viewModel: DocViewModel,
    selectedDoc: DocEntity?,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    var panelWidth by remember { mutableStateOf(400.dp) }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF1E1E22) else Color(0xFFF9FAFB))
    ) {
        // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = "AI Assistant",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Assistant",
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else Color.Black,
                        fontSize = 16.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { messages = emptyList() }, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = "New Chat", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { /* History */ }, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Outlined.Info, contentDescription = "History", modifier = Modifier.size(18.dp)) // Using Info since History might not exist
                    }
                    IconButton(onClick = { /* Settings */ }, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Divider(color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f))

            // Message Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("How can I help you today?", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages) { message ->
                            ChatBubble(message = message, isDarkTheme = isDarkTheme)
                        }
                    }
                }
            }

            // Quick Actions
            val quickActions = listOf("Rewrite", "Summarize", "Translate", "Explain", "Format", "Create Table", "Create Diagram")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickActions) { action ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkTheme) Color(0xFF2B2B30) else Color(0xFFE5E7EB),
                        modifier = Modifier.clickable { 
                            val query = "$action the document"
                            messages = messages + ChatMessage(isUser = true, text = query)
                            messages = messages + ChatMessage(isUser = false, text = "I am processing your request to '$action'.\n\n(This is a simulated AI response.)")
                        }
                    ) {
                        Text(
                            text = action,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = if (isDarkTheme) Color.LightGray else Color.DarkGray
                        )
                    }
                }
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
                    .background(if (isDarkTheme) Color(0xFF2B2B30) else Color.White, RoundedCornerShape(24.dp))
                    .border(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Attach */ }, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = "Attach", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Ask AI anything about this document...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = if (isDarkTheme) Color.White else Color.Black),
                        maxLines = 4
                    )
                }
                if (inputText.isNotEmpty()) {
                    IconButton(
                        onClick = { 
                            val userMsg = inputText
                            inputText = ""
                            messages = messages + ChatMessage(isUser = true, text = userMsg)
                            // Simulate AI
                            messages = messages + ChatMessage(isUser = false, text = "I heard you say:\n\"$userMsg\"\n\nHow else can I assist?")
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Outlined.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
}

@Composable
fun ChatBubble(message: ChatMessage, isDarkTheme: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Star, contentDescription = "AI", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .background(
                    color = if (message.isUser) MaterialTheme.colorScheme.primary else (if (isDarkTheme) Color(0xFF2B2B30) else Color.White),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (message.isUser) Color.Transparent else (if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isUser) Color.White else (if (isDarkTheme) Color.White else Color.Black),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        if (message.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, contentDescription = "User", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
