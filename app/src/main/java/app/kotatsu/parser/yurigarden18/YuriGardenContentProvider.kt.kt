package app.kotatsu.parser.yurigarden18

import app.kotatsu.plugin.sdk.KotatsuParserContentProvider

class YuriGardenContentProvider : KotatsuParserContentProvider<YuriGardenParser>(
    authority = "app.kotatsu.parser.yurigarden18"
) {
    override fun onCreateParser() = YuriGardenParser(authority)
}