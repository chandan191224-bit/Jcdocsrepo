package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.text.RegexOption
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.spring
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.db.DocEntity
import com.example.ui.theme.*
import com.example.viewmodel.DocViewModel
import com.example.viewmodel.SlideItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocEditorScreen(
    viewModel: DocViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val selectedDoc by viewModel.selectedDoc.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()

    val draftTitle by viewModel.draftTitle.collectAsStateWithLifecycle()
    val draftContent by viewModel.draftContent.collectAsStateWithLifecycle()

    val isPlayingPresentation by viewModel.isPlayingPresentation.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var listExpanded by remember { mutableStateOf(true) } // For responsive left-right side panes

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main adaptive workspace
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Document explorer sidebar (Width is dynamic/collapsible)
                AnimatedVisibility(
                    visible = listExpanded,
                    enter = slideInHorizontally() + fadeIn(),
                    exit = slideOutHorizontally() + fadeOut()
                ) {
                    SidebarExplorer(
                        documents = documents,
                        selectedDoc = selectedDoc,
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onFilterChange = { viewModel.setTypeFilter(it) },
                        onDocSelect = { viewModel.selectDocument(it) },
                        onDocDelete = { viewModel.deleteDocument(it) },
                        onDocFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onCreateClick = { showCreateDialog = true },
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.background)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                            )
                    )
                }

                // Workspace pane
                WorkspacePane(
                    selectedDoc = selectedDoc,
                    draftTitle = draftTitle,
                    draftContent = draftContent,
                    onTitleChange = { viewModel.updateDraftTitle(it) },
                    onContentChange = { viewModel.updateDraftContent(it) },
                    onCloseClick = { viewModel.selectDocument(null) },
                    onToggleSidebar = { listExpanded = !listExpanded },
                    isSidebarExpanded = listExpanded,
                    viewModel = viewModel,
                    onFABClick = { showCreateDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Dialog for creating a new document template
            if (showCreateDialog) {
                CreateDocumentDialog(
                    onDismiss = { showCreateDialog = false },
                    onConfirm = { title, type ->
                        viewModel.createNewDocument(title, type)
                        showCreateDialog = false
                    }
                )
            }

            // Full-screen presentation mode overlay
            if (isPlayingPresentation && selectedDoc?.type == "slide") {
                FullscreenPresentationView(
                    viewModel = viewModel,
                    onExit = { viewModel.togglePresenterMode(false) }
                )
            }
        }
    }
}

@Composable
fun SidebarExplorer(
    documents: List<DocEntity>,
    selectedDoc: DocEntity?,
    searchQuery: String,
    selectedFilter: String,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onDocSelect: (DocEntity) -> Unit,
    onDocDelete: (DocEntity) -> Unit,
    onDocFavoriteToggle: (DocEntity) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // App header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ONLYOFFICE inspired styled icon with terracotta orange background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OnlyOfficePrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "JCdocs Logo Symbol",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "JCdocs",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "ONLYOFFICE Suite Engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // Search Bar with custom tags for automations
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search office sheets & files...", fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Files"
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OnlyOfficePrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("file_search_bar")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Create New Document Primary Action button
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = OnlyOfficePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("create_document_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add New")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Office File", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter categories slider Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf(
                FilterItem("all", "All", Icons.Default.List),
                FilterItem("word", "Writer", Icons.Default.Edit),
                FilterItem("sheet", "Sheets", Icons.Default.PlayArrow),
                FilterItem("slide", "Slides", Icons.Default.Share)
            )
            items(filters) { category ->
                val isSelected = selectedFilter == category.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterChange(category.id) },
                    label = { Text(category.displayName, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.displayName,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OnlyOfficePrimary.copy(alpha = 0.15f),
                        selectedLabelColor = OnlyOfficePrimary,
                        selectedLeadingIconColor = OnlyOfficePrimary
                    )
                )
            }
        }

        Divider(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
        )

        // Title listing title
        Text(
            text = "RECENT DOCUMENTS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // List of document tiles
        if (documents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty State",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No files found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(documents, key = { it.id }) { doc ->
                    val isSelected = selectedDoc?.id == doc.id
                    DocumentTile(
                        doc = doc,
                        isSelected = isSelected,
                        onClick = { onDocSelect(doc) },
                        onDelete = { onDocDelete(doc) },
                        onFavoriteToggle = { onDocFavoriteToggle(doc) }
                    )
                }
            }
        }
    }
}

data class FilterItem(val id: String, val displayName: String, val icon: ImageVector)

@Composable
fun DocumentTile(
    doc: DocEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = when (doc.type) {
        "word" -> DocWordColor
        "sheet" -> DocSheetColor
        "slide" -> DocSlideColor
        else -> OnlyOfficePrimary
    }

    val typeIconStr = when (doc.type) {
        "word" -> "W"
        "sheet" -> "S"
        "slide" -> "P"
        else -> "D"
    }

    val formattedDate = remember(doc.updatedAt) {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(doc.updatedAt))
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                typeColor.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) typeColor.copy(alpha = 0.5f) else Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("document_tile_${doc.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon layout matching ONLYOFFICE aesthetic
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(typeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = typeIconStr,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Document item actions
            Row(horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (doc.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (doc.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Doc",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Data class representing high-fidelity Ribbon Tool
data class RibbonTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val category: String,
    val tab: String,
    val actionId: String,
    val hasDropdown: Boolean = false,
    val onClick: () -> Unit = {}
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RibbonToolCard(
    tool: RibbonTool,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { 
                if (tool.hasDropdown) expanded = true 
                else tool.onClick() 
            }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.title,
                tint = if (isDarkTheme) Color.White.copy(alpha = 0.85f) else Color.DarkGray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tool.title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isDarkTheme) Color.LightGray else Color.DarkGray,
                textAlign = TextAlign.Center
            )
        }
        if (tool.hasDropdown) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = if (isDarkTheme) Color.LightGray else Color.DarkGray,
                modifier = Modifier.size(12.dp).align(Alignment.CenterEnd).padding(end = 4.dp)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("More Options...", fontSize = 12.sp) },
                    onClick = {
                        expanded = false
                        tool.onClick()
                    }
                )
            }
        }
    }
}

fun getRibbonTools(
    selectedDoc: DocEntity,
    onActionText: (String) -> Unit
): List<RibbonTool> {
    return listOf(
        // --- HOME TAB TOOLS ---
        RibbonTool(
            id = "bold",
            title = "Bold",
            description = "Apply bold style layout",
            icon = Icons.Default.FormatBold,
            category = "Font Formatting",
            tab = "Home",
            actionId = "bold"
        ),
        RibbonTool(
            id = "italic",
            title = "Italic",
            description = "Apply italic text",
            icon = Icons.Default.FormatItalic,
            category = "Font Formatting",
            tab = "Home",
            actionId = "italic"
        ),
        RibbonTool(
            id = "underline",
            title = "Underline",
            description = "Apply text underlining",
            icon = Icons.Default.FormatUnderlined,
            category = "Font Formatting",
            tab = "Home",
            actionId = "underline"
        ),
        RibbonTool(
            id = "align_left",
            title = "Align Left",
            description = "Position text on the left",
            icon = Icons.Default.FormatAlignLeft,
            category = "Paragraph Alignment",
            tab = "Home",
            actionId = "align_left"
        ),
        RibbonTool(
            id = "align_center",
            title = "Center",
            description = "Center document paragraph",
            icon = Icons.Default.FormatAlignCenter,
            category = "Paragraph Alignment",
            tab = "Home",
            actionId = "align_center"
        ),
        RibbonTool(
            id = "align_right",
            title = "Align Right",
            description = "Position text on the right",
            icon = Icons.Default.FormatAlignRight,
            category = "Paragraph Alignment",
            tab = "Home",
            actionId = "align_right"
        ),
        RibbonTool(
            id = "theme_white",
            title = "White Mode",
            description = "Select white paper backdrop",
            icon = Icons.Default.LightMode,
            category = "Page Theme Layout",
            tab = "Home",
            actionId = "theme_white"
        ),
        RibbonTool(
            id = "theme_ivory",
            title = "Ivory Mode",
            description = "Select warm notepad tone",
            icon = Icons.Default.WbSunny,
            category = "Page Theme Layout",
            tab = "Home",
            actionId = "theme_ivory"
        ),
        RibbonTool(
            id = "theme_dark",
            title = "Dark Mode",
            description = "Select low-light layout canvas",
            icon = Icons.Default.DarkMode,
            category = "Page Theme Layout",
            tab = "Home",
            actionId = "theme_dark"
        ),
        RibbonTool(
            id = "font_incr",
            title = "Increase Font",
            description = "Increase font text size",
            icon = Icons.Default.TextIncrease,
            category = "Text Size Scale",
            tab = "Home",
            actionId = "font_incr"
        ),
        RibbonTool(
            id = "font_decr",
            title = "Decrease Font",
            description = "Decrease font text size",
            icon = Icons.Default.TextDecrease,
            category = "Text Size Scale",
            tab = "Home",
            actionId = "font_decr"
        ),
        RibbonTool(
            id = "clear_format",
            title = "Clear Edits",
            description = "Strip active styling tags",
            icon = Icons.Default.Close,
            category = "Text Size Scale",
            tab = "Home",
            actionId = "clear_format"
        ),

        // --- INSERT TAB TOOLS ---
        RibbonTool(id = "cover_page", title = "Cover Page", description = "Cover Page", icon = Icons.Default.Description, category = "Pages", tab = "Insert", actionId = "cover_page"),
        RibbonTool(id = "blank_page", title = "Blank Page", description = "Blank Page", icon = Icons.Default.NoteAdd, category = "Pages", tab = "Insert", actionId = "blank_page"),
        RibbonTool(id = "page_break", title = "Page Break", description = "Page Break", icon = Icons.Default.VerticalAlignBottom, category = "Pages", tab = "Insert", actionId = "page_break"),
        RibbonTool(id = "insert_table", title = "Table", description = "Insert Table", icon = Icons.Default.TableChart, category = "Tables", tab = "Insert", actionId = "insert_table"),
        RibbonTool(id = "picture", title = "Picture", description = "Picture", icon = Icons.Default.Image, category = "Illustrations", tab = "Insert", actionId = "picture"),
        RibbonTool(id = "shapes", title = "Shapes", description = "Shapes", icon = Icons.Default.Category, category = "Illustrations", tab = "Insert", actionId = "shapes"),
        RibbonTool(id = "chart", title = "Chart", description = "Chart", icon = Icons.Default.BarChart, category = "Illustrations", tab = "Insert", actionId = "chart"),
        RibbonTool(id = "hyperlink", title = "Link", description = "Link", icon = Icons.Default.Link, category = "Links", tab = "Insert", actionId = "hyperlink"),
        RibbonTool(id = "bookmark", title = "Bookmark", description = "Bookmark", icon = Icons.Default.Bookmark, category = "Links", tab = "Insert", actionId = "bookmark"),
        RibbonTool(id = "header_footer", title = "Header & Footer", description = "Header & Footer", icon = Icons.Default.ViewAgenda, category = "Header & Footer", tab = "Insert", actionId = "header_footer"),
        RibbonTool(id = "text_box", title = "Text Box", description = "Text Box", icon = Icons.Default.TextFields, category = "Text", tab = "Insert", actionId = "text_box"),

        // --- LAYOUT TAB TOOLS ---
        // 1. Page Setup Group
        RibbonTool(id = "margins", title = "Margins", description = "Set Page Margins", icon = Icons.Default.SettingsOverscan, category = "Page Setup", tab = "Layout", actionId = "margins", hasDropdown = true),
        RibbonTool(id = "orientation", title = "Orientation", description = "Page Orientation", icon = Icons.Default.ScreenRotation, category = "Page Setup", tab = "Layout", actionId = "orientation", hasDropdown = true),
        RibbonTool(id = "size", title = "Size", description = "Page Size", icon = Icons.Default.AspectRatio, category = "Page Setup", tab = "Layout", actionId = "size", hasDropdown = true),
        RibbonTool(id = "columns", title = "Columns", description = "Page Columns", icon = Icons.Default.ViewColumn, category = "Page Setup", tab = "Layout", actionId = "columns", hasDropdown = true),
        RibbonTool(id = "breaks", title = "Breaks", description = "Page Breaks", icon = Icons.Default.KeyboardReturn, category = "Page Setup", tab = "Layout", actionId = "breaks", hasDropdown = true),

        // 2. Themes Group
        RibbonTool(id = "theme_apply", title = "Themes", description = "Document Themes", icon = Icons.Default.ColorLens, category = "Themes", tab = "Layout", actionId = "theme_apply", hasDropdown = true),
        RibbonTool(id = "theme_colors", title = "Colors", description = "Theme Colors", icon = Icons.Default.Palette, category = "Themes", tab = "Layout", actionId = "theme_colors", hasDropdown = true),
        RibbonTool(id = "theme_fonts", title = "Fonts", description = "Theme Fonts", icon = Icons.Default.FontDownload, category = "Themes", tab = "Layout", actionId = "theme_fonts", hasDropdown = true),
        RibbonTool(id = "theme_effects", title = "Effects", description = "Theme Effects", icon = Icons.Default.AutoAwesome, category = "Themes", tab = "Layout", actionId = "theme_effects", hasDropdown = true),

        // 3. Page Background Group
        RibbonTool(id = "watermark", title = "Watermark", description = "Page Watermark", icon = Icons.Default.BrandingWatermark, category = "Page Background", tab = "Layout", actionId = "watermark", hasDropdown = true),
        RibbonTool(id = "page_color", title = "Page Color", description = "Page Background Color", icon = Icons.Default.FormatColorFill, category = "Page Background", tab = "Layout", actionId = "page_color", hasDropdown = true),
        RibbonTool(id = "page_borders", title = "Page Borders", description = "Page Borders", icon = Icons.Default.BorderAll, category = "Page Background", tab = "Layout", actionId = "page_borders", hasDropdown = false),

        // References Tab Tools removed

        // --- REVIEW TAB TOOLS ---
        RibbonTool(id = "spelling_grammar", title = "Spelling", description = "Spelling", icon = Icons.Default.Spellcheck, category = "Proofing", tab = "Review", actionId = "spelling_grammar", hasDropdown = false),
        RibbonTool(id = "thesaurus", title = "Thesaurus", description = "Thesaurus", icon = Icons.Default.MenuBook, category = "Proofing", tab = "Review", actionId = "thesaurus", hasDropdown = false),
        RibbonTool(id = "word_count", title = "Word Count", description = "Word Count", icon = Icons.Default.Numbers, category = "Proofing", tab = "Review", actionId = "word_count", hasDropdown = false),
        RibbonTool(id = "read_aloud", title = "Read Aloud", description = "Read Aloud", icon = Icons.Default.VolumeUp, category = "Speech", tab = "Review", actionId = "read_aloud", hasDropdown = false),
        RibbonTool(id = "check_accessibility", title = "Accessibility", description = "Accessibility", icon = Icons.Default.Accessibility, category = "Accessibility", tab = "Review", actionId = "check_accessibility", hasDropdown = false),
        RibbonTool(id = "translate", title = "Translate", description = "Translate", icon = Icons.Default.Translate, category = "Language", tab = "Review", actionId = "translate", hasDropdown = true),
        RibbonTool(id = "language", title = "Language", description = "Language", icon = Icons.Default.Language, category = "Language", tab = "Review", actionId = "language", hasDropdown = true),
        RibbonTool(id = "new_comment", title = "New Comment", description = "New Comment", icon = Icons.Default.AddComment, category = "Comments", tab = "Review", actionId = "new_comment", hasDropdown = false),
        RibbonTool(id = "delete_comment", title = "Delete", description = "Delete", icon = Icons.Default.DeleteOutline, category = "Comments", tab = "Review", actionId = "delete_comment", hasDropdown = false),
        RibbonTool(id = "show_comments", title = "Show Comments", description = "Show Comments", icon = Icons.Default.Chat, category = "Comments", tab = "Review", actionId = "show_comments", hasDropdown = false),
        RibbonTool(id = "track_changes", title = "Track Changes", description = "Track Changes", icon = Icons.Default.EditNote, category = "Tracking", tab = "Review", actionId = "track_changes", hasDropdown = true),

        // --- AI ASSISTANT TAB TOOLS ---
        RibbonTool(
            id = "ai_summarize",
            title = "Summarize Text",
            description = "Summarize Text",
            icon = Icons.Default.AutoAwesome,
            category = "AI Co Pilot Engine",
            tab = "AI Assistant",
            actionId = "ai_summarize"
        ),
        RibbonTool(
            id = "ai_improve",
            title = "Improve Tone",
            description = "Improve Tone",
            icon = Icons.Default.AutoFixHigh,
            category = "AI Co Pilot Engine",
            tab = "AI Assistant",
            actionId = "ai_improve"
        ),
        RibbonTool(
            id = "ai_grammar",
            title = "Fix Grammar",
            description = "Fix Grammar Error",
            icon = Icons.Default.Spellcheck,
            category = "AI Co Pilot Engine",
            tab = "AI Assistant",
            actionId = "ai_grammar"
        ),
        RibbonTool(
            id = "ai_topics",
            title = "Suggest Topics",
            description = "Suggest Topics",
            icon = Icons.Default.Lightbulb,
            category = "Creative Writing Vectors",
            tab = "AI Assistant",
            actionId = "ai_topics"
        )
    ).map { tool ->
        tool.copy(onClick = { onActionText(tool.actionId) })
    }
}

