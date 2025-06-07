package app.kotatsu.parser.yurigarden

import app.kotatsu.plugin.sdk.KotatsuParserContentProvider

class YuriGardenContentProvider : KotatsuParserContentProvider<YuriGardenParser>(
    authority = "app.kotatsu.parser.yurigarden"
) {
    override fun onCreateParser() = YuriGardenParser(authority)
}