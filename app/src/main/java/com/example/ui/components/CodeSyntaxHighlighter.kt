package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EditorBackground
import com.example.ui.theme.EditorGutter
import com.example.ui.theme.EditorLineNumber

object SyntaxHighlighter {

    private val KEYWORDS = setOf(
        "package", "import", "class", "interface", "object", "fun", "val", "var",
        "if", "else", "when", "for", "while", "return", "try", "catch", "finally",
        "throw", "data", "sealed", "override", "public", "private", "protected",
        "internal", "suspend", "def", "async", "await", "from", "import", "const",
        "let", "function", "export", "default", "type", "enum", "extends", "implements",
        "null", "true", "false", "this", "super", "new", "void"
    )

    private val TYPES = setOf(
        "String", "Int", "Boolean", "Long", "Float", "Double", "List", "Map", "Set",
        "Unit", "Any", "Array", "Result", "Flow", "StateFlow", "Composable", "Modifier"
    )

    fun highlightCode(code: String, fileExtension: String): AnnotatedString {
        val lines = code.lines()
        return buildAnnotatedString {
            for ((lineIdx, line) in lines.withIndex()) {
                val trimmed = line.trimStart()

                // Full line comment
                if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
                    append(line)
                    addStyle(
                        SpanStyle(color = Color(0xFF6B7280), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        length - line.length,
                        length
                    )
                } else {
                    // Tokenize by word boundaries
                    var i = 0
                    while (i < line.length) {
                        val c = line[i]

                        // String literal
                        if (c == '"' || c == '\'') {
                            val quoteChar = c
                            val startQuote = i
                            i++
                            while (i < line.length && line[i] != quoteChar) {
                                if (line[i] == '\\' && i + 1 < line.length) i++
                                i++
                            }
                            if (i < line.length) i++ // include closing quote
                            val strToken = line.substring(startQuote, i)
                            val tokenStart = length
                            append(strToken)
                            addStyle(SpanStyle(color = Color(0xFF86EFAC)), tokenStart, length)
                            continue
                        }

                        // Identifier / Keyword
                        if (c.isLetter() || c == '_') {
                            val startWord = i
                            while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) {
                                i++
                            }
                            val word = line.substring(startWord, i)
                            val tokenStart = length
                            append(word)

                            if (KEYWORDS.contains(word)) {
                                addStyle(
                                    SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold),
                                    tokenStart,
                                    length
                                )
                            } else if (TYPES.contains(word)) {
                                addStyle(
                                    SpanStyle(color = Color(0xFFA855F7), fontWeight = FontWeight.SemiBold),
                                    tokenStart,
                                    length
                                )
                            } else if (word.startsWith("@")) {
                                addStyle(SpanStyle(color = Color(0xFFFBBF24)), tokenStart, length)
                            }
                            continue
                        }

                        // Numbers
                        if (c.isDigit()) {
                            val startNum = i
                            while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i] == 'L' || line[i] == 'f')) {
                                i++
                            }
                            val num = line.substring(startNum, i)
                            val tokenStart = length
                            append(num)
                            addStyle(SpanStyle(color = Color(0xFFFDBA74)), tokenStart, length)
                            continue
                        }

                        // Regular characters
                        append(c.toString())
                        i++
                    }
                }

                if (lineIdx < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }
}

@Composable
fun CodeEditorView(
    codeText: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    fileExtension: String = "kt"
) {
    val lines = codeText.lines()
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
    ) {
        // Line number gutter
        Column(
            modifier = Modifier
                .width(44.dp)
                .fillMaxHeight()
                .background(EditorGutter)
                .verticalScroll(verticalScroll)
                .padding(vertical = 12.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.End
        ) {
            for (idx in 1..lines.size.coerceAtLeast(1)) {
                Text(
                    text = "$idx",
                    color = EditorLineNumber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // Code text area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll)
                .padding(12.dp)
        ) {
            BasicTextField(
                value = codeText,
                onValueChange = onCodeChange,
                readOnly = isReadOnly,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFFF1F5F9)
                ),
                cursorBrush = SolidColor(Color(0xFF38BDF8)),
                visualTransformation = {
                    val annotated = SyntaxHighlighter.highlightCode(it.text, fileExtension)
                    androidx.compose.ui.text.input.TransformedText(
                        annotated,
                        androidx.compose.ui.text.input.OffsetMapping.Identity
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
