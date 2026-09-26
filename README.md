# TecladoIA

Barra de ferramentas de IA que aparece **imediatamente acima do teclado que você já usa** (Gboard, Samsung Keyboard, etc.). O app **não substitui o teclado**: ele adiciona um overlay compacto com ações de IA sobre o texto que você está digitando.

Kotlin · Jetpack Compose · Material 3 · Gradle Kotlin DSL · minSdk 26 · targetSdk 35

## O que ele faz

| Função | Descrição |
| --- | --- |
| Corrigir | Ortografia, gramática e pontuação, mantendo o significado |
| Melhorar | Clareza e naturalidade, mantendo o significado |
| Traduzir | Para o idioma configurado em Ajustes |
| Mudar tom | 8 tons (Profissional, Amigável, Casual, Formal, Romântico, Direto, Educado, Criativo) |
| Resumir | Preserva as informações essenciais |
| Reescrever | Reescreve seguindo instrução |
| Voz | Ditado com transcrição e correção completa pela IA (pontuação, acentos, maiúsculas, concordância) |
| Minhas funções | Prompts personalizados criados por você (CRUD completo) |

Estados da barra: **Normal** — só as funções padrão da IA, em ciano (Corrigir, Melhorar, Traduzir, Mudar Tom, Resumir, Reescrever), roláveis na horizontal → **Minhas funções** (puxe a barra para baixo, ou toque no chip ✨ IA: os atalhos padrão dão lugar aos seus prompts — emoji + nome, na mesma barra, mesma altura, com rolagem horizontal; nada de painel. O chip ✨ IA volta para os padrões) → **Ditado** (onda, cronômetro, cancelar/concluir, tudo dentro da barra) → **Minimizado** (botão pequeno arrastável).

No app: na primeira abertura aparecem a tela de **permissões** (acessibilidade, microfone, bateria) e a de **Novidades**. Depois, **Assistente** mostra o selo ATIVO/DESATIVADO com brilho, e **Ajustes** tem os menus Gemini API Key, Aparência (tema, altura da barra, idioma), Permissões e Sobre (com Novidades dentro).

Tocar em um atalho **aplica o texto na hora**, sem tela de confirmação; o botão **Desfazer** aparece na barra e volta o texto anterior. A posição em que você deixar a barra é salva e volta igual na próxima vez.

## Estrutura

```
app/src/main/java/com/odorizzioficial/tecladoia/
├── domain/     AiAction, Tone, CustomPrompt, AppSettings, AiError
├── data/       SecureKeyStore (AES/GCM no Android Keystore), SettingsRepository, PromptRepository (DataStore)
├── ai/         PromptLibrary, GeminiService (camada isolada), AiEngine
├── service/    KeyboardOverlayService (AccessibilityService), OverlayController,
│               KeyboardWatcher, TextFieldBridge, VoiceInputController
└── ui/         MainActivity + 3 abas (Assistente, Funções, Ajustes), tela Sobre, tema e overlay Compose
```

## Como abrir no Android Studio

1. Clone o repositório:
   ```bash
   git clone https://github.com/odorizzioficial/ai-keyboard-assistant.git
   ```
2. Android Studio → **File › Open** → selecione a pasta do projeto.
3. Aguarde o Gradle Sync (o wrapper baixa o Gradle 8.9 automaticamente).
   Requisitos: Android Studio Ladybug ou mais novo, JDK 17, Android SDK 35 instalado.
3. **Run ▶** no dispositivo ou emulador (Android 8.0+).

## Como configurar a Gemini API Key

A chave **não está no código** e nunca é enviada para o GitHub. Cada usuário coloca a própria chave no app.

