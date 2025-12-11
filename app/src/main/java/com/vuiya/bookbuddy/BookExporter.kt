package com.vuiya.bookbuddy

import android.content.Context
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.io.File

/**
 * Converts Quill.js HTML content to various publishable formats
 */
class BookExporter(private val context: Context) {

    /**
     * Exports book to plain text format
     * Removes all HTML tags but preserves structure
     */
    fun exportToPlainText(bookDir: File, outputFile: File) {
        val pages = loadPages(bookDir)
        val plainText = buildString {
            pages.forEachIndexed { index, content ->
                if (index > 0) appendLine("\n--- Page ${index + 1} ---\n")
                appendLine(htmlToPlainText(content))
            }
        }
        outputFile.writeText(plainText)
    }

    /**
     * Exports book to Markdown format
     * Preserves formatting (headings, bold, italic, lists)
     */
    fun exportToMarkdown(bookDir: File, outputFile: File) {
        val pages = loadPages(bookDir)
        val markdown = buildString {
            pages.forEachIndexed { index, content ->
                if (index > 0) appendLine("\n\\pagebreak\n")
                appendLine(htmlToMarkdown(content))
            }
        }
        outputFile.writeText(markdown)
    }

    /**
     * Exports book to HTML format (clean, printable)
     * Suitable for conversion to PDF or EPUB
     */
    fun exportToCleanHtml(bookDir: File, outputFile: File, bookTitle: String, author: String) {
        val pages = loadPages(bookDir)
        val html = buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"en\">")
            appendLine("<head>")
            appendLine("    <meta charset=\"UTF-8\">")
            appendLine("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            appendLine("    <title>$bookTitle</title>")
            appendLine("    <style>")
            appendLine("        @page { size: A4; margin: 2.5cm; }")
            appendLine("        body { font-family: Georgia, serif; font-size: 12pt; line-height: 1.6; max-width: 21cm; margin: 0 auto; }")
            appendLine("        h1 { font-size: 24pt; margin-top: 1em; }")
            appendLine("        h2 { font-size: 18pt; margin-top: 1em; }")
            appendLine("        h3 { font-size: 14pt; margin-top: 1em; }")
            appendLine("        p { margin: 0.5em 0; text-align: justify; }")
            appendLine("        .page-break { page-break-after: always; }")
            appendLine("        .title-page { text-align: center; margin-top: 30%; }")
            appendLine("        .title-page h1 { font-size: 36pt; }")
            appendLine("        .title-page .author { font-size: 18pt; margin-top: 2em; }")
            appendLine("    </style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("    <div class=\"title-page\">")
            appendLine("        <h1>$bookTitle</h1>")
            appendLine("        <p class=\"author\">by $author</p>")
            appendLine("    </div>")
            appendLine("    <div class=\"page-break\"></div>")

            pages.forEach { content ->
                appendLine("    <div class=\"page\">")
                appendLine(cleanHtml(content))
                appendLine("    </div>")
                appendLine("    <div class=\"page-break\"></div>")
            }

            appendLine("</body>")
            appendLine("</html>")
        }
        outputFile.writeText(html)
    }

    /**
     * Loads all pages from book directory in order
     */
    private fun loadPages(bookDir: File): List<String> {
        return bookDir.listFiles()
            ?.filter { it.name.startsWith("page_") && it.name.endsWith(".txt") }
            ?.mapNotNull { file ->
                PageNumber.fromFileName(file.name)?.let { pageNum -> pageNum to file }
            }
            ?.sortedBy { it.first }
            ?.map { it.second.readText() }
            ?: emptyList()
    }

    /**
     * Converts Quill HTML to plain text
     */
    private fun htmlToPlainText(html: String): String {
        if (html.isBlank()) return ""
        val doc = Jsoup.parse(html)
        return doc.text()
    }

    /**
     * Converts Quill HTML to Markdown
     */
    private fun htmlToMarkdown(html: String): String {
        if (html.isBlank()) return ""
        val doc = Jsoup.parse(html)
        return buildString {
            processElement(doc.body(), this)
        }
    }

    private fun processElement(element: Element, output: StringBuilder) {
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> output.append(node.text())
                is Element -> {
                    when (node.tagName().lowercase()) {
                        "h1" -> output.appendLine("# ${node.text()}\n")
                        "h2" -> output.appendLine("## ${node.text()}\n")
                        "h3" -> output.appendLine("### ${node.text()}\n")
                        "h4" -> output.appendLine("#### ${node.text()}\n")
                        "h5" -> output.appendLine("##### ${node.text()}\n")
                        "h6" -> output.appendLine("###### ${node.text()}\n")
                        "strong", "b" -> output.append("**${node.text()}**")
                        "em", "i" -> output.append("*${node.text()}*")
                        "u" -> output.append("_${node.text()}_")
                        "p" -> {
                            processElement(node, output)
                            output.appendLine("\n")
                        }
                        "br" -> output.appendLine()
                        "ol" -> {
                            node.select("li").forEachIndexed { index, li ->
                                output.appendLine("${index + 1}. ${li.text()}")
                            }
                            output.appendLine()
                        }
                        "ul" -> {
                            node.select("li").forEach { li ->
                                output.appendLine("- ${li.text()}")
                            }
                            output.appendLine()
                        }
                        "blockquote" -> {
                            node.text().lines().forEach { line ->
                                output.appendLine("> $line")
                            }
                            output.appendLine()
                        }
                        "code" -> output.append("`${node.text()}`")
                        "pre" -> output.appendLine("```\n${node.text()}\n```\n")
                        else -> processElement(node, output)
                    }
                }
            }
        }
    }

    /**
     * Cleans Quill HTML - removes Quill-specific classes, keeps semantic HTML
     */
    private fun cleanHtml(html: String): String {
        if (html.isBlank()) return ""
        val doc = Jsoup.parse(html)

        // Remove all Quill-specific classes
        doc.select("[class]").forEach { element ->
            val classes = element.classNames().filter { !it.startsWith("ql-") }
            if (classes.isEmpty()) {
                element.removeAttr("class")
            } else {
                element.classNames(classes.toSet())
            }
        }

        // Convert alignment classes to inline styles
        doc.select("p").forEach { p ->
            when {
                html.contains("ql-align-center") -> p.attr("style", "text-align: center;")
                html.contains("ql-align-right") -> p.attr("style", "text-align: right;")
                html.contains("ql-align-justify") -> p.attr("style", "text-align: justify;")
            }
        }

        return doc.body().html()
    }
}

