package com.odorizzioficial.tecladoia.ai

/**
 * Descobre em que idioma um texto foi escrito, para dizer isso a IA com todas as
 * letras. So afirma quando tem certeza: na duvida devolve nulo e o prompt cai na
 * regra generica "responda no idioma do texto".
 *
 * Funciona sem internet e sem biblioteca: confere a escrita (kana, hangul, han,
 * devanagari) e, para os idiomas latinos, conta palavras muito comuns de cada um.
 * O objetivo nao e rotular qualquer texto, e impedir que a IA troque o idioma de
 * quem escreveu em ingles, espanhol, frances etc.
 */
object LanguageDetector {

    /** Nome do idioma em ingles (o prompt e escrito em ingles), ou nulo se nao tiver certeza. */
    fun detect(text: String): String? {
        val sample = text.take(MAX_SAMPLE)
        scriptOf(sample)?.let { return it }
        return latinOf(sample)
    }

    // --- Escrita nao latina -------------------------------------------------

    private fun scriptOf(text: String): String? {
        var kana = 0
        var hangul = 0
        var han = 0
        var devanagari = 0
        var letters = 0
        var other = 0
        for (ch in text) {
            if (!ch.isLetter()) continue
            letters++
            when (Character.UnicodeBlock.of(ch)) {
                Character.UnicodeBlock.HIRAGANA, Character.UnicodeBlock.KATAKANA -> kana++
                Character.UnicodeBlock.HANGUL_SYLLABLES,
                Character.UnicodeBlock.HANGUL_JAMO,
                Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO -> hangul++

                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS -> han++
                Character.UnicodeBlock.DEVANAGARI -> devanagari++
                Character.UnicodeBlock.CYRILLIC,
                Character.UnicodeBlock.ARABIC,
                Character.UnicodeBlock.HEBREW,
                Character.UnicodeBlock.GREEK,
                Character.UnicodeBlock.THAI -> other++

                else -> Unit
            }
        }
        if (letters == 0) return null
        val nonLatin = kana + hangul + han + devanagari + other
        if (nonLatin * 2 < letters) return null // maioria latina: segue para as palavras comuns
        return when {
            kana > 0 -> "Japanese"
            hangul >= han && hangul > 0 -> "Korean"
            han > 0 && hangul == 0 -> "Chinese"
            devanagari > 0 -> "Hindi"
            else -> null // cirilico, arabe etc.: sem lista, fica na regra generica
        }
    }

    // --- Escrita latina -----------------------------------------------------

    private fun latinOf(text: String): String? {
        val words = WORD.findAll(text.lowercase()).map { it.value }.toList()
        if (words.size < MIN_WORDS) return null

        val scores = WORDS.mapValues { (_, list) -> words.count { it in list } }.toMutableMap()
        // Letras que so existem em alguns idiomas pesam mais que uma palavra comum.
        val lower = text.lowercase()
        fun cue(language: String, chars: String) {
            val hits = lower.count { it in chars }
            if (hits > 0) scores[language] = (scores[language] ?: 0) + hits.coerceAtMost(3) * 2
        }
        cue("Portuguese", "ãõ")
        cue("Spanish", "ñ¿¡")
        cue("Romanian", "țșăț")
        cue("German", "ßäöü")
        cue("French", "êèùœ")

        val ranked = scores.entries.sortedByDescending { it.value }
        val best = ranked[0]
        val second = ranked.getOrNull(1)?.value ?: 0
        // Precisa de sinal claro: pelo menos 2 pontos e vantagem sobre o segundo.
        if (best.value < 2 || best.value < second + 2) return null
        return best.key
    }

    private val WORD = Regex("\\p{L}+")

    private fun set(vararg w: String) = w.toSet()

    private val WORDS: Map<String, Set<String>> = mapOf(
        "English" to set(
            "the", "and", "is", "are", "was", "were", "to", "of", "in", "that", "it", "for", "you", "with",
            "this", "have", "has", "my", "me", "we", "they", "not", "but", "on", "at", "be", "will", "would",
            "can", "could", "what", "your", "from", "just", "so", "if", "do", "did", "how", "when", "there",
            "been", "about", "an", "going", "want", "need", "thanks", "please", "hello", "yes", "very", "also"
        ),
        "Portuguese" to set(
            "o", "os", "as", "um", "uma", "de", "do", "da", "dos", "das", "em", "no", "na", "nos", "nas",
            "para", "por", "com", "que", "não", "nao", "é", "eu", "você", "voce", "meu", "minha", "mais",
            "mas", "foi", "são", "sao", "está", "esta", "estou", "tem", "isso", "isto", "ele", "ela", "nós",
            "vocês", "muito", "já", "ja", "como", "quando", "também", "tambem", "ao", "aos", "se", "ou",
            "obrigado", "obrigada", "olá", "ola", "sim", "tudo", "bem", "quero", "preciso", "vou"
        ),
        "Spanish" to set(
            "el", "la", "los", "las", "un", "una", "de", "del", "en", "y", "que", "no", "es", "por", "con",
            "para", "se", "su", "sus", "al", "lo", "como", "más", "mas", "pero", "muy", "yo", "tú", "usted",
            "mi", "mis", "está", "esta", "estoy", "tiene", "esto", "eso", "él", "nosotros", "también", "ya",
            "hay", "fue", "son", "cuando", "porque", "gracias", "hola", "sí", "todo", "bien", "quiero", "necesito"
        ),
        "French" to set(
            "le", "la", "les", "un", "une", "des", "du", "de", "et", "est", "en", "que", "qui", "pas", "pour",
            "dans", "ce", "cette", "je", "tu", "il", "elle", "nous", "vous", "ils", "elles", "mon", "ma", "mes",
            "très", "plus", "mais", "avec", "sur", "au", "aux", "ne", "être", "avoir", "suis", "sont", "merci",
            "bonjour", "oui", "tout", "bien", "veux", "besoin", "aussi", "comme", "quand"
        ),
        "Romanian" to set(
            "și", "si", "de", "în", "la", "cu", "pe", "este", "sunt", "am", "nu", "că", "ca", "un", "o", "ce",
            "mai", "dar", "să", "sa", "pentru", "din", "eu", "tu", "el", "ea", "noi", "voi", "ei", "mă", "te",
            "acest", "această", "foarte", "bine", "mulțumesc", "bună", "da", "vreau", "trebuie"
        ),
        "German" to set(
            "der", "die", "das", "und", "ist", "nicht", "ich", "du", "er", "sie", "es", "wir", "ihr", "zu",
            "mit", "von", "den", "dem", "ein", "eine", "auch", "auf", "für", "fur", "im", "dass", "wie",
            "aber", "noch", "nur", "wenn", "danke", "hallo", "ja", "sehr", "bitte", "will", "brauche"
        ),
        "Italian" to set(
            "il", "lo", "la", "gli", "le", "un", "uno", "una", "di", "del", "della", "e", "è", "che", "non",
            "per", "con", "sono", "io", "tu", "lui", "lei", "noi", "voi", "loro", "mi", "ti", "ma", "anche",
            "come", "più", "piu", "molto", "questo", "questa", "grazie", "ciao", "sì", "tutto", "bene",
            "voglio", "ho", "bisogno"
        )
    )

    private const val MAX_SAMPLE = 600
    private const val MIN_WORDS = 3
}
