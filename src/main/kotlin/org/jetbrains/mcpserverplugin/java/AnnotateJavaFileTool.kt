package com.example


import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import kotlinx.serialization.Serializable
import org.jetbrains.ide.mcp.Response
import org.jetbrains.mcpserverplugin.AbstractMcpTool

@Serializable
data class AnnotateDocumentArgs(
    val filePath: String
)



class AnnotateDocumentTool : AbstractMcpTool<AnnotateDocumentArgs>() {
    override val name: String = "annotateJavaFile"
    override val description: String = """
        This will annotate a java file with line numbers and column ranges.  This information can be used to use refactoring tools
        An annotated line will be formatted as:
        <lineIndex>: <java line> <identifier:columnRange>*
        
        Example:
        
        109:     private void processNode(Node node, java.util.function.Consumer<T> action) {   [processNode:18-29] [Node:30-34]
         [node:35-39] [java:41-45] [util:46-50] [function:51-59] [Consumer:60-68] [T:69-70] [action:72-78]
        110:         logger.trace("Processing node value: {}", node.value);   [logger:9-15] [trace:16-21] [node:51-55] [value:56
        -61]
        111:         action.accept(node.value);   [action:9-15] [accept:16-22] [node:23-27] [value:28-33]
        112:     }
        
    """.trimIndent()


    override fun handle(project: Project, args: AnnotateDocumentArgs): Response {
        return ApplicationManager.getApplication().runReadAction<Response> {
            val basePath = project.basePath ?: return@runReadAction Response(null, "Project base path not found")
            val fullPath = "$basePath/${args.filePath}"
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(fullPath)
                ?: return@runReadAction Response(null, "File not found: $fullPath")
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: return@runReadAction Response(null, "PSI file not found for: ${args.filePath}")
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
                ?: return@runReadAction Response(null, "Could not get document for file: ${args.filePath}")

            // Collect all PsiIdentifiers once, convert to a list, and filter.
            val allIdentifiers = PsiTreeUtil.collectElements(psiFile) { it is PsiIdentifier }
                .toList()
                .filterIsInstance<PsiIdentifier>()

            // Group identifiers by their line number.
            val identifiersByLine = mutableMapOf<Int, MutableList<PsiIdentifier>>()
            for (identifier in allIdentifiers) {
                val offset = identifier.textRange.startOffset
                val line = document.getLineNumber(offset)
                identifiersByLine.getOrPut(line) { mutableListOf() }.add(identifier)
            }

            val annotatedLines = mutableListOf<String>()

            // Process each line of the document.
            for (lineNumber in 0 until document.lineCount) {
                val lineStart = document.getLineStartOffset(lineNumber)
                val lineEnd = document.getLineEndOffset(lineNumber)
                val lineText = document.getText(TextRange(lineStart, lineEnd))
                val identifiers = identifiersByLine[lineNumber] ?: emptyList()

                // Build inline annotations (e.g., "[foo:5-7]").
                val annotations = identifiers.map { identifier ->
                    val startColumn = identifier.textRange.startOffset - lineStart + 1
                    val endColumn = identifier.textRange.endOffset - lineStart + 1
                    "[${identifier.text}:$startColumn-$endColumn]"
                }.joinToString(" ")

                // Format the line.
                val annotatedLine = if (annotations.isNotBlank()) {
                    "${lineNumber + 1}:\t$lineText   $annotations"
                } else {
                    "${lineNumber + 1}:\t$lineText"
                }
                annotatedLines.add(annotatedLine)
            }

            val result = annotatedLines.joinToString("\n")
            Response(result, null)
        }
    }
}