1. Acesse [Google AI Studio](https://aistudio.google.com/app/apikey) e crie uma API Key.
2. No app, abra **Ajustes › Gemini API Key**. Esse menu reúne tudo da IA: link para criar a chave, a chave, o modelo e a temperatura.
3. Toque em **Abrir o Google AI Studio** (abre direto em `aistudio.google.com/apikey`), crie a chave e copie.
4. Cole no campo **Chave de API** (mascarado, com botão de olho) e toque em **Salvar chave**.
5. Toque em **Testar conexão** para validar.
6. Escolha o modelo (padrão: `gemini-flash-latest`, que acompanha sozinho a versão mais nova) e a temperatura. Os idiomas ficam em **Ajustes › Idiomas da IA** — eles definem o idioma das respostas da IA, não o idioma da interface (o app é em português).

### Erro "modelo indisponível"

O Google desligou as famílias Gemini 1.5 e 2.0 — qualquer chamada nelas responde **404**, e é isso que gera a mensagem de modelo indisponível. O app já corrige isso de três formas: o padrão passou a ser `gemini-flash-latest`, um modelo antigo salvo em versões anteriores é trocado automaticamente pelo padrão, e o botão **Buscar modelos da minha chave** pergunta à própria API quais modelos aquela chave aceita em `generateContent` e monta a lista com o resultado. Se aparecer o erro, use esse botão.

A chave é cifrada com **AES/GCM 256** usando uma chave gerada no **Android Keystore** e guardada apenas no aparelho. Para remover, use **Remover chave** na mesma tela.

## Play Protect e configurações restritas

Instalando o APK fora da Play Store, o Android trava duas coisas — as duas são comportamento padrão do sistema, não bug do app:

1. **Play Protect bloqueia a instalação.** Todo APK que não passou pela revisão da Play Store recebe esse aviso. Toque em **Mais detalhes › Instalar mesmo assim**. Para distribuir sem o aviso ficar tão agressivo, assine o APK de release com a sua própria chave (`assembleRelease` com `signingConfigs`) em vez de usar o `app-debug.apk`.
2. **A acessibilidade aparece bloqueada ("configurações restritas").** Desde o Android 13, um app instalado fora da loja não pode ativar serviços de acessibilidade até você liberar: **Ajustes › Apps › TecladoIA › ⋮ (três pontos) › Permitir configurações restritas**. Depois disso, ative o serviço em **Acessibilidade › Apps instalados**.

Esse passo a passo também está no app, em **Ajustes › Sobre › Se o Android bloquear o app**, com botões que abrem direto as informações do app e os ajustes de bateria.

## Como ativar o serviço

1. Abra a aba **Assistente**. Leia o card *"Por que essa permissão"* — ele explica, antes de qualquer pedido, por que o acesso é necessário: ler o texto do campo focado, substituir esse texto e saber a altura do teclado para posicionar a barra.
2. Toque em **Ativar serviço** → você cai em **Ajustes do Android › Acessibilidade › Apps instalados › TecladoIA** → ative.
3. Volte ao app: o status muda para **ATIVO**.

O serviço **não declara `isAccessibilityTool="true"`** (o app não é uma ferramenta de acessibilidade) e usa o escopo mínimo: 4 tipos de evento (`typeWindowStateChanged`, `typeWindowsChanged`, `typeViewFocused`, `typeViewTextSelectionChanged`) e nenhum keylogging. Nada é enviado à IA sem você tocar em uma ação.

## Como testar

- **Sem sair do app:** na aba **Assistente**, no fim da página, em **Testar a IA aqui** — digite um texto, escolha uma ação e veja o resultado.
- **No mundo real:** abra o WhatsApp/Telegram/Gmail, toque no campo de texto. A barra aparece acima do teclado. Digite algo, toque em **Corrigir**, depois em **Substituir**.
- **Voz:** toque no 🎙 da barra. A permissão de microfone é pedida **nesse momento**, não na instalação.
- **Posição da barra:** segure a alça à esquerda e arraste para onde quiser; a posição fica salva entre sessões, e a seta para baixo encolhe tudo em um botão.
- **Ditado:** a fala aparece no campo em tempo real e é corrigida enquanto você fala — 700 ms de pausa disparam a correção do trecho já dito, que passa a ser o prefixo fixo; o trecho novo entra cru e é corrigido em seguida. Ao concluir, a barra volta na hora e a passada final ajusta a frase inteira. O placeholder do campo não é lido junto: um campo vazio é detectado pelo cursor (posição 0) além de `isShowingHintText`, porque apps como o WhatsApp devolvem o próprio "Mensagem" em `node.text`. O texto entra já corrigido (gramática, acentos, pontuação) e o placeholder do campo — "Mensagem", "Pesquisar" — não é mais colado junto. Toque no 🎙 — a barra vira onda + cronômetro. Pausas no meio da fala não encerram a gravação; ela termina quando você toca no ✓ (ou ✕ para cancelar).
- **Minhas funções:** puxe a barra para baixo (ou toque no chip ✨ IA) para trocar os atalhos padrão pelos seus prompts na própria barra; as fixadas vêm primeiro.
- **Funções personalizadas:** aba **Funções** — segure o card (ou a alça) e arraste de ponta a ponta para reordenar e escolha qualquer emoji no seletor por categorias — crie, edite, duplique, reordene, exclua. Marque *Fixar na barra* para o atalho aparecer na barra compacta.

## Como gerar o APK

Local:

```bash
./gradlew assembleDebug
# APK em: app/build/outputs/apk/debug/TecladoIA-debug.apk
```

Release assinado:

1. Abra `keystore.properties` na raiz e preencha com o caminho do seu `.jks`, as senhas e o alias. Esse arquivo e o `.jks` ficam fora do Git (`.gitignore`).
2. Rode:

```bash
./gradlew assembleRelease
# APK em: app/build/outputs/apk/release/TecladoIA-release.apk
```

Sem o `keystore.properties`, o projeto continua compilando normalmente — só o release sai sem a sua assinatura.

## APK do GitHub Actions

O workflow `.github/workflows/android-build.yml` roda a cada push em `main`/`master`, em pull requests e manualmente.

Para baixar o APK: repositório no GitHub → aba **Actions** → clique na execução mais recente de **Android Build** → seção **Artifacts** no fim da página → baixe **`tecladoia-debug`** (um `.zip` com o `app-debug.apk`). Artifacts ficam disponíveis por 30 dias.

Para rodar manualmente: **Actions › Android Build › Run workflow**.

Nenhuma API Key ou secret é usada no workflow.

## Limitações técnicas (reais, documentadas)

Onde o Android não permite exatamente o comportamento ideal, foi implementada a alternativa correta — sem inventar API:

1. **A barra fica sobre o conteúdo do app.** Um overlay de acessibilidade é desenhado *por cima* da janela do app de destino; o Android não permite empurrar o conteúdo de outro app para cima. Por isso a barra é **arrastável**: segure a alça (⠿) na ponta esquerda e leve a barra para qualquer ponto da tela; o botão de alinhar no painel expandido devolve ela para cima do teclado. A **seta para baixo** encolhe tudo em um botão pequeno — e nesse modo a janela também encolhe (`WRAP_CONTENT`), com `FLAG_NOT_TOUCH_MODAL`, para o resto da tela (inclusive o botão de enviar do app) continuar clicável. Em **Ajustes › Aparência e tamanho** a altura **Compacta** deixa a faixa mais fina.

2. **Aparecer em qualquer app.** A barra agora aparece sempre que o teclado do sistema estiver na tela, sem exigir um nó de texto editável — antes, apps cujo campo não é exposto pela acessibilidade (buscas, WebView, campos em Compose) ficavam sem a barra. A janela também é criada uma única vez e depois só escondida (`View.GONE`), em vez de ser recriada a cada abertura do teclado; era isso que travava a animação.

3. **Teclado fechando.** Os eventos de acessibilidade chegam com alturas intermediárias enquanto o IME desce, e acompanhar cada uma fazia a barra escorregar aos pulos atrás do teclado. Agora, quando a altura diminui mais de 24 px, a barra é escondida de uma vez — não há API para sincronizar um overlay com a animação do IME de outro app.

4. **Animação do painel.** A janela do overlay é `WRAP_CONTENT`: animar a altura do painel obrigava o `WindowManager` a refazer o layout da janela a cada frame, o que travava a abertura e o fechamento. O painel agora muda de altura uma única vez e apenas opacidade e escala são animadas, dentro da GPU (`graphicsLayer`), em 110 ms.

5. **Altura do teclado.** Não existe API pública que informe a altura do IME de outro app. O `KeyboardWatcher` deduz isso via `AccessibilityService.getWindows()`, procurando a janela `TYPE_INPUT_METHOD` e medindo seu topo. Teclados flutuantes, em split-screen ou teclados que não expõem a janela pelo serviço de acessibilidade podem fazer a barra ficar mal posicionada ou não aparecer. Há um limite mínimo de 160 px para não confundir a barra de navegação com um teclado.
6. **Substituição de texto.** A substituição usa `AccessibilityNodeInfo.ACTION_SET_TEXT` no nó focado. Apps com campos customizados (Canvas, WebView, jogos, campos com `flagSecure`) não expõem um nó editável — nesses casos o app **não falha em silêncio**: mostra *"Este aplicativo não permite substituir o texto automaticamente."* e copia o resultado para a área de transferência como alternativa.
7. **Leitura de texto.** Só é lido o texto do campo **com foco atual**. Se o app de destino não publicar o conteúdo, aparece *"Não foi possível acessar este campo de texto."*
8. **Reescrever direto na barra.** A janela do overlay é `FLAG_NOT_FOCUSABLE` de propósito — isso é o que garante que ela não roube o foco do teclado nem bloqueie toques fora da própria barra. Consequência: não é possível digitar uma instrução livre dentro da barra. Por isso, o **Reescrever** da barra usa uma instrução padrão, e instruções livres ficam em **Minhas funções** (que aparecem na barra como atalhos) e no playground da aba **Assistente**.
9. **Tipografia.** O design especifica **Roboto Flex**. A fonte variável não é empacotada no APK (para não inflar o binário nem depender de download); o app usa a família padrão do sistema com os mesmos tamanhos, pesos e letter-spacing do design. Para usar a fonte exata, adicione o `.ttf` em `app/src/main/res/font/` e troque `FontFamily.Default` em `ui/theme/Type.kt`.
10. **Voz.** O texto transcrito passa pela IA antes de entrar no campo, com instrução explícita para corrigir ortografia, acentuação, concordância e inserir pontuação (ponto, vírgula, interrogação, exclamação) e maiúsculas. Se a correção falhar (sem chave, sem rede), o texto cru é inserido e o motivo aparece na barra. A transcrição usa o `SpeechRecognizer` do sistema (que já roda no aparelho e não exige chave). O reconhecedor do Android encerra sozinho no primeiro silêncio, então a sessão aqui é contínua: os trechos são acumulados e a escuta reinicia enquanto o usuário não toca em concluir (com `EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS` alto e reinício automático em `ERROR_SPEECH_TIMEOUT`/`ERROR_NO_MATCH`). O áudio **não** é enviado para a Gemini; apenas o texto transcrito é polido pela IA. Não há gravação em segundo plano: o microfone só abre ao toque e fecha ao final.
11. **Idioma da interface.** O app é escrito em português; o seletor de idioma controla o idioma das respostas da IA, não a tradução das telas. Localizar a interface exige extrair os textos para `res/values/strings.xml` e criar as pastas `values-en`, `values-es` e assim por diante.

12. **Serviço de acessibilidade desativando.** O Android pode desconectar o serviço ao encerrar o app em segundo plano (economia de bateria) ou se o serviço lançar uma exceção. O app trata as duas frentes: cada evento de acessibilidade roda dentro de `runCatching`, e o escopo de corrotinas é recriado a cada `onServiceConnected` (antes, uma reconexão reaproveitava um escopo já cancelado e a barra ficava viva sem receber ajustes). O que não dá para resolver por código é a política de bateria do fabricante — há um atalho para os **Ajustes de bateria** em **Ajustes › Sobre › Se o Android bloquear o app**.

13. **Histórico.** Desativado por padrão (modo efêmero). Quando ligado, fica apenas em DataStore local, no aparelho.
14. **Gradle wrapper.** O `gradle-wrapper.jar` oficial (8.9) já está no repositório, então o clone compila sem passos extras.

## Privacidade

- A API Key fica cifrada no Android Keystore, apenas no dispositivo.
- Texto só sai do aparelho quando você toca em uma ação de IA.
- O destino é exclusivamente a API da Gemini, com a sua chave.
- Sem analytics, sem telemetria, sem backup automático (`allowBackup="false"`).

## Onde está a latência

Cada ação registra o tempo das etapas no Logcat, sem nunca incluir a chave:

```bash
adb logcat -s GeminiService:D OverlayController:D
# GeminiService: generateContent model=gemini-flash-latest http=200 preparo=3ms rede+IA=812ms leitura=6ms total=821ms
# OverlayController: Corrigir: leitura=12ms IA=830ms aplicação=18ms total=860ms
```

Assim fica claro se o tempo está na leitura do campo, na rede/Gemini ou na substituição do texto. O app já elimina o que estava sob seu controle: cliente HTTP e pool de conexões reaproveitados, chave decifrada uma vez e mantida em memória, nenhuma chamada de teste ou listagem de modelos antes das ações, teto de saída proporcional ao texto e `thinkingBudget = 0` (o raciocínio interno dos modelos 2.5+ dominava a latência em tarefas de reescrita; se o modelo não aceitar o campo, a chamada é repetida sem ele).

## Autor

Feito por [@odorizzioficial](https://www.youtube.com/@odorizzioficial) — o link também está no rodapé de **Ajustes** dentro do app.

## Licença

MIT — veja [LICENSE](LICENSE).