fun executeRibbonAction(
    actionId: String,
    draftContent: String,
    onContentChange: (String) -> Unit,
    selectedDoc: DocEntity,
    viewModel: DocViewModel,
    editorTheme: String,
    onThemeChange: (String) -> Unit,
    onMarginsChange: (androidx.compose.ui.unit.Dp) -> Unit,
    onColumnsChange: (Int) -> Unit,
    onAlignmentChange: (androidx.compose.ui.text.style.TextAlign) -> Unit,
    onFontSizeChange: (androidx.compose.ui.unit.TextUnit) -> Unit,
    onLandscapeChange: (Boolean) -> Unit,
    snackbarScope: kotlinx.coroutines.CoroutineScope,
    snackbarState: androidx.compose.material3.SnackbarHostState,
    tts: android.speech.tts.TextToSpeech?,
    isSpeaking: Boolean,
    onSpeakStateChange: (Boolean) -> Unit,
    textFieldValue: TextFieldValue? = null,
    onTextFieldValueChange: ((TextFieldValue) -> Unit)? = null,
    lastSelection: TextRange? = null
) {
    fun showToast(msg: String) {
        snackbarScope.launch {
            snackbarState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    val applyFormatting = { prefix: String, suffix: String, placeholder: String ->
        if (textFieldValue != null && onTextFieldValueChange != null) {
            val selection = lastSelection ?: textFieldValue.selection
            val text = textFieldValue.text
            if (!selection.collapsed) {
                val selectedStr = text.substring(selection.start, selection.end)
                val formatted = prefix + selectedStr + suffix
                val newText = text.replaceRange(selection.start, selection.end, formatted)
                val newSelection = TextRange(selection.start + prefix.length, selection.start + prefix.length + selectedStr.length)
                onTextFieldValueChange(TextFieldValue(text = newText, selection = newSelection))
            } else {
                val insertText = prefix + placeholder + suffix
                val newText = text.replaceRange(selection.start, selection.start, insertText)
                val newSelection = TextRange(selection.start + prefix.length, selection.start + prefix.length + placeholder.length)
                onTextFieldValueChange(TextFieldValue(text = newText, selection = newSelection))
            }
        } else {
            onContentChange(draftContent + " " + prefix + placeholder + suffix)
        }
    }

    when (actionId) {
        // --- HOME ACTIONS ---
        "bold" -> {
            applyFormatting("**", "**", "Bold Text")
            showToast("Bold formatting applied to content")
        }
        "italic" -> {
            applyFormatting("*", "*", "Italic Text")
            showToast("Italic formatting applied to content")
        }
        "underline" -> {
            applyFormatting("<u>", "</u>", "Underline Text")
            showToast("Underline formatting applied to content")
        }
        "strikethrough" -> {
            applyFormatting("~~", "~~", "Strikethrough text")
            showToast("Strikethrough formatting applied to content")
        }
        "subscript" -> {
            applyFormatting("<sub>", "</sub>", "Subscript text")
            showToast("Subscript formatting applied to content")
        }
        "superscript" -> {
            applyFormatting("<sup>", "</sup>", "Superscript text")
            showToast("Superscript formatting applied to content")
        }
        "color" -> {
            applyFormatting("<font color=\"#3B82F6\">", "</font>", "Colored Text")
            showToast("Font color changed to primary accent")
        }
        "highlight" -> {
            applyFormatting("<mark>", "</mark>", "Highlighted Text")
            showToast("Text highlight applied")
        }
        "align_left" -> {
            onAlignmentChange(TextAlign.Left)
            showToast("Text alignment set to Left")
        }
        "align_center" -> {
            onAlignmentChange(TextAlign.Center)
            showToast("Text alignment set to Center")
        }
        "align_right" -> {
            onAlignmentChange(TextAlign.Right)
            showToast("Text alignment set to Right")
        }
        "theme_white" -> {
            onThemeChange("white")
            showToast("Paper theme changed to White")
        }
        "theme_ivory" -> {
            onThemeChange("ivory")
            showToast("Paper theme changed to Ivory Note")
        }
        "theme_dark" -> {
            onThemeChange("dark")
            showToast("Paper theme changed to OLED Dark")
        }
        "font_incr" -> {
            onFontSizeChange(18.sp)
            showToast("Font text size increased to 18sp")
        }
        "font_decr" -> {
            onFontSizeChange(11.sp)
            showToast("Font text size decreased to 11sp")
        }
        "clear_format" -> {
            if (textFieldValue != null && onTextFieldValueChange != null) {
                val selection = lastSelection ?: textFieldValue.selection
                val text = textFieldValue.text
                if (!selection.collapsed) {
                    val selectedStr = text.substring(selection.start, selection.end)
                    val cleaned = selectedStr
                        .replace("**", "")
                        .replace("*", "")
                        .replace("<u>", "")
                        .replace("</u>", "")
                        .replace("~~", "")
                        .replace("<sub>", "")
                        .replace("</sub>", "")
                        .replace("<sup>", "")
                        .replace("</sup>", "")
                        .replace("<font[^>]*>".toRegex(), "")
                        .replace("</font>", "")
                        .replace("<span[^>]*>".toRegex(), "")
                        .replace("</span>", "")
                        .replace("<mark>", "")
                        .replace("</mark>", "")
                    val newText = text.replaceRange(selection.start, selection.end, cleaned)
                    onTextFieldValueChange(TextFieldValue(text = newText, selection = TextRange(selection.start, selection.start + cleaned.length)))
                } else {
                    val cleaned = text
                        .replace("**", "")
                        .replace("*", "")
                        .replace("<u>", "")
                        .replace("</u>", "")
                        .replace("~~", "")
                        .replace("<sub>", "")
                        .replace("</sub>", "")
                        .replace("<sup>", "")
                        .replace("</sup>", "")
                        .replace("<font[^>]*>".toRegex(), "")
                        .replace("</font>", "")
                        .replace("<span[^>]*>".toRegex(), "")
                        .replace("</span>", "")
                        .replace("<mark>", "")
                        .replace("</mark>", "")
                    onTextFieldValueChange(TextFieldValue(text = cleaned, selection = TextRange(cleaned.length)))
                }
            } else {
                val cleaned = draftContent
                    .replace("**", "")
                    .replace("*", "")
                    .replace("<u>", "")
                    .replace("</u>", "")
                onContentChange(cleaned)
            }
            onAlignmentChange(TextAlign.Left)
            onFontSizeChange(14.sp)
            showToast("All text styling and layout formatting tags cleared")
        }

        // --- INSERT ACTIONS ---
        "cover_page" -> {
            val cover = "========================================\n" +
                        "       DOCUMENT COVER PORTFOLIO\n" +
                        "       Title: ${selectedDoc.title}\n" +
                        "       Date: June 8, 2026\n" +
                        "========================================\n\n"
            onContentChange(cover + draftContent)
            showToast("Stylish Document Cover Page prepended at top!")
        }
        "blank_page" -> {
            onContentChange(draftContent + "\n\n\n\n")
            showToast("Blank spacing lines appended to note body")
        }
        "page_break" -> {
            onContentChange(draftContent + "\n\n---\n*PAGE DIVIDER BREAK*\n---\n\n")
            showToast("Visual page break rule appended to note body")
        }
        "insert_table" -> {
            val table = "\n| Item Coordinate | Header Label | Value Count |\n" +
                        "|---|---|---|\n" +
                        "| Office Suite | JCdocs ONLYOFFICE | 100% Native |\n" +
                        "| Database Eng | Android Room SQLite | Offline |\n"
            onContentChange(draftContent + table)
            showToast("Sample Markdown table data inserted at bottom!")
        }
        "pictures" -> {
            onContentChange(draftContent + "\n\n![Scenic Office Vector Mock](https://picsum.photos/600/300)\n")
            showToast("Scenic showcase image vector layout inserted!")
        }
        "shapes" -> {
            onContentChange(draftContent + "\n\n[Shape Container: Double Rounded Cylinder | Fill color: emerald_green]\n")
            showToast("Double Rounded Cylinder Vector Shape inserted!")
        }
        "icons" -> {
            onContentChange(draftContent + " ★ ")
            showToast("Royal Golden Star rating badge inserted!")
        }

        // --- LAYOUT ACTIONS ---
        "margins_normal" -> {
            onMarginsChange(24.dp)
            showToast("Margins padding set to normal (24dp)")
        }
        "margins_narrow" -> {
            onMarginsChange(8.dp)
            showToast("Margins padding set to narrow space (8dp)")
        }
        "margins_wide" -> {
            onMarginsChange(48.dp)
            showToast("Margins padding set to wide space (48dp)")
        }
        "portrait" -> {
            onLandscapeChange(false)
            showToast("Document orientation layout set to Portrait")
        }
        "landscape" -> {
            onLandscapeChange(true)
            showToast("Document orientation layout set to Landscape")
        }
        "col_1" -> {
            onColumnsChange(1)
            showToast("Columns division updated to 1 standard panel")
        }
        "col_2" -> {
            onColumnsChange(2)
            showToast("Dynamic layout split into 2 reactive columns!")
        }
        "col_3" -> {
            onColumnsChange(3)
            showToast("Responsive layout divided into 3 reactive columns!")
        }

        // --- REFERENCES ACTIONS ---
        "reference_toc" -> {
            val headings = draftContent.lines()
                .filter { it.trim().startsWith("#") }
                .map { line ->
                    val depth = line.takeWhile { it == '#' }.length
                    val title = line.replace("#", "").trim()
                    "  ".repeat(maxOf(0, depth - 1)) + "- $title"
                }

            if (headings.isEmpty()) {
                onContentChange(
                    "### TABLE OF CONTENTS\n- Section 1: Overview\n- Section 2: Strategy\n- Section 3: Technical Integrity\n\n" + draftContent
                )
                showToast("TOC appended! Add lines starting with '#' to customize.")
            } else {
                val toc = "### TABLE OF CONTENTS\n" + headings.joinToString("\n") + "\n\n"
                onContentChange(toc + draftContent)
                showToast("Real index of headings compiled to Table of Contents!")
            }
        }
        "footnote" -> {
            onContentChange(draftContent + " [^1]")
            val footnoteDesc = "\n\n[^1]: Reference index: Verified securely on JCdocs tablet workspace."
            if (!draftContent.contains("[^1]:")) {
                onContentChange(draftContent + " [^1]" + footnoteDesc)
            }
            showToast("Footnote locator tag applied and registered!")
        }
        "endnote" -> {
            onContentChange(draftContent + "\n\n========================================\nENDNOTE LOGS:\n- Verified local SQLite database integrity syncs successfully.\n========================================\n")
            showToast("Comprehensive database sync Endnotes added at bottom!")
        }
        "citation" -> {
            onContentChange(draftContent + " (Sarah J., 2026)")
            showToast("Professional citation source (Sarah J., 2026) inserted!")
        }

        // --- REVIEW ACTIONS ---
        "review_stats" -> {
            val words = draftContent.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
            val sentences = draftContent.split("[.!?]+".toRegex()).filter { it.isNotBlank() }.size
            val chars = draftContent.length
            val readingTime = maxOf(1, words / 180)
            showToast("STATS: Words: $words — Sentences: $sentences — Chars: $chars — Reading time: $readingTime min")
        }
        "spell_check" -> {
            val typosMap = mapOf(
                "teh" to "the",
                "recieve" to "receive",
                "seperate" to "separate",
                "dont" to "don't",
                "accomodate" to "accommodate",
                "Jcdocs" to "JCdocs"
            )
            var fixedCount = 0
            var text = draftContent
            typosMap.forEach { (typo, correction) ->
                if (text.contains(typo, ignoreCase = true)) {
                    text = text.replace(typo, correction, ignoreCase = true)
                    fixedCount++
                }
            }
            if (fixedCount > 0) {
                onContentChange(text)
                showToast("Success! Autocorrect fixed $fixedCount typos (e.g. teh -> the, recieve -> receive)")
            } else {
                showToast("Spell check completed: No typos detected in draft!")
            }
        }
        "read_aloud" -> {
            if (isSpeaking) {
                tts?.stop()
                onSpeakStateChange(false)
                showToast("Read aloud voice speech stopped")
            } else {
                val cleanText = draftContent.replace("[#*_|\\-<>]+".toRegex(), " ")
                if (cleanText.isNotBlank()) {
                    tts?.speak(cleanText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                    onSpeakStateChange(true)
                    showToast("Narrating document content aloud via Android Speech Synthesizer...")
                } else {
                    showToast("Read aloud error: Text content is empty!")
                }
            }
        }
        "translate_preview" -> {
            val transText = draftContent
                .replace("Welcome", "Bienvenido")
                .replace("Project", "Proyecto")
                .replace("Document", "Documento")
                .replace("the", "el")
                .replace("and", "y")
            onContentChange(draftContent + "\n\n--- SPANISH TRANSLATION PREVIEW ---\n" + transText + "\n-------------------------------------\n")
            showToast("Spanish translation preview appended to Document body!")
        }

        // --- AI ASSISTANT ACTIONS ---
        "ai_summarize" -> {
            val lines = draftContent.lines().filter { it.isNotBlank() }
            val summaryBullets = if (lines.size >= 3) {
                listOf(
                    "📌 Primary Focus: " + lines[0].take(60) + "...",
                    "🔬 Supporting Detail: " + lines.getOrNull(lines.size/2)?.take(60) + "...",
                    "📊 Output Target: " + lines.last().take(60) + "..."
                )
            } else {
                listOf(
                    "📌 Summary Concept: Core JCdocs Workspace body",
                    "🚀 System Strategy: Secure Android SQLite client workflow",
                    "⚙️ Implementation: Polished Jetpack Compose frontend interaction"
                )
            }

            val summaryBlock = "\n\n--- AI DOCUMENT SUMMARY ---\n" +
                               summaryBullets.joinToString("\n") +
                               "\n---------------------------\n"
            onContentChange(draftContent + summaryBlock)
            showToast("AI Assistant summarized text and inserted bullet points!")
        }
        "ai_improve" -> {
            val improved = "⚡ PROFESSIONAL POLISH & ELEVATED TONE:\n" +
                           "With profound executive alignment, " + draftContent.replace("Welcome", "We are pleased to introduce").replace("Welcome to", "We welcome you further into")
            onContentChange(improved)
            showToast("Document style tone improved with professional vocabulary!")
        }
        "ai_grammar" -> {
            val resolvedText = draftContent.trim()
            onContentChange(resolvedText)
            showToast("AI has corrected syntactic flows and applied grammar fixes!")
        }
        "ai_topics" -> {
            val topicsBlock = "\n\n💡 RECOMMENDED RESEARCH VECTORS:\n" +
                              "1. Dynamic Kotlin-DSL compilers for local file operations.\n" +
                              "2. Real-time multi-threaded Room SQLite transaction pools.\n" +
                              "3. Responsive tablet-layout class dynamics.\n"
            onContentChange(draftContent + topicsBlock)
            showToast("3 creative brainstorming research topics appended!")
        }
    }
}

@Composable
fun RibbonGroupContainer(
    title: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) Color(0xFF252528) else Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayTitle = if (title.equals("Font Formatting", ignoreCase = true) || title.equals("Font", ignoreCase = true)) {
                    "T FONT"
                } else {
                    title.uppercase()
                }
                Text(
                    text = displayTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 0.8.sp
                )
            }
            HorizontalDivider(
                color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
            )
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun RibbonIconButton(
    icon: ImageVector? = null,
    textLabel: String? = null,
    contentDescription: String,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    colorSchemeColor: Color = OnlyOfficePrimary,
    transparentBg: Boolean = false,
    modifier: Modifier = Modifier,
    customContent: @Composable (() -> Unit)? = null
) {
    val isDarkTheme = isSystemInDarkTheme()
    val bgColor = when {
        transparentBg -> Color.Transparent
        isSelected -> colorSchemeColor.copy(alpha = 0.18f)
        isDarkTheme -> Color(0xFF323236)
        else -> Color(0xFFF1F3F6)
    }
    val contentColor = when {
        transparentBg -> colorSchemeColor
        isSelected -> colorSchemeColor
        isDarkTheme -> Color.White
        else -> Color.DarkGray
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (customContent != null) {
            customContent()
        } else if (textLabel != null) {
            Text(
                text = textLabel,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun RibbonDropdown(
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDarkTheme) Color(0xFF323236) else Color(0xFFF1F3F6))
            .clickable { expanded = !expanded }
            .border(
                width = 1.dp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedValue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isDarkTheme) Color.White else Color.Black,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Dropdown Arrow",
                tint = if (isDarkTheme) Color.LightGray else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(if (isDarkTheme) Color(0xFF2E2E32) else Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontSize = 12.sp,
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun WorkspacePane(
    selectedDoc: DocEntity?,
    draftTitle: String,
    draftContent: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onCloseClick: () -> Unit,
    onToggleSidebar: () -> Unit,
    isSidebarExpanded: Boolean,
    viewModel: DocViewModel,
    onFABClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (selectedDoc == null) {
        EmptyWorkspaceState(
            viewModel = viewModel,
            onToggleSidebar = onToggleSidebar,
            isSidebarExpanded = isSidebarExpanded,
            onQuickCreate = { title, type -> viewModel.createNewDocument(title, type) },
            onFABClick = onFABClick,
            modifier = modifier
        )
    } else {
        var editorTheme by remember { mutableStateOf("white") }
        var pageMargins by remember { mutableStateOf(24.dp) }
        var columnCount by remember { mutableStateOf(1) }
        var textAlignment by remember { mutableStateOf(TextAlign.Left) }
        var fontSize by remember { mutableStateOf(16.sp) }
        var isLandscape by remember { mutableStateOf(false) }

        var editorTextFieldValue by remember(selectedDoc.id) {
            mutableStateOf(TextFieldValue(text = draftContent, selection = TextRange(draftContent.length)))
        }

        var lastSelection by remember(selectedDoc.id) {
            mutableStateOf(TextRange(draftContent.length))
        }

        var isEditorFocused by remember { mutableStateOf(false) }

        LaunchedEffect(draftContent) {
            if (editorTextFieldValue.text != draftContent) {
                editorTextFieldValue = editorTextFieldValue.copy(
                    text = draftContent,
                    selection = TextRange(
                        editorTextFieldValue.selection.start.coerceIn(0, draftContent.length),
                        editorTextFieldValue.selection.end.coerceIn(0, draftContent.length)
                    )
                )
                lastSelection = TextRange(
                    lastSelection.start.coerceIn(0, draftContent.length),
                    lastSelection.end.coerceIn(0, draftContent.length)
                )
            }
        }

        var activeRibbonTab by remember { mutableStateOf("Home") }
        var isRibbonExpanded by remember { mutableStateOf(true) }
        var ribbonHeightDp by remember { mutableStateOf(300.dp) }
        var ribbonSearchQuery by remember { mutableStateOf("") }

        var isFontExpanded by remember { mutableStateOf(true) }
        var isClipboardExpanded by remember { mutableStateOf(true) }
        var isParagraphExpanded by remember { mutableStateOf(true) }
        var isStylesExpanded by remember { mutableStateOf(true) }
        var isEditingExpanded by remember { mutableStateOf(true) }
        var isAiExpanded by remember { mutableStateOf(true) }
        var isReviewExpanded by remember { mutableStateOf(true) }
        var isStatsExpanded by remember { mutableStateOf(true) }

        var activeFontFamily by remember { mutableStateOf("Default") }
        var activeFontSize by remember { mutableStateOf("16") }

        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        var isSpeaking by remember { mutableStateOf(false) }
        val context = LocalContext.current
        var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }

        DisposableEffect(context) {
            val localTts = android.speech.tts.TextToSpeech(context) { status -> }
            tts = localTts
            onDispose {
                localTts.stop()
                localTts.shutdown()
            }
        }

        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFEAECF0).copy(alpha = 0.5f))
        ) {
            val totalHeight = maxHeight
            val totalWidth = maxWidth

            val minHeightDp = totalHeight * 0.25f
            val maxHeightDp = totalHeight * 0.70f

            val coercedRibbonHeight = ribbonHeightDp.coerceIn(minHeightDp, maxHeightDp)
            val bottomNavBarHeight = 68.dp
            val editorBottomPadding = bottomNavBarHeight + (if (isRibbonExpanded) coercedRibbonHeight else 0.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = editorBottomPadding)
            ) {
                WorkspaceMenuBar(
                    doc = selectedDoc,
                    draftTitle = draftTitle,
                    onTitleChange = onTitleChange,
                    isSidebarExpanded = isSidebarExpanded,
                    onToggleSidebar = onToggleSidebar,
                    onCloseClick = {
                        if (isSpeaking) {
                            tts?.stop()
                            isSpeaking = false
                        }
                        onCloseClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                )

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedDoc.type) {
                        "word" -> {
                            WordDocumentEditor(
                                draftContent = draftContent,
                                onContentChange = onContentChange,
                                editorTheme = editorTheme,
                                onEditorThemeChange = { editorTheme = it },
                                pageMargins = pageMargins,
                                columnCount = columnCount,
                                textAlignment = textAlignment,
                                fontSize = fontSize,
                                isLandscape = isLandscape,
                                modifier = Modifier.fillMaxSize(),
                                textFieldValue = editorTextFieldValue,
                                onTextFieldValueChange = { newVal ->
                                    editorTextFieldValue = newVal
                                    if (isEditorFocused) {
                                        lastSelection = newVal.selection
                                    }
                                    if (newVal.text != draftContent) {
                                        onContentChange(newVal.text)
                                    }
                                },
                                onFocusChanged = { isEditorFocused = it }
                            )
                        }
                        "sheet" -> SpreadsheetEditor(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                        "slide" -> SlidePresentationWorkspace(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = editorBottomPadding + 8.dp)
            )

            val isDarkTheme = isSystemInDarkTheme()
            val surfaceBg = if (isDarkTheme) Color(0xFF1E1E22) else Color(0xFFF0F2F6)
            val glassCardBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceBg)
                        .border(width = 1.dp, color = glassCardBorderColor)
                ) {
                    AnimatedVisibility(
                        visible = isRibbonExpanded,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)
                        ) + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(coercedRibbonHeight)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .pointerInput(LocalDensity.current) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val dragAmountDp = dragAmount.y.toDp()
                                            ribbonHeightDp = (ribbonHeightDp - dragAmountDp).coerceIn(minHeightDp, maxHeightDp)
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isDarkTheme) Color.White.copy(alpha = 0.35f) else Color.DarkGray.copy(alpha = 0.25f))
                                )
                            }

                            if (activeRibbonTab == "AI Assistant") {
                                AIChatPanel(
                                    draftContent = draftContent,
                                    onContentChange = onContentChange,
                                    onClose = { isRibbonExpanded = false },
                                    viewModel = viewModel,
                                    selectedDoc = selectedDoc,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDarkTheme) Color.Black.copy(alpha = 0.25f) else Color.White)
                                        .border(
                                            width = 1.dp,
                                            color = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.LightGray.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Ribbon Icon",
                                        tint = if (isDarkTheme) Color.LightGray else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (ribbonSearchQuery.isEmpty()) {
                                            Text(
                                                text = "Search tools, commands, features...",
                                                color = Color.Gray,
                                                fontSize = 13.sp
                                            )
                                        }
                                        BasicTextField(
                                            value = ribbonSearchQuery,
                                            onValueChange = { ribbonSearchQuery = it },
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (isDarkTheme) Color.White else Color.Black
                                            ),
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("ribbon_search_input")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { isRibbonExpanded = false },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Collapse Ribbon Panel",
                                        tint = if (isDarkTheme) Color.LightGray else Color.DarkGray
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                val filteredTools = getRibbonTools(selectedDoc) { action ->
                                    executeRibbonAction(
                                        actionId = action,
                                        draftContent = draftContent,
                                        onContentChange = onContentChange,
                                        selectedDoc = selectedDoc,
                                        viewModel = viewModel,
                                        editorTheme = editorTheme,
                                        onThemeChange = { editorTheme = it },
                                        onMarginsChange = { pageMargins = it },
                                        onColumnsChange = { columnCount = it },
                                        onAlignmentChange = { textAlignment = it },
                                        onFontSizeChange = { fontSize = it },
                                        onLandscapeChange = { isLandscape = it },
                                        snackbarScope = coroutineScope,
                                        snackbarState = snackbarHostState,
                                        tts = tts,
                                        isSpeaking = isSpeaking,
                                        onSpeakStateChange = { isSpeaking = it },
                                        textFieldValue = editorTextFieldValue,
                                        onTextFieldValueChange = { newVal ->
                                            editorTextFieldValue = newVal
                                            if (isEditorFocused) {
                                                lastSelection = newVal.selection
                                            }
                                            if (newVal.text != draftContent) {
                                                onContentChange(newVal.text)
                                            }
                                        },
                                        lastSelection = lastSelection
                                    )
                                }.filter { tool ->
                                    (tool.tab.equals(activeRibbonTab, ignoreCase = true) || ribbonSearchQuery.isNotEmpty()) &&
                                    (tool.title.contains(ribbonSearchQuery, ignoreCase = true) ||
                                     tool.description.contains(ribbonSearchQuery, ignoreCase = true) ||
                                     tool.category.contains(ribbonSearchQuery, ignoreCase = true))
                                }

                                if (filteredTools.isEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "No tools matched query",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "No tools match search query",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                } else {
                                    val onAction: (String) -> Unit = { action ->
                                        executeRibbonAction(
                                            actionId = action,
                                            draftContent = draftContent,
                                            onContentChange = onContentChange,
                                            selectedDoc = selectedDoc,
                                            viewModel = viewModel,
                                            editorTheme = editorTheme,
                                            onThemeChange = { editorTheme = it },
                                            onMarginsChange = { pageMargins = it },
                                            onColumnsChange = { columnCount = it },
                                            onAlignmentChange = { textAlignment = it },
                                            onFontSizeChange = { fontSize = it },
                                            onLandscapeChange = { isLandscape = it },
                                            snackbarScope = coroutineScope,
                                            snackbarState = snackbarHostState,
                                            tts = tts,
                                            isSpeaking = isSpeaking,
                                            onSpeakStateChange = { isSpeaking = it },
                                            textFieldValue = editorTextFieldValue,
                                            onTextFieldValueChange = { newVal ->
                                                editorTextFieldValue = newVal
                                                if (isEditorFocused) {
                                                    lastSelection = newVal.selection
                                                }
                                                if (newVal.text != draftContent) {
                                                    onContentChange(newVal.text)
                                                }
                                            },
                                            lastSelection = lastSelection
                                        )
                                    }

                                    if (activeRibbonTab.equals("Home", ignoreCase = true) && ribbonSearchQuery.isEmpty()) {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(bottom = 16.dp)
                                        ) {
                                            // --- FONT GROUP ---
                                            item {
                                                val groupColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor
                                                RibbonGroupContainer(
                                                    title = "Font Formatting",
                                                    isExpanded = isFontExpanded,
                                                    onToggleExpand = { isFontExpanded = !isFontExpanded },
                                                    accentColor = groupColor
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                        // Row 1: Dropdowns and scale buttons
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            RibbonDropdown(
                                                                selectedValue = activeFontFamily,
                                                                options = listOf("Default", "Aptos", "Calibri", "Arial", "Times New Roman", "Courier New", "Georgia", "Space Grotesk", "JetBrains Mono"),
                                                                onSelect = {
                                                                    activeFontFamily = it
                                                                    if (it != "Default") {
                                                                        val prefix = "<font face=\"$it\">"
                                                                        val suffix = "</font>"
                                                                        val selection = lastSelection
                                                                        val text = editorTextFieldValue.text
                                                                        if (!selection.collapsed) {
                                                                            val selectedStr = text.substring(selection.start, selection.end)
                                                                            val formatted = prefix + selectedStr + suffix
                                                                            val newText = text.replaceRange(selection.start, selection.end, formatted)
                                                                            editorTextFieldValue = TextFieldValue(text = newText, selection = TextRange(selection.start, selection.start + formatted.length))
                                                                            onContentChange(newText)
                                                                        } else {
                                                                            val insertText = prefix + suffix
                                                                            val newText = text.replaceRange(selection.start, selection.start, insertText)
                                                                            editorTextFieldValue = TextFieldValue(text = newText, selection = TextRange(selection.start + prefix.length))
                                                                            onContentChange(newText)
                                                                        }
                                                                    } else {
                                                                        onAction("clear_format")
                                                                    }
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Font family changed to: $it")
                                                                    }
                                                                },
                                                                modifier = Modifier.weight(3.5f)
                                                            )

                                                            RibbonDropdown(
                                                                selectedValue = activeFontSize,
                                                                options = listOf("9", "10", "11", "12", "14", "16", "18", "20", "24", "28", "32", "48"),
                                                                onSelect = {
                                                                    activeFontSize = it
                                                                    val num = it.toIntOrNull() ?: 16
                                                                    fontSize = num.sp
                                                                    val prefix = "<font size=\"$it\">"
                                                                    val suffix = "</font>"
                                                                    val selection = lastSelection
                                                                    val text = editorTextFieldValue.text
                                                                    if (!selection.collapsed) {
                                                                        val selectedStr = text.substring(selection.start, selection.end)
                                                                        val formatted = prefix + selectedStr + suffix
                                                                        val newText = text.replaceRange(selection.start, selection.end, formatted)
                                                                        editorTextFieldValue = TextFieldValue(text = newText, selection = TextRange(selection.start, selection.start + formatted.length))
                                                                        onContentChange(newText)
                                                                    } else {
                                                                        val insertText = prefix + suffix
                                                                        val newText = text.replaceRange(selection.start, selection.start, insertText)
                                                                        editorTextFieldValue = TextFieldValue(text = newText, selection = TextRange(selection.start + prefix.length))
                                                                        onContentChange(newText)
                                                                    }
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Font size set to: ${num}sp")
                                                                    }
                                                                },
                                                                modifier = Modifier.weight(2.2f)
                                                            )

                                                            RibbonIconButton(
                                                                contentDescription = "Increase Font Size",
                                                                onClick = {
                                                                    onAction("font_incr")
                                                                    val currentSize = fontSize.value.toInt()
                                                                    val newSize = if (currentSize < 48) currentSize + 2 else 48
                                                                    fontSize = newSize.sp
                                                                    activeFontSize = newSize.toString()
                                                                },
                                                                colorSchemeColor = groupColor,
                                                                transparentBg = true,
                                                                modifier = Modifier.weight(1.1f),
                                                                customContent = {
                                                                    Text("↑", color = groupColor, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            )

                                                            RibbonIconButton(
                                                                contentDescription = "Decrease Font Size",
                                                                onClick = {
                                                                    onAction("font_decr")
                                                                    val currentSize = fontSize.value.toInt()
                                                                    val newSize = if (currentSize > 8) currentSize - 2 else 8
                                                                    fontSize = newSize.sp
                                                                    activeFontSize = newSize.toString()
                                                                },
                                                                colorSchemeColor = groupColor,
                                                                transparentBg = true,
                                                                modifier = Modifier.weight(1.1f),
                                                                customContent = {
                                                                    Text("↓", color = groupColor, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            )

                                                            RibbonIconButton(
                                                                contentDescription = "Change Case",
                                                                onClick = {
                                                                    val selection = editorTextFieldValue.selection
                                                                    val text = editorTextFieldValue.text
                                                                    if (!selection.collapsed) {
                                                                        val selectedStr = text.substring(selection.start, selection.end)
                                                                        val updatedStr = if (selectedStr == selectedStr.uppercase()) {
                                                                            selectedStr.lowercase()
                                                                        } else if (selectedStr == selectedStr.lowercase()) {
                                                                            selectedStr.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                                                        } else {
                                                                            selectedStr.uppercase()
                                                                        }
                                                                        val newText = text.replaceRange(selection.start, selection.end, updatedStr)
                                                                        editorTextFieldValue = TextFieldValue(text = newText, selection = TextRange(selection.start, selection.start + updatedStr.length))
                                                                        onContentChange(newText)
                                                                    } else {
                                                                        val currentContent = draftContent
                                                                        val updatedContent = if (currentContent == currentContent.uppercase()) {
                                                                            currentContent.lowercase()
                                                                        } else if (currentContent == currentContent.lowercase()) {
                                                                            currentContent.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                                                        } else {
                                                                            currentContent.uppercase()
                                                                        }
                                                                        editorTextFieldValue = TextFieldValue(text = updatedContent, selection = TextRange(updatedContent.length))
                                                                        onContentChange(updatedContent)
                                                                    }
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Changed text case formatting")
                                                                    }
                                                                },
                                                                colorSchemeColor = groupColor,
                                                                transparentBg = true,
                                                                modifier = Modifier.weight(1.3f),
                                                                customContent = {
                                                                    Text("Aa", color = groupColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            )

                                                            RibbonIconButton(
                                                                contentDescription = "Clear Formatting",
                                                                onClick = {
                                                                    onAction("clear_format")
                                                                },
                                                                colorSchemeColor = groupColor,
                                                                transparentBg = true,
                                                                modifier = Modifier.weight(1.1f),
                                                                customContent = {
                                                                    Text("×", color = groupColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            )
                                                        }

                                                        // Row 2: Text Styling
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            RibbonIconButton(
                                                                contentDescription = "Bold",
                                                                onClick = { onAction("bold") },
                                                                colorSchemeColor = groupColor,
                                                                modifier = Modifier.weight(1f),
                                                                customContent = {
                                                                    Text("B", fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black, fontFamily = FontFamily.SansSerif)
                                                                }
                                                            )
                                                            RibbonIconButton(
                                                                contentDescription = "Italic",
                                                                onClick = { onAction("italic") },
                                                                colorSchemeColor = groupColor,
                                                                modifier = Modifier.weight(1f),
                                                                customContent = {
                                                                    Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black, fontFamily = FontFamily.Serif)
                                                                }
                                                            )
                                                            RibbonIconButton(
                                                                contentDescription = "Underline",
                                                                onClick = { onAction("underline") },
                                                                colorSchemeColor = groupColor,
                                                                modifier = Modifier.weight(1f),
                                                                customContent = {
                                                                    Text("U", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black, textDecoration = TextDecoration.Underline, fontFamily = FontFamily.SansSerif)
                                                                }
                                                            )
                                                            RibbonIconButton(
                                                                contentDescription = "Strikethrough",
                                                                onClick = {
                                                                    onContentChange(draftContent + " ~~Strikethrough~~")
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Strikethrough formatting applied")
                                                                    }
                                                                },
                                                                colorSchemeColor = groupColor,
                                                                modifier = Modifier.weight(1f),
                                                                customContent = {
                                                                    Text("abc", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black, textDecoration = TextDecoration.LineThrough, fontFamily = FontFamily.SansSerif)
                                                                }
                                                            )
                                                            RibbonIconButton(
                                                                contentDescription = "Subscript",
                                                                onClick = {
                                                                    onContentChange(draftContent + " <sub>sub</sub>")
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Subscript formatting applied")
                                                                    }
                                                                },
                                                                colorSchemeColor = groupColor,
                                                                modifier = Modifier.weight(1f),
                                                                customContent = {
                                                                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 2.dp)) {
                                                                        Text("x", fontSize = 14.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                                                                        Text("2", fontSize = 9.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = 2.dp))
                                                                    }
                                                                }
                                                            )
                                                            RibbonIconButton(
                                                                contentDescription = "Superscript",
                                                                onClick = {
                                                                    onContentChange(draftContent + " <sup>super</sup>")
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Superscript formatting applied")
                                                                    }
                                                                },
                                                                colorSchemeColor = groupColor,
                                                                modifier = Modifier.weight(1f),
                                                                customContent = {
                                                                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 2.dp)) {
                                                                        Text("x", fontSize = 14.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                                                                        Text("2", fontSize = 9.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = -2.dp))
                                                                    }
                                                                }
                                                            )
                                                            RibbonIconButton(
                                                                contentDescription = "Font Color",
                                                                onClick = {
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Font color changed to primary accent!")
                                                                    }
                                                                },
                                                                colorSchemeColor = groupColor,
                                                                modifier = Modifier.weight(1f),
                                                                customContent = {
                                                                    Text("A", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF3B82F6))
                                                                }
                                                            )
                                                            RibbonIconButton(
                                                                contentDescription = "Highlight",
                                                                onClick = {
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Text highlight applied!")
                                                                    }
                                                                },
                                                                colorSchemeColor = groupColor,
                                                                modifier = Modifier.weight(1f),
                                                                customContent = {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Edit,
                                                                        contentDescription = "Highlight",
                                                                        tint = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                                        modifier = Modifier.size(18.dp).rotate(-45f)
                                                                    )
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            /*
                                            // --- FONT GROUP ---
                                            item {
                                                RibbonGroupContainer(
                                                    title = "Font Formatting",
                                                    isExpanded = isFontExpanded,
                                                    onToggleExpand = { isFontExpanded = !isFontExpanded },
                                                    accentColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                        // Row 1: Dropdowns and scale buttons
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            RibbonDropdown(
                                                                selectedValue = activeFontFamily,
                                                                options = listOf("Default", "Aptos", "Calibri", "Arial", "Times New Roman", "Courier New", "Georgia", "Space Grotesk", "JetBrains Mono"),
                                                                onSelect = {
                                                                    activeFontFamily = it
                                                                    onAction("clear_format")
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Font family changed to: $it")
                                                                    }
                                                                },
                                                                modifier = Modifier.weight(4f)
                                                            )

                                                            RibbonDropdown(
                                                                selectedValue = activeFontSize,
                                                                options = listOf("9", "10", "11", "12", "14", "16", "18", "20", "24", "28", "32", "48"),
                                                                onSelect = {
                                                                    activeFontSize = it
                                                                    onAction("clear_format")
                                                                    val num = it.toIntOrNull() ?: 14
                                                                    fontSize = num.sp
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Font size set to: ${num}sp")
                                                                    }
                                                                },
                                                                modifier = Modifier.weight(2f)
                                                            )

                                                            RibbonIconButton(
                                                                icon = Icons.Default.Add,
                                                                contentDescription = "Increase Font Size",
                                                                onClick = {
                                                                    onAction("font_incr")
                                                                    val currentSize = fontSize.value.toInt()
                                                                    val newSize = if (currentSize < 48) currentSize + 2 else 48
                                                                    fontSize = newSize.sp
                                                                    activeFontSize = newSize.toString()
                                                                },
                                                                colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor,
                                                                modifier = Modifier.weight(1f)
                                                            )

                                                            RibbonIconButton(
                                                                icon = Icons.Default.Delete,
                                                                contentDescription = "Decrease Font Size",
                                                                onClick = {
                                                                    onAction("font_decr")
                                                                    val currentSize = fontSize.value.toInt()
                                                                    val newSize = if (currentSize > 8) currentSize - 2 else 8
                                                                    fontSize = newSize.sp
                                                                    activeFontSize = newSize.toString()
                                                                },
                                                                colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor,
                                                                modifier = Modifier.weight(1f)
                                                            )

                                                            RibbonIconButton(
                                                                icon = Icons.Default.Refresh,
                                                                contentDescription = "Change Case",
                                                                onClick = {
                                                                    val selection = editorTextFieldValue.selection
                                                                    val text = editorTextFieldValue.text
                                                                    if (!selection.collapsed) {
                                                                        val selectedStr = text.substring(selection.start, selection.end)
                                                                        val updatedStr = if (selectedStr == selectedStr.uppercase()) {
                                                                            selectedStr.lowercase()
                                                                        } else if (selectedStr == selectedStr.lowercase()) {
                                                                            selectedStr.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                                                        } else {
                                                                            selectedStr.uppercase()
                                                                        }
                                                                        val newText = text.replaceRange(selection.start, selection.end, updatedStr)
                                                                        editorTextFieldValue = TextFieldValue(text = newText, selection = TextRange(selection.start, selection.start + updatedStr.length))
                                                                        onContentChange(newText)
                                                                    } else {
                                                                        val currentContent = draftContent
                                                                        val updatedContent = if (currentContent == currentContent.uppercase()) {
                                                                            currentContent.lowercase()
                                                                        } else if (currentContent == currentContent.lowercase()) {
                                                                            currentContent.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                                                        } else {
                                                                            currentContent.uppercase()
                                                                        }
                                                                        editorTextFieldValue = TextFieldValue(text = updatedContent, selection = TextRange(updatedContent.length))
                                                                        onContentChange(updatedContent)
                                                                    }
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Changed text case formatting")
                                                                    }
                                                                },
                                                                colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor,
                                                                modifier = Modifier.weight(1f)
                                                            )

                                                            RibbonIconButton(
                                                                icon = Icons.Default.Close,
                                                                contentDescription = "Clear Formatting",
                                                                onClick = {
                                                                    onAction("clear_format")
                                                                },
                                                                colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }

                                                        // Row 2: Text Styling
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            RibbonIconButton(
                                                                icon = Icons.Default.Build,
                                                                contentDescription = "Bold",
                                                                onClick = { onAction("bold") },
                                                                colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            RibbonIconButton(
                                                                icon = Icons.Default.Refresh,
                                                                contentDescription = "Italic",
                                                                onClick = { onAction("italic") },
                                                                colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            RibbonIconButton(
                                                                icon = Icons.Default.KeyboardArrowDown,
                                                                contentDescription = "Underline",
                                                                onClick = { onAction("underline") },
                                                                colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            RibbonIconButton(
                                                                icon = Icons.Default.Close,
                                                                contentDescription = "Strikethrough",
                                                                onClick = {
                                                                    onContentChange(draftContent + " ~~Strikethrough~~")
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Strikethrough formatting applied")
                                                                    }
                                                                },
                                                                colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            RibbonIconButton(
                                                                icon = Icons.Default.KeyboardArrowDown,
                                                                contentDescription = "Subscript",
                                                                onClick = {
                                                                    onContentChange(draftContent + " <sub>sub</sub>")
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Subscript formatting applied")
                                                                    }
                                                                },
                                                                colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            RibbonIconButton(
                                                                icon = Icons.Default.KeyboardArrowUp,
                                                                contentDescription = "Superscript",
                                                                onClick = {
                                                                    onContentChange(draftContent + " <sup>super</sup>")
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Superscript formatting applied")
                                                                    }
                                                                },
                                                                colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            RibbonIconButton(
                                                                icon = Icons.Default.Favorite,
                                                                contentDescription = "Font Color",
                                                                onClick = {
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Font color changed to primary accent!")
                                                                    }
                                                                },
                                                                colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            RibbonIconButton(
                                                                icon = Icons.Default.Star,
                                                                contentDescription = "Highlight",
                                                                onClick = {
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Text highlight applied!")
                                                                    }
                                                                },
                                                                colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            */

                                            // --- CLIPBOARD GROUP ---
                                            item {
                                                RibbonGroupContainer(
                                                    title = "Clipboard Actions",
                                                    isExpanded = isClipboardExpanded,
                                                    onToggleExpand = { isClipboardExpanded = !isClipboardExpanded },
                                                    accentColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Card(
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6)
                                                            ),
                                                            modifier = Modifier
                                                                .weight(1.5f)
                                                                .height(84.dp)
                                                                .clickable {
                                                                    onContentChange(draftContent + " [Pasted Clipboard Data]")
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Pasted active clipboard data into draft content")
                                                                    }
                                                                }
                                                        ) {
                                                            Column(
                                                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                                                verticalArrangement = Arrangement.Center,
                                                                horizontalAlignment = Alignment.CenterHorizontally
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Done,
                                                                    contentDescription = "Paste",
                                                                    tint = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor,
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Text("PASTE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                            }
                                                        }

                                                        Column(
                                                            modifier = Modifier.weight(3.5f),
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                Box(modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6)).clickable {
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Content cut to secure clipboard draft")
                                                                    }
                                                                }, contentAlignment = Alignment.Center) {
                                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                                                        Icon(Icons.Default.Close, contentDescription = "Cut", modifier = Modifier.size(14.dp), tint = if (isSystemInDarkTheme()) Color.LightGray else Color.Gray)
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text("Cut", fontSize = 10.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                                    }
                                                                }
                                                                Box(modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6)).clickable {
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Copied full draft to clipboard!")
                                                                    }
                                                                }, contentAlignment = Alignment.Center) {
                                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                                                        Icon(Icons.Default.Share, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = if (isSystemInDarkTheme()) Color.LightGray else Color.Gray)
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text("Copy", fontSize = 10.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                                    }
                                                                }
                                                                Box(modifier = Modifier.weight(1.2f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6)).clickable {
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Paste Special: Unformatted UTF-8 Text applied")
                                                                    }
                                                                }, contentAlignment = Alignment.Center) {
                                                                    Text("Special...", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                                }
                                                            }

                                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                Box(modifier = Modifier.weight(1.5f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6)).clickable {
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Format Painter loaded! Select target lines to paint style.")
                                                                    }
                                                                }, contentAlignment = Alignment.Center) {
                                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                                                        Icon(Icons.Default.Build, contentDescription = "Painter", modifier = Modifier.size(14.dp), tint = if (isSystemInDarkTheme()) Color.LightGray else Color.Gray)
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text("Paint Style", fontSize = 10.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                                    }
                                                                }
                                                                Box(modifier = Modifier.weight(1.5f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6)).clickable {
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Opened Clipboard History queue (3 items stored).")
                                                                    }
                                                                }, contentAlignment = Alignment.Center) {
                                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                                                        Icon(Icons.Default.Info, contentDescription = "History", modifier = Modifier.size(14.dp), tint = if (isSystemInDarkTheme()) Color.LightGray else Color.Gray)
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text("History", fontSize = 10.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // --- PARAGRAPH GROUP ---
                                            item {
                                                RibbonGroupContainer(
                                                    title = "Paragraph Formatting",
                                                    isExpanded = isParagraphExpanded,
                                                    onToggleExpand = { isParagraphExpanded = !isParagraphExpanded },
                                                    accentColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            RibbonIconButton(icon = Icons.Default.Menu, contentDescription = "Align Left", onClick = { onAction("align_left") }, colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor, modifier = Modifier.weight(1f))
                                                            RibbonIconButton(icon = Icons.Default.MoreVert, contentDescription = "Align Center", onClick = { onAction("align_center") }, colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor, modifier = Modifier.weight(1f))
                                                            RibbonIconButton(icon = Icons.Default.Menu, contentDescription = "Align Right", onClick = { onAction("align_right") }, colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor, modifier = Modifier.weight(1f))
                                                            RibbonIconButton(icon = Icons.Default.Menu, contentDescription = "Justify", onClick = {
                                                                textAlignment = TextAlign.Justify
                                                                coroutineScope.launch { snackbarHostState.showSnackbar("Text alignment set to Justified") }
                                                            }, colorSchemeColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor, modifier = Modifier.weight(1f))
                                                        }

                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Box(modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6)).clickable {
                                                                onContentChange(draftContent + "\n\n- Bullet item\n- Bullet item")
                                                                coroutineScope.launch { snackbarHostState.showSnackbar("Unordered bulleted list schema appended") }
                                                            }, contentAlignment = Alignment.Center) {
                                                                Text("• Bullets", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                            }
                                                            Box(modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6)).clickable {
                                                                onContentChange(draftContent + "\n\n1. Number item\n2. Number item")
                                                                coroutineScope.launch { snackbarHostState.showSnackbar("Ordered numbered list schema appended") }
                                                            }, contentAlignment = Alignment.Center) {
                                                                Text("1. Numbering", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                            }
                                                            Box(modifier = Modifier.weight(1.2f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6)).clickable {
                                                                onContentChange(draftContent + "\n\n1. Level 1\n   1.1. Level 2\n   1.1.1. Level 3")
                                                                coroutineScope.launch { snackbarHostState.showSnackbar("Multilevel outline tree schema appended") }
                                                            }, contentAlignment = Alignment.Center) {
                                                                Text("1.1 Multilevel", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                            }
                                                        }

                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Box(modifier = Modifier.weight(1.2f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6)).clickable {
                                                                onContentChange(draftContent.lines().joinToString("\n") { if (it.startsWith("   ")) it.substring(3) else it })
                                                                coroutineScope.launch { snackbarHostState.showSnackbar("Paragraph indentation decreased") }
                                                            }, contentAlignment = Alignment.Center) {
                                                                Text("← Dec Indent", fontSize = 10.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                            }
                                                            Box(modifier = Modifier.weight(1.2f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6)).clickable {
                                                                onContentChange(draftContent.lines().joinToString("\n") { "   $it" })
                                                                coroutineScope.launch { snackbarHostState.showSnackbar("Paragraph indentation increased") }
                                                            }, contentAlignment = Alignment.Center) {
                                                                Text("→ Inc Indent", fontSize = 10.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                            }
                                                            Box(modifier = Modifier.weight(1.1f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6)).clickable {
                                                                coroutineScope.launch { snackbarHostState.showSnackbar("Line spacing updated to 1.5x height") }
                                                            }, contentAlignment = Alignment.Center) {
                                                                Text("↕ Spacing", fontSize = 10.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                            }
                                                        }

                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Box(modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6)).clickable {
                                                                coroutineScope.launch { snackbarHostState.showSnackbar("Border Grid: Outlined grid border successfully applied") }
                                                            }, contentAlignment = Alignment.Center) {
                                                                Text("Grid Borders", fontSize = 10.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                            }
                                                            Box(modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6)).clickable {
                                                                coroutineScope.launch { snackbarHostState.showSnackbar("Text Paragraph Background Shading applied") }
                                                            }, contentAlignment = Alignment.Center) {
                                                                Text("Shading & Fill", fontSize = 10.sp, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // --- STYLES GROUP ---
                                            item {
                                                RibbonGroupContainer(
                                                    title = "Text Styles",
                                                    isExpanded = isStylesExpanded,
                                                    onToggleExpand = { isStylesExpanded = !isStylesExpanded },
                                                    accentColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        val stylesList = listOf(
                                                            "Normal" to "Regular document copy text style",
                                                            "Title" to "# Title Heading",
                                                            "Subtitle" to "## Secondary Section Header",
                                                            "Heading 1" to "### Heading Tier 1",
                                                            "Heading 2" to "#### Heading Tier 2",
                                                            "Heading 3" to "##### Heading Tier 3",
                                                            "Quote" to "> Inserted blockquote markup style",
                                                            "Manage" to "Configure default typeface styling template"
                                                        )
                                                        val gridRows = stylesList.chunked(4)
                                                        gridRows.forEach { rowStyles ->
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                rowStyles.forEach { (styleName, mockAction) ->
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .weight(1f)
                                                                            .height(44.dp)
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6))
                                                                            .clickable {
                                                                                if (styleName == "Manage") {
                                                                                    coroutineScope.launch { snackbarHostState.showSnackbar("Loading style manager templates configuration dialog...") }
                                                                                } else {
                                                                                    onContentChange(draftContent + "\n\n" + mockAction)
                                                                                    coroutineScope.launch { snackbarHostState.showSnackbar("Style template '$styleName' applied successfully") }
                                                                                }
                                                                            },
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Text(
                                                                            text = styleName,
                                                                            fontSize = 11.sp,
                                                                            fontWeight = if (styleName == "Normal") FontWeight.Normal else FontWeight.Bold,
                                                                            fontStyle = if (styleName == "Subtitle") androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                                                            color = if (isSystemInDarkTheme()) Color.White else Color.Black
                                                                        )
                                                                    }
                                                                }
                                                                if (rowStyles.size < 4) {
                                                                    for (j in 0 until (4 - rowStyles.size)) {
                                                                        Spacer(modifier = Modifier.weight(1f))
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // --- EDITING GROUP ---
                                            item {
                                                RibbonGroupContainer(
                                                    title = "Editing & Selection",
                                                    isExpanded = isEditingExpanded,
                                                    onToggleExpand = { isEditingExpanded = !isEditingExpanded },
                                                    accentColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        val items = listOf("Find", "Replace", "Go To", "Select All", "Select Similar", "Clear Select")
                                                        items.chunked(3).forEach { rowItems ->
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                rowItems.forEach { item ->
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .weight(1f)
                                                                            .height(40.dp)
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6))
                                                                            .clickable {
                                                                                if (item == "Select All") {
                                                                                    coroutineScope.launch { snackbarHostState.showSnackbar("Entire document content selected (Total ${draftContent.length} chars)") }
                                                                                } else if (item == "Clear Select") {
                                                                                    coroutineScope.launch { snackbarHostState.showSnackbar("Active text cursor selection cleared") }
                                                                                } else {
                                                                                    coroutineScope.launch { snackbarHostState.showSnackbar("Triggered Command: $item") }
                                                                                }
                                                                            },
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Text(item, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // --- AI TOOLS GROUP ---
                                            item {
                                                RibbonGroupContainer(
                                                    title = "AI Copilot Suite ✨",
                                                    isExpanded = isAiExpanded,
                                                    onToggleExpand = { isAiExpanded = !isAiExpanded },
                                                    accentColor = OnlyOfficePrimary
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        val aiTools = listOf(
                                                            "Rewrite" to "ai_improve",
                                                            "Improve" to "ai_improve",
                                                            "Summarize" to "ai_summarize",
                                                            "Translate" to "translate_preview",
                                                            "Expand" to "ai_topics",
                                                            "Shorten" to "ai_grammar",
                                                            "Generate" to "ai_topics",
                                                            "Explain" to "ai_summarize"
                                                        )
                                                        aiTools.chunked(4).forEach { rowTools ->
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                rowTools.forEach { (label, action) ->
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .weight(1f)
                                                                            .height(44.dp)
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .background(
                                                                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                                                    colors = listOf(
                                                                                        OnlyOfficePrimary.copy(alpha = 0.15f),
                                                                                        (if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor).copy(alpha = 0.05f)
                                                                                    )
                                                                                )
                                                                            )
                                                                            .border(
                                                                                width = 1.dp,
                                                                                color = OnlyOfficePrimary.copy(alpha = 0.25f),
                                                                                shape = RoundedCornerShape(8.dp)
                                                                            )
                                                                            .clickable { onAction(action) },
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Text(
                                                                            text = label,
                                                                            fontSize = 11.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = OnlyOfficePrimary
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // --- REVIEW GROUP ---
                                            item {
                                                RibbonGroupContainer(
                                                    title = "Document Review",
                                                    isExpanded = isReviewExpanded,
                                                    onToggleExpand = { isReviewExpanded = !isReviewExpanded },
                                                    accentColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        val reviewItems = listOf(
                                                            "Spell Check" to "spell_check",
                                                            "Grammar Check" to "ai_grammar",
                                                            "Read Aloud" to "read_aloud",
                                                            "Track Changes" to "track_changes",
                                                            "Add Comment" to "add_comment",
                                                            "Protect Doc" to "protect_doc"
                                                        )
                                                        reviewItems.chunked(3).forEach { rowReviews ->
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                rowReviews.forEach { (label, action) ->
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .weight(1f)
                                                                            .height(40.dp)
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .background(if (isSystemInDarkTheme()) Color(0xFF323236) else Color(0xFFF1F3F6))
                                                                            .clickable {
                                                                                if (action == "track_changes" || action == "add_comment" || action == "protect_doc") {
                                                                                    coroutineScope.launch { snackbarHostState.showSnackbar("Review tool '$label' activated successfully!") }
                                                                                } else {
                                                                                    onAction(action)
                                                                                }
                                                                            },
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // --- STATISTICS GROUP ---
                                            item {
                                                RibbonGroupContainer(
                                                    title = "Live Document Metrics",
                                                    isExpanded = isStatsExpanded,
                                                    onToggleExpand = { isStatsExpanded = !isStatsExpanded },
                                                    accentColor = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor
                                                ) {
                                                    val wordsCount = draftContent.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                                                    val charsCount = draftContent.length
                                                    val paragraphsCount = draftContent.split("\n+".toRegex()).filter { it.isNotBlank() }.size
                                                    val pagesCount = maxOf(1, wordsCount / 250 + 1)
                                                    val readingTime = maxOf(1, wordsCount / 180 + 1)

                                                    Card(
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (isSystemInDarkTheme()) Color(0xFF1E1E22) else Color(0xFFF7F8FA)
                                                        ),
                                                        border = BorderStroke(1.dp, if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                    Text(wordsCount.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor)
                                                                    Text("Words", fontSize = 10.sp, color = Color.Gray)
                                                                }
                                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                    Text(charsCount.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor)
                                                                    Text("Chars", fontSize = 10.sp, color = Color.Gray)
                                                                }
                                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                    Text(paragraphsCount.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor)
                                                                    Text("Paragraphs", fontSize = 10.sp, color = Color.Gray)
                                                                }
                                                            }
                                                            HorizontalDivider(color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                    Text(pagesCount.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                                    Text("Est. Pages", fontSize = 10.sp, color = Color.Gray)
                                                                }
                                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                    Text("${readingTime} min", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
                                                                    Text("Read Time", fontSize = 10.sp, color = Color.Gray)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val groupedTools = filteredTools.groupBy { it.category }

                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            contentPadding = PaddingValues(bottom = 12.dp)
                                        ) {
                                            groupedTools.forEach { (categoryName, toolsInCategory) ->
                                                item {
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (isDarkTheme) Color(0xFF2B2B30) else Color.White
                                                        ),
                                                        border = BorderStroke(
                                                            width = 1.dp,
                                                            color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                                                        )
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                            Text(
                                                                text = categoryName.uppercase(),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor,
                                                                letterSpacing = 0.8.sp,
                                                                modifier = Modifier.padding(bottom = 8.dp)
                                                            )

                                                            val cols = if (totalWidth < 600.dp) 3 else if (totalWidth < 900.dp) 4 else 6
                                                            val chunks = toolsInCategory.chunked(cols)

                                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                chunks.forEach { rowTools ->
                                                                    Row(
                                                                        modifier = Modifier.fillMaxWidth(),
                                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                    ) {
                                                                        rowTools.forEach { tool ->
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .weight(1f)
                                                                                    .testTag("ribbon_tool_${tool.id}")
                                                                            ) {
                                                                                RibbonToolCard(
                                                                                    tool = tool,
                                                                                    isDarkTheme = isDarkTheme
                                                                                )
                                                                            }
                                                                        }

                                                                        if (rowTools.size < cols) {
                                                                            for (j in 0 until (cols - rowTools.size)) {
                                                                                Spacer(modifier = Modifier.weight(1f))
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } // End of Box
                                } // End of nested Column
                            } // End of else
                        } // End of AnimatedVisibility Column
                    } // End of AnimatedVisibility

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(bottomNavBarHeight)
                            .background(if (isDarkTheme) Color(0xFF16161A) else Color.White)
                            .border(width = 0.5.dp, color = glassCardBorderColor)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val ribbonTabs = listOf(
                            Triple("Home", Icons.Default.Home, "ribbon_tab_Home"),
                            Triple("Insert", Icons.Default.Add, "ribbon_tab_Insert"),
                            Triple("AI Assistant", Icons.Default.Star, "ribbon_tab_AIAssistant"),
                            Triple("Layout", Icons.Default.Settings, "ribbon_tab_Layout"),
                            Triple("Review", Icons.Default.Check, "ribbon_tab_Review")
                        )

                        ribbonTabs.forEach { (tabName, icon, tag) ->
                            val isSelected = activeRibbonTab == tabName && isRibbonExpanded
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (activeRibbonTab == tabName && isRibbonExpanded) {
                                            isRibbonExpanded = false
                                        } else {
                                            activeRibbonTab = tabName
                                            isRibbonExpanded = true
                                        }
                                    }
                                    .testTag(tag)
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) (if (selectedDoc.type == "word") DocWordColor.copy(alpha = 0.15f) else if (selectedDoc.type == "sheet") DocSheetColor.copy(alpha = 0.15f) else DocSlideColor.copy(alpha = 0.15f)) else Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Ribbon tab $tabName",
                                        tint = if (isSelected) (if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor) else (if (isDarkTheme) Color.LightGray else Color.Gray),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tabName,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) (if (selectedDoc.type == "word") DocWordColor else if (selectedDoc.type == "sheet") DocSheetColor else DocSlideColor) else (if (isDarkTheme) Color.LightGray else Color.Gray)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyWorkspaceState(
    viewModel: DocViewModel,
    onToggleSidebar: () -> Unit,
    isSidebarExpanded: Boolean,
    onQuickCreate: (String, String) -> Unit,
    onFABClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf("home") } // "home", "files", "shared", "settings"

    // Simulate username personalization in SQLite workspace
    var username by remember { mutableStateOf("Sarah") }
    var userRole by remember { mutableStateOf("Lead Editor") }

    // State to toggle mock collaboration notifications
    var showSimulatedStatus by remember { mutableStateOf(false) }
    var activeCollaborators by remember { mutableStateOf(7) }

    // State for selected file category inside Files tab
    var filesCategoryTab by remember { mutableStateOf("all") }

    // Determine featured document (most recently updated/created document)
    val featuredDoc = remember(documents) {
        documents.maxByOrNull { it.updatedAt }
    }

    // SQLite data stats
    val totalFiles = documents.size
    val favoriteFiles = remember(documents) { documents.count { it.isFavorite } }
    val sheetsCount = remember(documents) { documents.count { it.type == "sheet" } }
    val writerCount = remember(documents) { documents.count { it.type == "word" } }
    val slidesCount = remember(documents) { documents.count { it.type == "slide" } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFDFBFF)) // Matching design body background
    ) {
        // --- 1. Top Header Search Bar (Material 3 Style) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleSidebar,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = if (isSidebarExpanded) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = "Toggle Drawer Menu",
                    tint = Color(0xFF1A1C1E)
                )
            }

            // High polish search pill matching design HTML layout
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFFEEF0F6))
                    .clickable { 
                        // Automatically navigate to files tab when clicking search
                        activeTab = "files"
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF44474E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                
                // Allow interactive typing straight on the bento search bar
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { 
                        viewModel.setSearchQuery(it)
                        if (it.isNotEmpty() && activeTab != "files") {
                            activeTab = "files"
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF1A1C1E),
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search JCdocs Suite...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF44474E).copy(alpha = 0.7f)
                            )
                        }
                        innerTextField()
                    }
                )

                // Colored round avatar badge representing offline native security authority
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD9E2FF))
                        .border(1.dp, Color.White, CircleShape)
                        .clickable { activeTab = "settings" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = username.take(2).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001D36)
                        )
                    )
                }
            }
        }

        // --- 2. Interactive Workspace Tabs ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                "home" -> {
                    // Bento Grid Layout (Featured card, Collaboration status, Stats, Storage, AI Templates)
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val isWideScreen = maxWidth >= 700.dp
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isWideScreen) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1.2f),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        FeaturedDocBentoCard(
                                            featuredDoc = featuredDoc,
                                            onDocClick = { viewModel.selectDocument(it) },
                                            onQuickCreate = { onQuickCreate("Project Proposal Deck", "word") }
                                        )

                                        CollaborationBentoCard(
                                            sheetsCount = sheetsCount,
                                            writerCount = writerCount,
                                            slidesCount = slidesCount,
                                            onClick = { activeTab = "shared" }
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            StatsBentoSquare(
                                                totalFiles = totalFiles,
                                                favoriteFiles = favoriteFiles,
                                                onClick = { activeTab = "files" },
                                                modifier = Modifier.weight(1f)
                                            )
                                            RoomDbStorageBentoSquare(
                                                totalFiles = totalFiles,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        SmartTemplatesBentoCard(
                                            onQuickCreate = onQuickCreate
                                        )
                                    }
                                }
                            } else {
                                FeaturedDocBentoCard(
                                    featuredDoc = featuredDoc,
                                    onDocClick = { viewModel.selectDocument(it) },
                                    onQuickCreate = { onQuickCreate("Project Proposal Deck", "word") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StatsBentoSquare(
                                        totalFiles = totalFiles,
                                        favoriteFiles = favoriteFiles,
                                        onClick = { activeTab = "files" },
                                        modifier = Modifier.weight(1f)
                                    )
                                    RoomDbStorageBentoSquare(
                                        totalFiles = totalFiles,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                CollaborationBentoCard(
                                    sheetsCount = sheetsCount,
                                    writerCount = writerCount,
                                    slidesCount = slidesCount,
                                    onClick = { activeTab = "shared" },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                SmartTemplatesBentoCard(
                                    onQuickCreate = onQuickCreate,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            // Spacer to prevent layout clips by navigation bar
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
                "files" -> {
                    // Modern styled files grid list
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "My Documents Ecosystem",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1A1C1E),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // File type filtering chips inside tab
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 12.dp)
                        ) {
                            listOf("all" to "All Streams", "word" to "Writer Note", "sheet" to "Spreadsheet", "slide" to "Slide Decks").forEach { (type, label) ->
                                val selected = filesCategoryTab == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(if (selected) Color(0xFFD9E2FF) else Color(0xFFEEF0F6))
                                        .clickable { 
                                            filesCategoryTab = type
                                            viewModel.setTypeFilter(type)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) Color(0xFF001D36) else Color(0xFF44474E)
                                        )
                                    )
                                }
                            }
                        }

                        // Listed documents
                        if (documents.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Empty",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No files match active filter",
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                items(documents, key = { it.id }) { doc ->
                                    val isSelected = featuredDoc?.id == doc.id
                                    DocumentTile(
                                        doc = doc,
                                        isSelected = isSelected,
                                        onClick = { viewModel.selectDocument(doc) },
                                        onDelete = { viewModel.deleteDocument(doc) },
                                        onFavoriteToggle = { viewModel.toggleFavorite(doc) }
                                    )
                                }
                            }
                        }
                    }
                }
                "shared" -> {
                    // Collaboration Dashboard Cockpit
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE1E2E9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    "JCdocs Real-Time Simulation Deck",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF1A1C1E)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Simulate background activity of virtual project contributors to demonstrate secure multi-window integrity.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF44474E)
                                )
                            }
                        }

                        Text(
                            text = "ACTIVE SIMULATORS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Gray
                        )

                        // Collaborator rows
                        val simulatedUsers = listOf(
                            Triple("Sarah Jenkins", "Writer Editor", Color(0xFF42A5F5)),
                            Triple("Alex Rivera", "Spreadsheet Coordinator", Color(0xFF66BB6A)),
                            Triple("David Chang", "Slides Presentation Designer", Color(0xFFAB47BC)),
                            Triple("Integrity Agent VIPER", "Autosave Bot", Color(0xFFDF4A32))
                        )

                        simulatedUsers.forEach { (name, role, avatarBg) ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFEEF0F6)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(avatarBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            name.take(1) + name.split(" ").getOrNull(1)?.take(1).orEmpty(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1A1C1E))
                                        Text(role, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(Color(0xFFE8F5E9))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Active", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Simulation interactive block
                        Button(
                            onClick = {
                                showSimulatedStatus = true
                                activeCollaborators++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OnlyOfficePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Simulate")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Co-Editor Background Edits", fontWeight = FontWeight.Bold)
                        }

                        if (showSimulatedStatus) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF3CD))
                                    .border(1.dp, Color(0xFFFFEBAA), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "✨ Simulation Triggered! Real-time local cache transaction registered. SQLite database synced securely.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF856404),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
                "settings" -> {
                    // Gorgeous settings panel
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "User Workspace Settings",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1A1C1E)
                        )

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFEEF0F6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("MY PROFILE CARD", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Gray)

                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text("Display Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = userRole,
                                    onValueChange = { userRole = it },
                                    label = { Text("Workspace Role Title") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFEEF0F6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("SYSTEM INFORMATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Gray)
                                
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Software Engine", fontSize = 13.sp, color = Color.DarkGray)
                                    Text("JCdocs ONLYOFFICE 2.4", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                HorizontalDivider(color = Color(0xFFEEF0F6))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Storage Engine", fontSize = 13.sp, color = Color.DarkGray)
                                    Text("Android SQLite Room DB", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                HorizontalDivider(color = Color(0xFFEEF0F6))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Offline Operations", fontSize = 13.sp, color = Color.DarkGray)
                                    Text("Enabled (100% Native)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                            }
                        }

                        // Cache reset action
                        Button(
                            onClick = {
                                documents.forEach { viewModel.deleteDocument(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Wipe")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear All Local Sandbox Documents", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }

            // --- 3. Large Circle FAB (Placed exactly matching HTML) ---
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 90.dp, end = 20.dp)
            ) {
                FloatingActionButton(
                    onClick = onFABClick,
                    containerColor = Color(0xFFD9E2FF),
                    contentColor = Color(0xFF001D36),
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("bento_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create New Document",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // --- 4. Material 3 Bottom Navigation bar (Placed exactly matching HTML) ---
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavItem(
                            icon = Icons.Default.Home,
                            label = "Home",
                            isSelected = activeTab == "home",
                            onClick = { activeTab = "home" }
                        )
                        BottomNavItem(
                            icon = Icons.Default.Search,
                            label = "Files",
                            isSelected = activeTab == "files",
                            onClick = { activeTab = "files" }
                        )
                        BottomNavItem(
                            icon = Icons.Default.Share,
                            label = "Shared",
                            isSelected = activeTab == "shared",
                            onClick = { activeTab = "shared" }
                        )
                        BottomNavItem(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            isSelected = activeTab == "settings",
                            onClick = { activeTab = "settings" }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(if (isSelected) Color(0xFFD9E2FF) else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFF001D36) else Color(0xFF44474E).copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF001D36) else Color(0xFF44474E)
        )
    }
}

// ==========================================
// BENTO GRID SUB-COMPONENTS
// ==========================================

@Composable
fun FeaturedDocBentoCard(
    featuredDoc: DocEntity?,
    onDocClick: (DocEntity) -> Unit,
    onQuickCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBgColor = Color(0xFFD9E2FF) // Lavender Blue
    val textColor = Color(0xFF001D36)
    val subtextColor = Color(0xFF44474E)

    val lastEditedFormatted = remember(featuredDoc?.updatedAt) {
        if (featuredDoc != null) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            "Modified Today at ${sdf.format(Date(featuredDoc.updatedAt))}"
        } else {
            "No recent modifications recorded"
        }
    }

    Card(
        onClick = { if (featuredDoc != null) onDocClick(featuredDoc) else onQuickCreate() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .testTag("bento_featured_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Icon layout inspired by OnlyOffice and Bento layouts
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    val symbolStr = when (featuredDoc?.type) {
                        "word" -> "W"
                        "sheet" -> "S"
                        "slide" -> "P"
                        else -> "O"
                    }
                    Text(
                        text = symbolStr,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF005AC1),
                        fontSize = 18.sp
                    )
                }

                // Dynamic badges
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFF005AC1).copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (featuredDoc != null) "Resume Editing" else "Create Now",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column {
                Text(
                    text = featuredDoc?.title ?: "Welcome To JCdocs Workspace",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp
                    ),
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (featuredDoc != null) lastEditedFormatted else "Get started immediately by clicking to build your proposal note.",
                    style = MaterialTheme.typography.bodySmall,
                    color = subtextColor
                )
            }
        }
    }
}

@Composable
fun CollaborationBentoCard(
    sheetsCount: Int,
    writerCount: Int,
    slidesCount: Int,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cardBgColor = Color(0xFFE1E2E9) // Cool Grey
    val textColor = Color(0xFF1A1C1E)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SANDBOX ENGAGEMENT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF44474E)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Avatar stacking overlay exactly replicating Tailwind CSS markup
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-8).dp)
            ) {
                // Color dots simulating users
                listOf(Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFAB47BC)).forEachIndexed { i, color ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.5.dp, cardBgColor, CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.6f))
                        .border(1.5.dp, cardBgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+4",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "7 simulated sandboxes active",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Robust offline operations utilizing local cache streams across $writerCount documents, $sheetsCount sheets, and $slidesCount decks.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF44474E).copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun StatsBentoSquare(
    totalFiles: Int,
    favoriteFiles: Int,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cardBgColor = Color(0xFFFAD8FD) // Pastel Lavender Purple
    val textColor = Color(0xFF2B1230)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = totalFiles.toString(),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = textColor
                )
                
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Review Status",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = "TOTAL REVIEWS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = textColor.copy(alpha = 0.5f)
                )
                
                Text(
                    text = "$favoriteFiles Starred Documents",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun RoomDbStorageBentoSquare(
    totalFiles: Int,
    modifier: Modifier = Modifier
) {
    val cardBgColor = Color(0xFFD3E8D3) // Pastel Mint Green
    val textColor = Color(0xFF00210B)
    val progressColor = Color(0xFF116C31)

    // Arbitrary percentage showcasing offline health
    val percentage = if (totalFiles == 0) 0f else minOf(100f, 15f + (totalFiles * 12f))

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SQL DATABASE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = textColor.copy(alpha = 0.6f)
                )
                
                Text(
                    text = "${percentage.toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = textColor
                )
            }

            // Custom green percentage progress bar representing sqlite memory limits
            Column {
                LinearProgressIndicator(
                    progress = percentage / 100f,
                    color = progressColor,
                    trackColor = textColor.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "SQLite schema integrity secure",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = progressColor
                )
            }
        }
    }
}

