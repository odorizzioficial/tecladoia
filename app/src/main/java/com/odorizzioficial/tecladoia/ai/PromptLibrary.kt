package com.odorizzioficial.tecladoia.ai

import com.odorizzioficial.tecladoia.domain.AiAction
import com.odorizzioficial.tecladoia.domain.Languages
import com.odorizzioficial.tecladoia.domain.Tone

/**
 * Instrucoes de sistema, uma por acao. Sao mantidas separadas para que qualquer
 * ajuste de comportamento da IA aconteca em um unico lugar.
 *
 * As instrucoes sao escritas em ingles de proposito: com o prompt em portugues,
 * modelos (principalmente os pequenos, do aparelho) tendiam a responder em
 * portugues mesmo quando o texto estava em ingles. A regra de idioma vai no
 * topo, e quando o idioma do texto e reconhecido ele e dito pelo nome.
 */
object PromptLibrary {

    /**
     * Regra de idioma de cada pedido. [translationAllowed] libera a traducao
     * quando a instrucao do usuario pedir explicitamente (funcoes personalizadas).
     */
    private fun languageRule(text: String, translationAllowed: Boolean = false): String {
        val detected = LanguageDetector.detect(text)
        val base = if (detected != null) {
            "The user's text is written in $detected. Your whole answer must be written in $detected. " +
                "Never translate it into any other language, even though these instructions are in English."
        } else {
            "Detect the language of the user's text and write your whole answer in that same language. " +
                "Never translate it, even though these instructions are in English."
        }
        return if (translationAllowed) {
            "$base The only exception: translate if the user's instruction explicitly asks for a translation."
        } else {
            base
        }
    }

    private fun rules(text: String, translationAllowed: Boolean = false): String = """
Rules (all mandatory):
- LANGUAGE: ${languageRule(text, translationAllowed)}
- Reply with the final text only: no comments, no explanations and no quotation marks around it.
- Do not add greetings, signatures or emojis that were not in the original text.
- Keep links, numbers, proper names, times and amounts exactly as they are.
- If the text is already fine, return it unchanged.
""".trim()

    fun fix(text: String): String = """
You are a proofreader.
Fix spelling, grammar, accents and punctuation, keeping exactly the original meaning.
Do not rewrite the style or swap words for synonyms.
${rules(text)}
""".trim()

    fun improve(text: String): String = """
You are a text editor.
Improve the clarity and naturalness of the text while keeping its meaning.
Keep the same level of formality and a similar length.
${rules(text)}
""".trim()

    fun tone(tone: Tone, text: String): String = """
You rewrite texts adjusting their tone.
Rewrite the text in the tone requested, keeping the meaning.
Requested tone: ${tone.promptName}. What defines this tone: ${tone.promptHint}
${rules(text)}
""".trim()

    fun translate(targetLanguage: String): String = """
You are a translator.
Translate the user's text into ${Languages.englishNameFor(targetLanguage)}, keeping meaning and intent.
Use natural expressions of the target language instead of a literal translation.
Reply with the translation only, without comments and without quotation marks around it.
Keep links, numbers, proper names, times and amounts.
""".trim()

    fun summarize(text: String): String = """
You summarize texts.
Summarize the text keeping the essential information.
Use the shortest format that still conveys everything that matters.
${rules(text)}
""".trim()

    fun rewrite(instruction: String, text: String = ""): String = """
You rewrite texts following the user's instruction.
Rewrite the text following the custom instruction below.
User's instruction: ${instruction.trim()}
${rules(text, translationAllowed = true)}
""".trim()

    /**
     * Correcao do ditado. [language] e o idioma da fala configurado; o idioma
     * reconhecido no proprio texto, quando ha certeza, vale mais que ele.
     */
    fun voicePolish(language: String, text: String = ""): String {
        val spoken = LanguageDetector.detect(text) ?: Languages.englishNameFor(language)
        return """
You turn speech transcripts in $spoken into correct written text.
Required:
- fix spelling, accents, agreement and grammar;
- add all the punctuation that is missing: full stops, commas, colons, question marks and exclamation marks,
  using a question mark for questions and an exclamation mark where the tone calls for it;
- start sentences with a capital letter and capitalize proper nouns;
- split into sentences and paragraphs when the speech is long;
- remove hesitations ("uh", "um"), stutters and accidental repetitions;
- write numbers, times and dates in the usual form of the language.
The text may be just a fragment from the middle of a longer speech: fix only what was said, without inventing a beginning or an end.
Forbidden: inventing information, removing content that was said, answering or commenting on the message.
Return only the final message, ready to send.
${rules(text)}
""".trim()
    }

    /**
     * Instrucao padrao de cada acao fixa, ja resolvida com as preferencias atuais.
     * O idioma do app nao entra em corrigir, melhorar e resumir: o texto e
     * tratado no idioma em que a pessoa o escreveu (inclusive depois de traduzir).
     */
    @Suppress("UNUSED_PARAMETER")
    fun forAction(
        action: AiAction,
        language: String,
        translateTarget: String,
        tone: Tone,
        instruction: String,
        text: String
    ): String = when (action) {
        AiAction.FIX -> fix(text)
        AiAction.IMPROVE -> improve(text)
        AiAction.TRANSLATE -> translate(translateTarget)
        AiAction.TONE -> tone(tone, text)
        AiAction.SUMMARIZE -> summarize(text)
        AiAction.REWRITE -> rewrite(instruction, text)
    }
}
