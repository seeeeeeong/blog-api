package com.blog.api.common.util

import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.springframework.stereotype.Component

@Component
class MarkdownUtil {

    private val extensions = listOf(TablesExtension.create())

    private val parser: Parser = Parser.builder()
        .extensions(extensions)
        .build()

    private val renderer: HtmlRenderer = HtmlRenderer.builder()
        .extensions(extensions)
        .build()

    fun convertToHtml(markdown: String): String {
        val document = parser.parse(markdown)
        val html = renderer.render(document)
        return sanitizeHtml(html)
    }

    private fun sanitizeHtml(html: String): String {
        val safelist = Safelist.relaxed()
            .addTags("h1", "h2", "h3", "h4", "h5", "h6")
            .addTags("pre", "code", "span")
            .addTags("table", "thead", "tbody", "tr", "th", "td")
            .addAttributes("code", "class")
            .addAttributes("span", "class")
            .addAttributes("pre", "class")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https")

        return Jsoup.clean(html, safelist)
    }
}