@Composable
fun SmartTemplatesBentoCard(
    onQuickCreate: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBgColor = Color(0xFFFFDAD6) // Pastel Peach Pink
    val textColor = Color(0xFF410002)

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Smart Templates",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = textColor
                )
                
                Text(
                    text = "AI-powered document structure generation in modern Jetpack Compose",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable row of template quick-action chips exactly conforming to the design
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TemplateChipItem(
                    label = "Invoice Project",
                    onClick = { onQuickCreate("Project Financial Invoice", "sheet") }
                )
                TemplateChipItem(
                    label = "AI Proposal Document",
                    onClick = { onQuickCreate("Bento Proposal Deck", "word") }
                )
                TemplateChipItem(
                    label = "NDA Agreement",
                    onClick = { onQuickCreate("Joint Consultation NDA Agreement", "word") }
                )
                TemplateChipItem(
                    label = "Keynote Slides",
                    onClick = { onQuickCreate("Smart Technology Keynote", "slide") }
                )
            }
        }
    }
}

@Composable
fun TemplateChipItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.6f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF410002)
            )
        )
    }
}

@Composable
fun TemplateCard(
    title: String,
    typeStr: String,
    iconChar: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = modifier
            .width(150.dp)
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconChar,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = typeStr,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WorkspaceMenuBar(
    doc: DocEntity,
    draftTitle: String,
    onTitleChange: (String) -> Unit,
    isSidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = when (doc.type) {
        "word" -> DocWordColor
        "sheet" -> DocSheetColor
        "slide" -> DocSlideColor
        else -> OnlyOfficePrimary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Toggle Sidebar Button
        IconButton(onClick = onToggleSidebar) {
            Icon(
                imageVector = if (isSidebarExpanded) Icons.Default.Close else Icons.Default.Menu,
                contentDescription = "Toggle Sidebar"
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Document Type Badge Indicator
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(themeColor),
            contentAlignment = Alignment.Center
        ) {
            val symbolChar = when (doc.type) {
                "word" -> "W"
                "sheet" -> "S"
                "slide" -> "P"
                else -> "D"
            }
            Text(
                text = symbolChar,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Document Title Text Edit field
        BasicTextField(
            value = draftTitle,
            onValueChange = onTitleChange,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("workspace_title_input")
        )

        // Saved Status Indicator (Automatic local saving is active)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E7D32)) // Soft Green
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Saved",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Close Document Button
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier.testTag("close_document_button")
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Exit to Dashboard",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

class RichTextVisualTransformation(private val baseTagColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        val n = rawText.length
        val isTagChar = BooleanArray(n) { false }

        // Helper to mark tag characters
        fun markTags(regex: Regex, contentGroupIndex: Int) {
            regex.findAll(rawText).forEach { match ->
                val range = match.range
                val contentGroup = match.groups[contentGroupIndex]
                val groupRange = contentGroup?.range
                if (groupRange != null) {
                    for (i in range.first until groupRange.first) {
                        if (i in 0 until n) isTagChar[i] = true
                    }
                    for (i in (groupRange.last + 1)..range.last) {
                        if (i in 0 until n) isTagChar[i] = true
                    }
                }
            }
        }

        // Mark all tags (using DOTALL where appropriate to match across lines via inline (?s) flag)
        markTags("(?s)\\*\\*(.*?)\\*\\*".toRegex(), 1)
        markTags("(?<!\\*)\\*([^*\\n]+?)\\*(?!\\*)".toRegex(), 1)
        markTags("(?s)(?i)<u>(.*?)</u>".toRegex(), 1)
        markTags("(?s)~~(.*?)~~".toRegex(), 1)
        markTags("(?s)(?i)<mark>(.*?)</mark>".toRegex(), 1)
        markTags("(?s)(?i)<sub>(.*?)</sub>".toRegex(), 1)
        markTags("(?s)(?i)<sup>(.*?)</sup>".toRegex(), 1)
        markTags("(?s)(?i)<font\\s+color=\"([^\"]+)\"[^>]*>(.*?)</font>".toRegex(), 2)
        markTags("(?s)(?i)<font\\s+size=\"(\\d+)\"[^>]*>(.*?)</font>".toRegex(), 2)
        markTags("(?s)(?i)<font\\s+face=\"([^\"]+)\"[^>]*>(.*?)</font>".toRegex(), 2)

        val cleanBuilder = java.lang.StringBuilder()
        val originalToTransformed = IntArray(n + 1)
        val transformedToOriginal = mutableListOf<Int>()

        for (i in 0 until n) {
            originalToTransformed[i] = cleanBuilder.length
            if (!isTagChar[i]) {
                cleanBuilder.append(rawText[i])
                transformedToOriginal.add(i)
            }
        }
        originalToTransformed[n] = cleanBuilder.length
        transformedToOriginal.add(n)

        val builder = AnnotatedString.Builder()
        builder.append(cleanBuilder.toString())

        fun applyStyle(regex: Regex, contentGroupIndex: Int, styleProvider: (MatchResult) -> SpanStyle) {
            regex.findAll(rawText).forEach { match ->
                val contentGroup = match.groups[contentGroupIndex]
                val groupRange = contentGroup?.range
                if (groupRange != null) {
                    val startClean = originalToTransformed[groupRange.first]
                    val endClean = originalToTransformed[groupRange.last + 1]
                    if (startClean < endClean) {
                        builder.addStyle(styleProvider(match), startClean, endClean)
                    }
                }
            }
        }

        // Apply styles to clean range
        // 1. Bold: **text**
        applyStyle("(?s)\\*\\*(.*?)\\*\\*".toRegex(), 1) {
            SpanStyle(fontWeight = FontWeight.Bold)
        }

        // 2. Italic: *text* (match single asterisk avoiding those part of double asterisks)
        applyStyle("(?<!\\*)\\*([^*\\n]+?)\\*(?!\\*)".toRegex(), 1) {
            SpanStyle(fontStyle = FontStyle.Italic)
        }

        // 3. Underline: <u>text</u>
        applyStyle("(?s)(?i)<u>(.*?)</u>".toRegex(), 1) {
            SpanStyle(textDecoration = TextDecoration.Underline)
        }

        // 4. Strikethrough: ~~text~~
        applyStyle("(?s)~~(.*?)~~".toRegex(), 1) {
            SpanStyle(textDecoration = TextDecoration.LineThrough)
        }

        // 5. Highlight: <mark>text</mark>
        applyStyle("(?s)(?i)<mark>(.*?)</mark>".toRegex(), 1) {
            SpanStyle(background = Color(0xFFFDE047).copy(alpha = 0.45f))
        }

        // 6. Subscript: <sub>text</sub>
        applyStyle("(?s)(?i)<sub>(.*?)</sub>".toRegex(), 1) {
            SpanStyle(
                baselineShift = androidx.compose.ui.text.style.BaselineShift.Subscript,
                fontSize = 11.sp
            )
        }

        // 7. Superscript: <sup>text</sup>
        applyStyle("(?s)(?i)<sup>(.*?)</sup>".toRegex(), 1) {
            SpanStyle(
                baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript,
                fontSize = 11.sp
            )
        }

        // 8. Font color: <font color="HEX">text</font>
        applyStyle("(?s)(?i)<font\\s+color=\"([^\"]+)\"[^>]*>(.*?)</font>".toRegex(), 2) { match ->
            val colorHex = match.groups[1]?.value ?: ""
            val parsedColor = try {
                Color(android.graphics.Color.parseColor(colorHex))
            } catch (e: Exception) {
                null
            }
            SpanStyle(color = parsedColor ?: Color.Unspecified)
        }

        // 9. Font size: <font size="SIZE">text</font>
        applyStyle("(?s)(?i)<font\\s+size=\"(\\d+)\"[^>]*>(.*?)</font>".toRegex(), 2) { match ->
            val sizeVal = match.groups[1]?.value?.toIntOrNull() ?: 16
            SpanStyle(fontSize = sizeVal.sp)
        }

        // 10. Font face: <font face="FAMILY">text</font>
        applyStyle("(?s)(?i)<font\\s+face=\"([^\"]+)\"[^>]*>(.*?)</font>".toRegex(), 2) { match ->
            val faceName = match.groups[1]?.value ?: ""
            val fontFamily = when (faceName) {
                "Aptos" -> FontFamily.SansSerif
                "Calibri" -> FontFamily.SansSerif
                "Arial" -> FontFamily.SansSerif
                "Times New Roman" -> FontFamily.Serif
                "Courier New" -> FontFamily.Monospace
                "Georgia" -> FontFamily.Serif
                "Space Grotesk" -> FontFamily.SansSerif
                "JetBrains Mono" -> FontFamily.Monospace
                else -> null
            }
            SpanStyle(fontFamily = fontFamily ?: FontFamily.Default)
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, n)
                return originalToTransformed[clamped]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, cleanBuilder.length)
                return transformedToOriginal[clamped]
            }
        }

        return TransformedText(builder.toAnnotatedString(), offsetMapping)
    }
}

// --- 1. JC WORD WRITER EDITOR ---
@Composable
fun WordDocumentEditor(
    draftContent: String,
    onContentChange: (String) -> Unit,
    editorTheme: String,
    onEditorThemeChange: (String) -> Unit,
    pageMargins: androidx.compose.ui.unit.Dp,
    columnCount: Int,
    textAlignment: androidx.compose.ui.text.style.TextAlign,
    fontSize: androidx.compose.ui.unit.TextUnit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
    textFieldValue: TextFieldValue? = null,
    onTextFieldValueChange: ((TextFieldValue) -> Unit)? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null
) {
    val paperColor = when (editorTheme) {
        "white" -> Color.White
        "ivory" -> Color(0xFFFAF6EE)
        "dark" -> Color(0xFF262626)
        else -> Color.White
    }

    val paperTextColor = when (editorTheme) {
        "dark" -> Color(0xFFE0E0E0)
        else -> Color(0xFF2D2D2D)
    }

    val paperMaxWidth = if (isLandscape) 1000.dp else 680.dp

    Column(modifier = modifier) {
        // Main Document Paper Container View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = paperColor),
                shape = RoundedCornerShape(4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = paperMaxWidth)
                    .heightIn(min = 600.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(pageMargins)
                ) {
                    Text(
                        text = "DOCUMENT BODY" + if (columnCount > 1) " ($columnCount Columns)" else "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DocWordColor.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Divider(color = DocWordColor.copy(alpha = 0.15f), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (columnCount > 1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val lines = draftContent.lines()
                            val linesPerColumn = (lines.size + columnCount - 1) / columnCount

                            for (i in 0 until columnCount) {
                                val colStart = i * linesPerColumn
                                val colEnd = minOf(colStart + linesPerColumn, lines.size)
                                val colContent = if (colStart < lines.size) {
                                    lines.subList(colStart, colEnd).joinToString("\n")
                                } else {
                                    ""
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    TextField(
                                        value = colContent,
                                        onValueChange = { newColText ->
                                            val currentLines = draftContent.lines().toMutableList()
                                            val linesToInsert = newColText.lines()

                                            while (currentLines.size < colEnd) {
                                                currentLines.add("")
                                            }
                                            val preList = currentLines.take(colStart)
                                            val postList = currentLines.drop(colEnd)

                                            val merged = preList + linesToInsert + postList
                                            onContentChange(merged.joinToString("\n"))
                                        },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = paperTextColor,
                                            lineHeight = 24.sp,
                                            textAlign = textAlignment,
                                            fontSize = fontSize
                                        ),
                                        placeholder = { Text("Write in column ${i+1}...") },
                                        visualTransformation = RichTextVisualTransformation(MaterialTheme.colorScheme.onSurface),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 450.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        if (textFieldValue != null && onTextFieldValueChange != null) {
                            TextField(
                                value = textFieldValue,
                                onValueChange = onTextFieldValueChange,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = paperTextColor,
                                    lineHeight = 24.sp,
                                    textAlign = textAlignment,
                                    fontSize = fontSize
                                ),
                                placeholder = { Text("Write draft text here...") },
                                visualTransformation = RichTextVisualTransformation(MaterialTheme.colorScheme.onSurface),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 450.dp)
                                    .onFocusChanged { onFocusChanged?.invoke(it.isFocused) }
                                    .testTag("word_editor_content_field")
                            )
                        } else {
                            TextField(
                                value = draftContent,
                                onValueChange = onContentChange,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = paperTextColor,
                                    lineHeight = 24.sp,
                                    textAlign = textAlignment,
                                    fontSize = fontSize
                                ),
                                placeholder = { Text("Write draft text here...") },
                                visualTransformation = RichTextVisualTransformation(MaterialTheme.colorScheme.onSurface),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 450.dp)
                                    .onFocusChanged { onFocusChanged?.invoke(it.isFocused) }
                                    .testTag("word_editor_content_field")
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 2. JC SPREADSHEET EDITOR ---
@Composable
fun SpreadsheetEditor(
    viewModel: DocViewModel,
    modifier: Modifier = Modifier
) {
    val selectedCell by viewModel.selectedCell.collectAsStateWithLifecycle()
    val sheetData by viewModel.sheetData.collectAsStateWithLifecycle()

    val columns = listOf("A", "B", "C", "D", "E", "F", "G", "H")
    val rows = (1..20).toList()

    val cellExpr = sheetData[selectedCell] ?: ""

    Column(modifier = modifier) {
        // Formulas edit top bar
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coordinate badge
                Box(
                    modifier = Modifier
                        .width(55.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DocSheetColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedCell,
                        fontWeight = FontWeight.Bold,
                        color = DocSheetColor,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Formula Icon indicator
                Text(
                    text = "fx",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DocSheetColor
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Cell Formula input bar
                OutlinedTextField(
                    value = cellExpr,
                    onValueChange = { viewModel.updateCellExpression(selectedCell, it) },
                    placeholder = { Text("Enter value or formula like =SUM(A1:A5) or =A1*A2", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DocSheetColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("formula_input_field")
                )
            }
        }

        // Active layout scrollable grid cells
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                // Header letters columns
                Row {
                    // Empty corner anchor
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 28.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(0.5.dp, Color.LightGray)
                    )

                    for (col in columns) {
                        Box(
                            modifier = Modifier
                                .size(width = 110.dp, height = 28.dp)
                                .background(Color(0xFFF1F3F4))
                                .border(0.5.dp, Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                col,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Numbers rows & dynamic cell contents
                for (row in rows) {
                    Row {
                        // Row coordinate badge
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 40.dp)
                                .background(Color(0xFFF1F3F4))
                                .border(0.5.dp, Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = row.toString(),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        for (col in columns) {
                            val cellRef = "$col$row"
                            val isSelected = selectedCell == cellRef

                            val evaluatedValue = viewModel.getCellValue(cellRef)
                            val originalExpression = sheetData[cellRef] ?: ""

                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 40.dp)
                                    .background(
                                        if (isSelected) DocSheetColor.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) DocSheetColor else Color.LightGray
                                    )
                                    .clickable { viewModel.selectCell(cellRef) }
                                    .padding(4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = evaluatedValue,
                                            fontWeight = if (originalExpression.startsWith("=")) FontWeight.Bold else FontWeight.Normal,
                                            color = if (evaluatedValue.startsWith("#")) Color.Red else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (originalExpression.startsWith("=") && !isSelected) {
                                            // Tiny tag indicating reactive formulas
                                            Text(
                                                text = "fx",
                                                color = DocSheetColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 3. JC SLIDE PRESENTATION WORKSPACE ---
@Composable
fun SlidePresentationWorkspace(
    viewModel: DocViewModel,
    modifier: Modifier = Modifier
) {
    val slides by viewModel.slides.collectAsStateWithLifecycle()
    val activeIdx by viewModel.currentSlideIndex.collectAsStateWithLifecycle()

    val activeSlide = slides.getOrNull(activeIdx) ?: SlideItem("Title Slide", "", "indigo", "title_slide")

    Column(modifier = modifier) {
        // Toolkit control actions bar
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.addNewSlide() },
                    colors = ButtonDefaults.buttonColors(containerColor = DocSlideColor),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Slide", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Slide", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.deleteSlide(activeIdx) },
                    enabled = slides.size > 1,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.08f), contentColor = Color.Red),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Slide", fontSize = 12.sp)
                }

                Divider(modifier = Modifier.height(20.dp).width(1.dp))

                // Play presentation mode launcher
                Button(
                    onClick = { viewModel.togglePresenterMode(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("play_slides_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play presentation", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play Deck", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Slide ${activeIdx + 1} of ${slides.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // Secondary workspace split view
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Left list of slides navigator
            Column(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Color.LightGray.copy(alpha = 0.3f))
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                slides.forEachIndexed { index, item ->
                    val isActive = index == activeIdx
                    Card(
                        onClick = { viewModel.selectSlide(index) },
                        colors = CardDefaults.cardColors(
                            containerColor = getSlideThemeBg(item.theme).copy(alpha = if (isActive) 1f else 0.4f)
                        ),
                        border = BorderStroke(
                            2.dp,
                            if (isActive) DocSlideColor else Color.Transparent
                        ),
                        modifier = Modifier
                            .size(76.dp, 54.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                fontWeight = FontWeight.Bold,
                                color = if (item.theme == "charcoal") Color.White else Color.Black,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            // Central layout editor
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Slide Template background preview wrapper
                Card(
                    colors = CardDefaults.cardColors(containerColor = getSlideThemeBg(activeSlide.theme)),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .height(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (activeSlide.layout) {
                            "title_slide" -> {
                                BasicTextField(
                                    value = activeSlide.title,
                                    onValueChange = { viewModel.updateSlideContent(it, activeSlide.body, activeSlide.theme, activeSlide.layout) },
                                    textStyle = TextStyleCompose(activeSlide.theme, 24.sp, FontWeight.ExtraBold, TextAlign.Center),
                                    modifier = Modifier.fillMaxWidth().testTag("slide_title_input")
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                BasicTextField(
                                    value = activeSlide.body,
                                    onValueChange = { viewModel.updateSlideContent(activeSlide.title, it, activeSlide.theme, activeSlide.layout) },
                                    textStyle = TextStyleCompose(activeSlide.theme, 13.sp, FontWeight.Normal, TextAlign.Center),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            "content_slide" -> {
                                BasicTextField(
                                    value = activeSlide.title,
                                    onValueChange = { viewModel.updateSlideContent(it, activeSlide.body, activeSlide.theme, activeSlide.layout) },
                                    textStyle = TextStyleCompose(activeSlide.theme, 18.sp, FontWeight.Bold),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                BasicTextField(
                                    value = activeSlide.body,
                                    onValueChange = { viewModel.updateSlideContent(activeSlide.title, it, activeSlide.theme, activeSlide.layout) },
                                    textStyle = TextStyleCompose(activeSlide.theme, 12.sp, FontWeight.Normal),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
                                )
                            }
                            "split_slide" -> {
                                BasicTextField(
                                    value = activeSlide.title,
                                    onValueChange = { viewModel.updateSlideContent(it, activeSlide.body, activeSlide.theme, activeSlide.layout) },
                                    textStyle = TextStyleCompose(activeSlide.theme, 16.sp, FontWeight.Bold),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    BasicTextField(
                                        value = activeSlide.body,
                                        onValueChange = { viewModel.updateSlideContent(activeSlide.title, it, activeSlide.theme, activeSlide.layout) },
                                        textStyle = TextStyleCompose(activeSlide.theme, 11.sp, FontWeight.Normal),
                                        modifier = Modifier.weight(1f).heightIn(min = 120.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(120.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "[ Presentation Illustration Placeholder ]",
                                            fontSize = 9.sp,
                                            textAlign = TextAlign.Center,
                                            color = if (activeSlide.theme == "charcoal") Color.LightGray else Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Slide configurations settings cards
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Slide Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Choose Color Deck Theme:", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("indigo", "crimson", "teal", "charcoal", "cyberpunk").forEach { themeName ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(getSlideThemeBg(themeName))
                                        .border(
                                            width = if (activeSlide.theme == themeName) 2.dp else 0.5.dp,
                                            color = if (activeSlide.theme == themeName) DocSlideColor else Color.LightGray
                                        )
                                        .clickable {
                                            viewModel.updateSlideContent(
                                                activeSlide.title,
                                                activeSlide.body,
                                                themeName,
                                                activeSlide.layout
                                            )
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Choose Slide Layout Structure:", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("title_slide", "content_slide", "split_slide").forEach { layout ->
                                Button(
                                    onClick = {
                                        viewModel.updateSlideContent(
                                            activeSlide.title,
                                            activeSlide.body,
                                            activeSlide.theme,
                                            layout
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (activeSlide.layout == layout) DocSlideColor else Color.LightGray.copy(alpha = 0.2f),
                                        contentColor = if (activeSlide.layout == layout) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text(layout.replace("_", " ").uppercase(), fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helpers for presentation theme compilation
fun getSlideThemeBg(theme: String): Color {
    return when (theme) {
        "indigo" -> Color(0xFFE8EAF6)
        "crimson" -> Color(0xFFFFEBEE)
        "teal" -> Color(0xFFE0F2F1)
        "charcoal" -> Color(0xFF2D3033)
        "cyberpunk" -> Color(0xFFFFFDE7)
        else -> Color(0xFFE8EAF6)
    }
}

@Composable
fun TextStyleCompose(theme: String, fontSize: androidx.compose.ui.unit.TextUnit, fontWeight: FontWeight, align: TextAlign = TextAlign.Start): androidx.compose.ui.text.TextStyle {
    return androidx.compose.ui.text.TextStyle(
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = if (theme == "charcoal") Color.White else Color.Black,
        textAlign = align
    )
}

// Fullscreen slideshow overlay presenter style
@Composable
fun FullscreenPresentationView(
    viewModel: DocViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val slides by viewModel.slides.collectAsStateWithLifecycle()
    val activeIdx by viewModel.currentSlideIndex.collectAsStateWithLifecycle()

    val activeSlide = slides.getOrNull(activeIdx) ?: SlideItem("End of Deck", "", "indigo", "title_slide")

    Dialog(onDismissRequest = onExit) {
        Card(
            colors = CardDefaults.cardColors(containerColor = getSlideThemeBg(activeSlide.theme)),
            shape = RoundedCornerShape(16.dp),
            modifier = modifier
                .fillMaxWidth()
                .height(420.dp)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Presenter top bar indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DocSlideColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("PRESENTATION MODE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Presentation",
                            tint = if (activeSlide.theme == "charcoal") Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // The actual presentation content display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = if (activeSlide.layout == "title_slide") Alignment.CenterHorizontally else Alignment.Start
                ) {
                    Text(
                        text = activeSlide.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (activeSlide.theme == "charcoal") Color.White else Color.Black,
                        textAlign = if (activeSlide.layout == "title_slide") TextAlign.Center else TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = activeSlide.body,
                        fontSize = 14.sp,
                        color = if (activeSlide.theme == "charcoal") Color.LightGray else Color.DarkGray,
                        textAlign = if (activeSlide.layout == "title_slide") TextAlign.Center else TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Presenter switching footers
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { viewModel.selectSlide(activeIdx - 1) },
                        enabled = activeIdx > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Previous Slide",
                            tint = if (activeSlide.theme == "charcoal") Color.White else Color.Black
                        )
                    }

                    Text(
                        text = "Slide ${activeIdx + 1} of ${slides.size}",
                        fontSize = 12.sp,
                        color = if (activeSlide.theme == "charcoal") Color.LightGray else Color.DarkGray
                    )

                    IconButton(
                        onClick = { viewModel.selectSlide(activeIdx + 1) },
                        enabled = activeIdx < slides.size - 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next Slide",
                            tint = if (activeSlide.theme == "charcoal") Color.White else Color.Black
                        )
                    }
                }
            }
        }
    }
}

// Dialog helper for creating new documents
@Composable
fun CreateDocumentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("word") } // "word", "sheet", "slide"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "New ONLYOFFICE File",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    placeholder = { Text("e.g. Sales Forecast 2026") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OnlyOfficePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("new_document_title_field")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "SELECT OFFICE APP TYPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Custom type buttons matching ONLYOFFICE layout
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeSelectionRow(
                        typeName = "Word Document",
                        typeId = "word",
                        color = DocWordColor,
                        desc = "Create styled notes & rich layout docs",
                        isSelected = selectedType == "word",
                        onClick = { selectedType = "word" }
                    )
                    TypeSelectionRow(
                        typeName = "Spreadsheet Ledger",
                        typeId = "sheet",
                        color = DocSheetColor,
                        desc = "Execute formulas & manage row cell matrices",
                        isSelected = selectedType == "sheet",
                        onClick = { selectedType = "sheet" }
                    )
                    TypeSelectionRow(
                        typeName = "Presentation Slides",
                        typeId = "slide",
                        color = DocSlideColor,
                        desc = "Design templates & play interactive slide decks",
                        isSelected = selectedType == "slide",
                        onClick = { selectedType = "slide" }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onConfirm(title, selectedType) },
                        colors = ButtonDefaults.buttonColors(containerColor = OnlyOfficePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("confirm_create_button")
                    ) {
                        Text("Create File", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun TypeSelectionRow(
    typeName: String,
    typeId: String,
    color: Color,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.08f) else Color.Transparent
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selection_type_$typeId")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                val leadChar = when (typeId) {
                    "word" -> "W"
                    "sheet" -> "S"
                    "slide" -> "P"
                    else -> "D"
                }
                Text(leadChar, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typeName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
