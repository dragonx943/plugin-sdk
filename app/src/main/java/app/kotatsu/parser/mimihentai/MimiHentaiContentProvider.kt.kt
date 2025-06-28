package app.kotatsu.parser.mimihentai

import app.kotatsu.plugin.sdk.KotatsuParserContentProvider

class MimiHentaiContentProvider : KotatsuParserContentProvider<MimiHentaiParser>(
    authority = "app.kotatsu.parser.mimihentai"
) {
    override fun onCreateParser() = MimiHentaiParser(authority)
}