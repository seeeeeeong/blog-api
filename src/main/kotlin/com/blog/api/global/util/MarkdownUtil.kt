package com.blog.api.global.util

import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.springframework.stereotype.Component

@Component
class MarkdownUtil {

    private val parser: Parser
    private val renderer: HtmlRenderer

    init {
        val extensions = listOf(TablesExtension.create())
        this.parser = Parser.builder()
            .extensions(extensions)
            .build()
        this.renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .build()
    }

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
