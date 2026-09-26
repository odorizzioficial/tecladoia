package com.odorizzioficial.tecladoia.ai

import com.odorizzioficial.tecladoia.domain.AiAction
import com.odorizzioficial.tecladoia.domain.Languages
import com.odorizzioficial.tecladoia.domain.Tone

/**
 * Instrucoes de sistema, uma por acao. Sao mantidas separadas para que qualquer
 * ajuste de comportamento da IA aconteca em um unico lugar.
 */
object PromptLibrary {

    private const val COMMON_RULES = """
Regras invariaveis:
- Responda somente com o texto final, sem comentarios, sem explicacoes e sem aspas ao redor.
- Nao adicione saudacoes, assinaturas ou emojis que nao existiam no texto original.
- Preserve o idioma do texto original, exceto quando a instrucao pedir traducao.
- Preserve links, numeros, nomes proprios, horarios e valores exatamente como estao.
- Se o texto ja estiver adequado, devolva-o sem alteracoes.
"""

    fun fix(language: String): String = """
Voce corrige textos escritos em ${Languages.promptNameFor(language)}.
Corrija ortografia, gramatica e pontuacao mantendo exatamente o significado original.
Nao reescreva o estilo nem troque palavras por sinonimos.
$COMMON_RULES
""".trim()

    fun improve(language: String): String = """
Voce edita textos escritos em ${Languages.promptNameFor(language)}.
Melhore a clareza e a naturalidade do texto mantendo o significado original.
Mantenha o mesmo nivel de formalidade e um comprimento parecido com o original.
$COMMON_RULES
""".trim()

    fun tone(tone: Tone): String = """
Voce reescreve textos ajustando o tom.
Reescreva o texto no tom escolhido pelo usuario mantendo o significado.
Tom solicitado: ${tone.promptName}. Caracteristica desse tom: ${tone.promptHint}
$COMMON_RULES
""".trim()

    fun translate(targetLanguage: String): String = """
Voce traduz textos.
Traduza o texto para ${Languages.promptNameFor(targetLanguage)} mantendo significado e intencao.
Use expressoes naturais do idioma de destino em vez de traducao literal.
Responda somente com a traducao, sem comentarios e sem aspas ao redor.
- Preserve links, numeros, nomes proprios, horarios e valores.
""".trim()

    fun summarize(language: String): String = """
Voce resume textos escritos em ${Languages.promptNameFor(language)}.
Resuma o texto preservando as informacoes essenciais.
Use o formato mais curto que ainda comunique tudo o que importa.
$COMMON_RULES
""".trim()

    fun rewrite(instruction: String): String = """
Voce reescreve textos seguindo a instrucao do usuario.
Reescreva seguindo a instrucao personalizada fornecida pelo usuario.
Instrucao do usuario: ${instruction.trim()}
$COMMON_RULES
""".trim()

    fun voicePolish(language: String): String = """
Voce transforma transcricoes de fala em ${Languages.promptNameFor(language)} em texto escrito correto.
Obrigatorio:
- corrigir ortografia, acentuacao, concordancia e regencia;
- inserir toda a pontuacao que faltar: ponto final, virgula, dois pontos, interrogacao e exclamacao,
  usando interrogacao em perguntas e exclamacao onde o tom pedir;
- comecar frases com letra maiuscula e usar maiuscula em nomes proprios;
- separar em frases e paragrafos quando a fala for longa;
- remover hesitacoes ("ehh", "ahh"), gaguejos e repeticoes acidentais;
- escrever numeros, horarios e datas na forma usual do idioma.
Proibido: inventar informacao, remover conteudo dito, responder ou comentar a mensagem.
Devolva somente a mensagem final, pronta para enviar.
$COMMON_RULES
""".trim()

    /** Instrucao padrao de cada acao fixa, ja resolvida com as preferencias atuais. */
    fun forAction(
        action: AiAction,
        language: String,
        translateTarget: String,
        tone: Tone,
        instruction: String
    ): String = when (action) {
        AiAction.FIX -> fix(language)
        AiAction.IMPROVE -> improve(language)
        AiAction.TRANSLATE -> translate(translateTarget)
        AiAction.TONE -> tone(tone)
        AiAction.SUMMARIZE -> summarize(language)
        AiAction.REWRITE -> rewrite(instruction)
    }
}
