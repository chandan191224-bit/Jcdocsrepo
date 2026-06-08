package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.DocDao
import com.example.db.DocDatabase
import com.example.db.DocEntity
import com.example.db.DocRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class DocViewModel(application: Application) : AndroidViewModel(application) {
    private val docDao: DocDao = DocDatabase.getDatabase(application).docDao()
    private val repository = DocRepository(docDao)

    // States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow("all") // "all", "word", "sheet", "slide"
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    // Loaded/Active documents list
    val documents: StateFlow<List<DocEntity>> = combine(
        repository.allDocuments,
        _searchQuery,
        _selectedTypeFilter
    ) { allDocs, query, filter ->
        var filtered = allDocs
        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.title.contains(query, ignoreCase = true) }
        }
        if (filter != "all") {
            filtered = filtered.filter { it.type == filter }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently open document for editing
    private val _selectedDoc = MutableStateFlow<DocEntity?>(null)
    val selectedDoc: StateFlow<DocEntity?> = _selectedDoc.asStateFlow()

    // Title & content draft state for active editing to avoid heavy DB writes
    private val _draftTitle = MutableStateFlow("")
    val draftTitle: StateFlow<String> = _draftTitle.asStateFlow()

    private val _draftContent = MutableStateFlow("")
    val draftContent: StateFlow<String> = _draftContent.asStateFlow()

    // Sheet specific states
    private val _selectedCell = MutableStateFlow("A1")
    val selectedCell: StateFlow<String> = _selectedCell.asStateFlow()

    private val _sheetData = MutableStateFlow<Map<String, String>>(emptyMap())
    val sheetData: StateFlow<Map<String, String>> = _sheetData.asStateFlow()

    // Presentation slides specific states
    private val _currentSlideIndex = MutableStateFlow(0)
    val currentSlideIndex: StateFlow<Int> = _currentSlideIndex.asStateFlow()

    private val _slides = MutableStateFlow<List<SlideItem>>(emptyList())
    val slides: StateFlow<List<SlideItem>> = _slides.asStateFlow()

    private val _isPlayingPresentation = MutableStateFlow(false)
    val isPlayingPresentation: StateFlow<Boolean> = _isPlayingPresentation.asStateFlow()

    init {
        // Create initial placeholder templates if db is empty
        viewModelScope.launch {
            repository.allDocuments.first().let { currentList ->
                if (currentList.isEmpty()) {
                    createInitialMockData()
                }
            }
        }
    }

    private suspend fun createInitialMockData() {
        val wordSample = DocEntity(
            title = "Project JCDocs Proposal",
            type = "word",
            content = "Welcome to JCDocs, your professional document companion!\n\nThis is a fully-functional, real-time reactive office suite modeled after powerful document servers like ONLYOFFICE.\n\nJCDocs supports:\n1. Rich text notes with themes.\n2. Spreadsheets with dynamic formula solving (try entering numbers, plus sums like =SUM(A1:B2)).\n3. Slide presentations with dynamic themes and fully interactive play mode.\n\nEnjoy editing and organizing all your docs offline on Android!",
            isFavorite = true
        )
        repository.insertDocument(wordSample)

        val sheetSampleData = JSONObject().apply {
            put("A1", "Item Price")
            put("B1", "Quantity")
            put("C1", "Total Cost")
            put("A2", "120")
            put("B2", "3")
            put("C2", "=A2*B2")
            put("A3", "45")
            put("B3", "5")
            put("C3", "=A3*B3")
            put("A4", "Total Cost Combined")
            put("C4", "=SUM(C2:C3)")
        }.toString()

        val sheetSample = DocEntity(
            title = "Quarterly Sales Sheet",
            type = "sheet",
            content = sheetSampleData,
            isFavorite = false
        )
        repository.insertDocument(sheetSample)

        val slidesSampleData = JSONArray().apply {
            put(JSONObject().apply {
                put("title", "OnlyOffice Native Deck")
                put("body", "Elevating document productivity through high-performance compilers on mobile and desktop web containers.\n\nPresented by: JCDocs Core Team")
                put("theme", "indigo")
                put("layout", "title_slide")
            })
            put(JSONObject().apply {
                put("title", "Feature Matrix")
                put("body", "- Complete Local Persistence using SQLite Room.\n- Fully reactive Jetpack Compose elements.\n- Lightweight cell evaluators supporting basic sum, product & ranges.")
                put("theme", "crimson")
                put("layout", "content_slide")
            })
            put(JSONObject().apply {
                put("title", "Robust Engineering Architecture")
                put("body", "Our client side runs completely offline with structured layout containers and M3 visual styles.")
                put("theme", "teal")
                put("layout", "split_slide")
            })
        }.toString()

        val slidesSample = DocEntity(
            title = "Product Strategy Presentation",
            type = "slide",
            content = slidesSampleData,
            isFavorite = true
        )
        repository.insertDocument(slidesSample)
    }

    // Filter type selection
    fun setTypeFilter(filter: String) {
        _selectedTypeFilter.value = filter
    }

    // Set search query
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Create a new document
    fun createNewDocument(title: String, type: String) {
        viewModelScope.launch {
            val initialContent = when (type) {
                "word" -> "Start typing your new document here..."
                "sheet" -> JSONObject().apply {
                    put("A1", "My Sheet")
                }.toString()
                "slide" -> JSONArray().apply {
                    put(JSONObject().apply {
                        put("title", "New Slide Title")
                        put("body", "Tap here to edit your slide content.")
                        put("theme", "indigo")
                        put("layout", "title_slide")
                    })
                }.toString()
                else -> ""
            }

            val doc = DocEntity(
                title = if (title.isBlank()) "Untitled ${type.replaceFirstChar { it.uppercase() }}" else title,
                type = type,
                content = initialContent
            )
            val newId = repository.insertDocument(doc)
            // Auto open the newly created document
            val created = doc.copy(id = newId.toInt())
            selectDocument(created)
        }
    }

    // Select document to edit/view
    fun selectDocument(doc: DocEntity?) {
        _selectedDoc.value = doc
        if (doc != null) {
            _draftTitle.value = doc.title
            _draftContent.value = doc.content

            // Setup sheet or slide specific drafts
            if (doc.type == "sheet") {
                _sheetData.value = parseSheetData(doc.content)
                _selectedCell.value = "A1"
            } else if (doc.type == "slide") {
                _slides.value = parseSlidesData(doc.content)
                _currentSlideIndex.value = 0
            }
        } else {
            _draftTitle.value = ""
            _draftContent.value = ""
            _sheetData.value = emptyMap()
            _slides.value = emptyList()
        }
    }

    // Update draft title
    fun updateDraftTitle(newTitle: String) {
        _draftTitle.value = newTitle
        saveChangesLocally()
    }

    // Update draft content (mainly Word)
    fun updateDraftContent(newContent: String) {
        _draftContent.value = newContent
        saveChangesLocally()
    }

    // Toggle favorite state
    fun toggleFavorite(doc: DocEntity) {
        viewModelScope.launch {
            val updated = doc.copy(isFavorite = !doc.isFavorite, updatedAt = System.currentTimeMillis())
            repository.insertDocument(updated)
            if (_selectedDoc.value?.id == doc.id) {
                _selectedDoc.value = updated
            }
        }
    }

    // Delete document
    fun deleteDocument(doc: DocEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            if (_selectedDoc.value?.id == doc.id) {
                selectDocument(null)
            }
        }
    }

    // --- Spreadsheet Operations ---
    fun selectCell(cellRef: String) {
        _selectedCell.value = cellRef
    }

    fun updateCellExpression(cellRef: String, expression: String) {
        val updated = _sheetData.value.toMutableMap()
        if (expression.isBlank()) {
            updated.remove(cellRef)
        } else {
            updated[cellRef] = expression
        }
        _sheetData.value = updated

        // Convert back to string content and update main draft content
        val json = JSONObject()
        updated.forEach { (k, v) -> json.put(k, v) }
        _draftContent.value = json.toString()
        saveChangesLocally()
    }

    // Re-evaluates cells reactively representing formula values
    fun getCellValue(cellRef: String): String {
        return evaluateCell(cellRef, _sheetData.value)
    }

    private fun evaluateCell(
        cell: String,
        data: Map<String, String>,
        visited: MutableSet<String> = mutableSetOf()
    ): String {
        if (cell in visited) return "#REF!" // Circular reference
        visited.add(cell)

        val expression = data[cell] ?: return ""
        if (!expression.startsWith("=")) return expression

        // It is a formula, evaluate it!
        val uppercaseExpr = expression.uppercase(Locale.ROOT).trim()

        // 1. SUM: =SUM(A1:B3)
        if (uppercaseExpr.startsWith("=SUM(") && uppercaseExpr.endsWith(")")) {
            val rangeStr = uppercaseExpr.substring(5, uppercaseExpr.length - 1)
            val coords = rangeStr.split(":")
            if (coords.size == 2) {
                val cells = getCellsInRange(coords[0], coords[1])
                var sum = 0.0
                for (c in cells) {
                    val valStr = evaluateCell(c, data, visited)
                    valStr.toDoubleOrNull()?.let { sum += it }
                }
                return formatDouble(sum)
            }
            return "#RANGE!"
        }

        // 2. AVERAGE: =AVG(A1:B3)
        if (uppercaseExpr.startsWith("=AVG(") && uppercaseExpr.endsWith(")")) {
            val rangeStr = uppercaseExpr.substring(5, uppercaseExpr.length - 1)
            val coords = rangeStr.split(":")
            if (coords.size == 2) {
                val cells = getCellsInRange(coords[0], coords[1])
                var sum = 0.0
                var count = 0
                for (c in cells) {
                    val valStr = evaluateCell(c, data, visited)
                    valStr.toDoubleOrNull()?.let {
                        sum += it
                        count++
                    }
                }
                return if (count > 0) formatDouble(sum / count) else "0"
            }
            return "#RANGE!"
        }

        // 3. Multiplication math formula, e.g. =A1*B1 or =A1+B1
        val mathOperators = listOf('*', '+', '-', '/')
        for (op in mathOperators) {
            val idx = uppercaseExpr.indexOf(op)
            if (idx > 0) {
                val leftCell = uppercaseExpr.substring(1, idx).trim()
                val rightCell = uppercaseExpr.substring(idx + 1).trim()

                // Evaluate operands
                val leftRes = if (leftCell.toDoubleOrNull() != null) leftCell else evaluateCell(leftCell, data, visited)
                val rightRes = if (rightCell.toDoubleOrNull() != null) rightCell else evaluateCell(rightCell, data, visited)

                val leftNum = leftRes.toDoubleOrNull()
                val rightNum = rightRes.toDoubleOrNull()

                if (leftNum != null && rightNum != null) {
                    val result = when (op) {
                        '*' -> leftNum * rightNum
                        '+' -> leftNum + rightNum
                        '-' -> leftNum - rightNum
                        '/' -> if (rightNum != 0.0) leftNum / rightNum else return "#DIV/0!"
                        else -> 0.0
                    }
                    return formatDouble(result)
                }
                return "#VALUE!"
            }
        }

        // Standard references like =A1 or =B12
        val referencedCell = uppercaseExpr.substring(1).trim()
        if (isCellReference(referencedCell)) {
            return evaluateCell(referencedCell, data, visited)
        }

        return "#ERROR!"
    }

    private fun isCellReference(ref: String): Boolean {
        if (ref.length < 2) return false
        val col = ref[0]
        val row = ref.substring(1).toIntOrNull()
        return col in 'A'..'Z' && row != null
    }

    private fun getCellsInRange(start: String, end: String): List<String> {
        val result = mutableListOf<String>()
        if (start.isBlank() || end.isBlank()) return result
        val startCol = start[0]
        val startRow = start.substring(1).toIntOrNull() ?: return result
        val endCol = end[0]
        val endRow = end.substring(1).toIntOrNull() ?: return result

        val colFrom = minOf(startCol, endCol)
        val colTo = maxOf(startCol, endCol)
        val rowFrom = minOf(startRow, endRow)
        val rowTo = maxOf(startRow, endRow)

        for (col in colFrom..colTo) {
            for (row in rowFrom..rowTo) {
                result.add("$col$row")
            }
        }
        return result
    }

    private fun formatDouble(d: Double): String {
        return if (d == d.toLong().toDouble()) {
            d.toLong().toString()
        } else {
            String.format(Locale.ROOT, "%.2f", d)
        }
    }

    private fun parseSheetData(jsonStr: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = json.getString(key)
            }
        } catch (_: Exception) {}
        return result
    }

    // --- Slide Presentation Operations ---
    fun selectSlide(index: Int) {
        if (index in 0 until _slides.value.size) {
            _currentSlideIndex.value = index
        }
    }

    fun addNewSlide() {
        val current = _slides.value.toMutableList()
        current.add(
            SlideItem(
                title = "New Slide Title",
                body = "Double-tap to modify this text body.",
                theme = "indigo",
                layout = "content_slide"
            )
        )
        _slides.value = current
        _currentSlideIndex.value = current.size - 1
        saveSlidesToContent()
    }

    fun deleteSlide(index: Int) {
        val current = _slides.value.toMutableList()
        if (current.size > 1 && index in 0 until current.size) {
            current.removeAt(index)
            _slides.value = current
            _currentSlideIndex.value = maxOf(0, index - 1)
            saveSlidesToContent()
        }
    }

    fun updateSlideContent(title: String, body: String, theme: String, layout: String) {
        val idx = _currentSlideIndex.value
        val current = _slides.value.toMutableList()
        if (idx in 0 until current.size) {
            current[idx] = SlideItem(title, body, theme, layout)
            _slides.value = current
            saveSlidesToContent()
        }
    }

    fun togglePresenterMode(isPlaying: Boolean) {
        _isPlayingPresentation.value = isPlaying
    }

    private fun saveSlidesToContent() {
        val arr = JSONArray()
        _slides.value.forEach { item ->
            arr.put(JSONObject().apply {
                put("title", item.title)
                put("body", item.body)
                put("theme", item.theme)
                put("layout", item.layout)
            })
        }
        _draftContent.value = arr.toString()
        saveChangesLocally()
    }

    private fun parseSlidesData(jsonStr: String): List<SlideItem> {
        val result = mutableListOf<SlideItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    SlideItem(
                        title = obj.optString("title", "Untitled Slide"),
                        body = obj.optString("body", ""),
                        theme = obj.optString("theme", "indigo"),
                        layout = obj.optString("layout", "content_slide")
                    )
                )
            }
        } catch (_: Exception) {
            // Add at least one default slide if parsing fails
            result.add(SlideItem("Presentation Deck", "Start editing presentation slides.", "indigo", "title_slide"))
        }
        return result
    }

    // Save direct changes from drafting to DB
    private fun saveChangesLocally() {
        val active = _selectedDoc.value ?: return
        viewModelScope.launch {
            val updated = active.copy(
                title = _draftTitle.value,
                content = _draftContent.value,
                updatedAt = System.currentTimeMillis()
            )
            repository.insertDocument(updated)
            // Synchronize active doc reference
            _selectedDoc.value = updated
        }
    }
}

data class SlideItem(
    val title: String,
    val body: String,
    val theme: String, // "indigo", "crimson", "teal", "charcoal", "cyberpunk"
    val layout: String // "title_slide", "content_slide", "split_slide"
)
