package com.lightningstudio.watchrss.ui.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

sealed class ContentBlock {
    data class Text(val text: String, val style: TextStyle) : ContentBlock()
    data class Image(val url: String, val alt: String?) : ContentBlock()
}

enum class TextStyle {
    TITLE,
    SUBTITLE,
    BODY,
    QUOTE,
    CODE
}

object RssContentParser {
    fun parse(raw: String): List<ContentBlock> {
        if (raw.isBlank()) {
            return emptyList()
        }
        val doc = Jsoup.parseBodyFragment(raw)
        doc.outputSettings().prettyPrint(false)
        doc.select("script,style").remove()

        val blocks = mutableListOf<ContentBlock>()
        doc.body().childNodes().forEach { node ->
            appendNode(node, blocks)
        }
        return blocks
    }

    private fun appendNode(node: Node, blocks: MutableList<ContentBlock>) {
        when (node) {
            is TextNode -> addText(blocks, node.text(), TextStyle.BODY)
            is Element -> {
                when (node.tagName().lowercase()) {
                    "p" -> appendParagraph(node, blocks)
                    "img" -> addImage(blocks, node)
                    "h1", "h2" -> addText(blocks, node.text(), TextStyle.TITLE)
                    "h3", "h4", "h5", "h6" -> addText(blocks, node.text(), TextStyle.SUBTITLE)
                    "blockquote" -> appendBlockquote(node, blocks)
                    "pre", "code" -> addText(blocks, node.text(), TextStyle.CODE)
                    "ul" -> appendList(node, blocks, ordered = false)
                    "ol" -> appendList(node, blocks, ordered = true)
                    "div", "section", "article" -> node.childNodes().forEach { child ->
                        appendNode(child, blocks)
                    }
                    "br" -> addText(blocks, "", TextStyle.BODY)
                    else -> {
                        if (node.children().isEmpty()) {
                            addText(blocks, node.text(), TextStyle.BODY)
                        } else {
                            node.childNodes().forEach { child ->
                                appendNode(child, blocks)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun appendParagraph(element: Element, blocks: MutableList<ContentBlock>) {
        val buffer = StringBuilder()
        fun flushText() {
            if (buffer.isNotBlank()) {
                addText(blocks, buffer.toString(), TextStyle.BODY)
                buffer.clear()
            }
        }

        element.childNodes().forEach { child ->
            when (child) {
                is TextNode -> buffer.append(child.text())
                is Element -> {
                    if (child.tagName().equals("img", ignoreCase = true)) {
                        flushText()
                        addImage(blocks, child)
                    } else {
                        buffer.append(child.text())
                    }
                }
            }
        }
        flushText()
    }

    private fun appendList(
        element: Element,
        blocks: MutableList<ContentBlock>,
        ordered: Boolean
    ) {
        val items = element.select("> li")
        items.forEachIndexed { index, item ->
            val prefix = if (ordered) "${index + 1}. " else "- "
            addText(blocks, prefix + item.text(), TextStyle.BODY)
        }
    }

    private fun appendBlockquote(element: Element, blocks: MutableList<ContentBlock>) {
        val items = element.select("> ul > li, > ol > li")
        if (items.isNotEmpty()) {
            items.forEach { item ->
                addText(blocks, "- " + item.text(), TextStyle.QUOTE)
            }
            return
        }
        addText(blocks, element.text(), TextStyle.QUOTE)
    }

    private fun addImage(blocks: MutableList<ContentBlock>, element: Element) {
        val url = element.attr("src").trim()
        if (url.isNotEmpty()) {
            val alt = element.attr("alt").trim().ifBlank { null }
            blocks.add(ContentBlock.Image(url, alt))
        }
    }

    private fun addText(blocks: MutableList<ContentBlock>, text: String, style: TextStyle) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            blocks.add(ContentBlock.Text(trimmed, style))
        }
    }
}
