# Teclado IA

<img width="1672" height="941" alt="CapaTecladoIAOdorizzi" src="https://github.com/user-attachments/assets/1c1c8ba2-60f0-40fa-ab6a-a3ca2fc0a83b" />

App Android nativo (Kotlin + Jetpack Compose) que desenha uma **barra de IA direto acima do teclado que você já usa** (Gboard, Samsung Keyboard, etc.), em qualquer app do aparelho. Não troca seu teclado por outro: adiciona um overlay compacto de acessibilidade com atalhos de IA sobre o campo de texto em que você está digitando. Corrige, melhora, traduz, muda o tom, resume, reescreve e dita por voz — tudo aplicado na hora, sem tela de confirmação. Usa a **API do Google Gemini** com a chave da própria pessoa, sem servidor intermediário.

## ⚡ Na barra, sobre o teclado

- **Corrigir** — ortografia, gramática e pontuação, sem mudar o sentido.
- **Melhorar** — deixa o texto mais claro e natural.
- **Traduzir** — para o idioma configurado em Ajustes.
- **Mudar tom** — 8 tons: Profissional, Amigável, Casual, Formal, Romântico, Direto, Educado, Criativo.
- **Resumir** — preserva as informações essenciais. Corrigir, Melhorar, Resumir e Mudar tom mantêm o idioma do texto: o app reconhece em que idioma você escreveu (sem internet) e diz isso à IA. Se você escreveu ou traduziu para o inglês, a correção continua em inglês.
- **Reescrever** — reescreve seguindo uma instrução padrão.
- **Voz** — ditado em tempo real: o texto aparece no campo enquanto você fala e, a cada pausa, o trecho que acabou de ser dito já é pontuado e corrigido pela IA (Gemini ou modelo offline). Ao concluir, só o que sobrou sem corrigir passa pela IA.

Tocar em qualquer atalho aplica o resultado direto no campo, sem passar por tela nenhuma; um **Desfazer** aparece na própria barra logo em seguida. A barra é arrastável — a posição em que você deixar fica salva e volta igual da próxima vez — e tem um modo minimizado, que encolhe tudo em um botão pequeno para não atrapalhar o app por baixo.

## 🧩 Minhas Funções

Prompts personalizados, feitos por você.

- Puxe a barra para baixo, ou toque no chip **✨ IA**, para trocar os atalhos padrão pelas suas próprias funções, na mesma barra.
- Segure o chip **✨ IA** por 2 segundos para ir direto ao app, já na aba Funções.
- Crie, edite, duplique, reordene (arraste o card) e exclua quantas quiser.
- Escolha qualquer emoji como ícone — pelas categorias prontas (sugestões, rostos, objetos, natureza, comida) ou abrindo o teclado de emoji do próprio aparelho para usar qualquer um que exista no sistema.
- Marque **Fixar na barra** para o atalho aparecer sempre entre os primeiros.

## 💾 Backup

Suas funções personalizadas não ficam presas a um aparelho só.

- Em **Ajustes › Backup**, exporte todas de uma vez para um arquivo `.json` salvo direto na pasta **Downloads** do aparelho.
- Restaure a partir de um arquivo salvo antes — pede confirmação primeiro, já que substitui as funções atuais.
- Sem nuvem, sem conta: o arquivo é seu, para guardar ou transferir como preferir.

## 🎨 Interface

- **Material 3**, com tema Escuro, Claro ou Sistema.
- Altura da barra flutuante ajustável em três tamanhos (Compacta, Normal, Expandida), com **preview ao vivo** mostrando o tamanho real antes de confirmar.
- Três modos de animação — Completa, Suave ou Desligada — cobrindo desde a troca de abas até os menus de Ajustes e a entrada da barra sobre o teclado.
- Onboarding na primeira abertura (permissões necessárias) e uma tela de Novidades a cada versão nova instalada.

## 🌍 10 idiomas

🇧🇷 Português (Brasil) · 🇵🇹 Português (Portugal) · 🇺🇸 English · 🇪🇸 Español · 🇷🇴 Română · 🇫🇷 Français · 🇨🇳 中文 · 🇯🇵 日本語 · 🇰🇷 한국어 · 🇮🇳 हिन्दी

O idioma da interface é independente do idioma que a IA usa para responder — esse último fica configurado à parte, em Ajustes.

## 📱 Compatibilidade

| | |
|---|---|
| **Android** | 8.0 (API 26) até o mais recente · o modo IA offline pede um aparelho ARM de 64 bits |
| **Stack** | Kotlin · Jetpack Compose · Material 3 · Coroutines · DataStore |
| **Root** | Não é necessário |

## 🔐 Acessibilidade, sem ser ferramenta de acessibilidade

A barra existe graças a um `AccessibilityService` — é o único jeito de desenhar um overlay que sabe a altura do teclado e consegue ler e substituir o texto do campo focado em *qualquer* app. Mesmo assim, o serviço **não declara `isAccessibilityTool="true"`**, porque o app não é uma ferramenta de acessibilidade: usa o escopo mínimo possível (4 tipos de evento) e nunca registra o que é digitado — nada sai do aparelho até você tocar em um atalho.

Sem essa permissão ativada, o app avisa exatamente o que falta, com um botão que leva direto à tela de ativação nos Ajustes do Android.

## 📴 Modo IA offline

Em **Ajustes › Inteligência artificial** você escolhe quem responde: o **Gemini** (nuvem) ou um **modelo que roda no próprio aparelho**, sem internet e sem enviar texto a ninguém.

- **Baixar**: três modelos prontos — Gemma 3 1B (GGUF, leve), Gemma 4 E2B e Gemma 4 E4B — baixados pelo Android e continuando em segundo plano.
- **Importar**: escolha um arquivo `.litertlm` ou `.gguf` que você já tenha. O formato é reconhecido pelo conteúdo do arquivo, que é conferido e copiado para dentro do app.
- **Temperatura** ajustável, como no Gemini, e botão para **testar o modelo**.
- Escolher um modelo já liga o modo offline na barra inteira. No topo do **Assistente** aparecem os dois motores: o que está em uso (e pronto) fica azul e **Online**; o outro fica apagado e **Offline**, em amarelo, assim como um motor escolhido mas sem chave ou sem modelo. Toque no apagado para ligá-lo; segure em qualquer um para ir direto à configuração dele.
- Dois motores: [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM), da Google, para `.litertlm`, e [llama.cpp](https://github.com/ggml-org/llama.cpp) (pela biblioteca Llamatik) para `.gguf`. Não abre `.bin` nem `.task`. Arquiteturas muito novas podem não ser aceitas pelo llama.cpp embutido.
- O primeiro pedido demora mais porque o modelo precisa ser carregado; depois de alguns minutos sem uso, ele sai da memória. Modelos grandes pedem bastante memória e podem ser lentos em celulares simples.
- Se o modelo offline falhar, o texto **não** é enviado ao Gemini por conta própria: o erro aparece na tela.

## 🔑 Gemini API Key

A chave é sua, criada de graça no [Google AI Studio](https://aistudio.google.com/app/apikey), colada em **Ajustes › Inteligência artificial › Gemini** e cifrada com **AES/GCM** no **Android Keystore** do próprio aparelho — nunca sai daí, nunca é enviada para nenhum servidor além da própria API do Google. Sem chave configurada, o app abre e funciona normalmente; só os atalhos de IA ficam à espera dela.

## 🔄 Atualizações

Em **Ajustes › Atualizações** o app procura uma versão nova nos *releases* deste repositório do GitHub — sem servidor próprio e sem custo. Ao abrir o app (no máximo a cada 12 horas, e dá para desligar), ele consulta a API pública do GitHub; se houver versão mais nova, um aviso aparece no topo do Assistente e a tela de atualização mostra o que mudou. Se uma versão nova sair, o app avisa também por **notificação** (no Android 13+ pede a permissão de notificações; dá para desligar o aviso). A verificação em segundo plano roda pelo próprio serviço de acessibilidade, no máximo a cada 12 horas, e cada versão avisa uma vez só. O botão **Atualizar agora** baixa o APK dentro do app e entrega ao instalador do Android, que pede a sua confirmação; na primeira vez é preciso permitir que o TecladoIA "instale apps desconhecidos" (por isso o app declara a permissão `REQUEST_INSTALL_PACKAGES`). O Android só aceita o arquivo se vier assinado com a mesma chave do app. O download pelo navegador continua como alternativa. A tela de atualização traz ainda o link do repositório (abrir, ver todas as versões e compartilhar).

Para publicar uma atualização: aumente o `versionCode` e o `versionName`, gere o APK de release assinado, crie um *release* com tag no formato `v1.3.8` e anexe o APK (um arquivo com `release` no nome é o preferido).

## 🔒 Privacidade

- O texto só sai do aparelho quando você toca em um atalho de IA — vai direto para a API do Gemini, com a sua própria chave.
- Sem servidor intermediário, sem analytics, sem telemetria. A única consulta que não é a IA é a busca por atualização, feita no GitHub (opcional, pode ser desligada); ela não envia dados seus.
- Backup automático de conta desligado (`allowBackup="false"`); o backup de funções (acima) é manual e local, por escolha sua.

## 🛠️ Solução de problemas

**Xiaomi, Redmi, Poco, Oppo, Realme, OnePlus ou vivo: o serviço desliga sozinho.** O sistema encerra apps em segundo plano. Em **Ajustes › Permissões** o app tem atalhos para isso: ative o *início automático*, deixe a bateria do app como *Sem restrições*, trave o app na tela de recentes e desative/ative a acessibilidade de novo. Se o Android bloquear a opção (“configuração restrita”), abra *Informações do app › ⋮ › Permitir configurações restritas*.

**O banco pediu para desinstalar o app.** Alguns bancos (como o Nubank) recusam abrir com qualquer serviço de acessibilidade instalado fora da Play Store ligado. Nos apps de banco e pagamento conhecidos a barra já fica desligada por padrão — a lista é editável em **Ajustes › Apps em geral** —, mas a decisão de abrir é do banco: desative o serviço antes de usar o app dele e reative depois (há um botão para isso em **Ajustes › Permissões**).

**“Gemini temporariamente indisponível”.** São picos de alta demanda nos servidores do Google (erro 503), que atingem um modelo de cada vez. O app tenta outros modelos sozinho; se persistir, aguarde alguns minutos.

**Play Protect bloqueou a instalação.** O Google bloqueia por padrão apps com serviço de acessibilidade instalados fora da Play Store. É uma regra do sistema, não um aviso sobre o conteúdo do app.

## 🙏 Créditos

- **[Google Gemini API](https://ai.google.dev/)** — o modelo de linguagem por trás de todos os atalhos de IA, usado com a chave gratuita que cada pessoa cria no [Google AI Studio](https://aistudio.google.com/app/apikey).
- **[LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM)** · Apache License 2.0 — o motor que roda os modelos offline no aparelho.
- **[llama.cpp](https://github.com/ggml-org/llama.cpp)** (MIT) pela biblioteca **[Llamatik](https://github.com/ferranpons/Llamatik)** (MIT) — o motor que roda os arquivos `.gguf` no aparelho.
- **Modelos oferecidos para baixar** — Gemma 4 (Google DeepMind, Apache 2.0), pela [comunidade LiteRT no Hugging Face](https://huggingface.co/litert-community), e Gemma 3 1B (Google, licença Gemma) em GGUF pelo [Unsloth](https://huggingface.co/unsloth/gemma-3-1b-it-GGUF).
- **[skydoves/compose-animations](https://github.com/skydoves/compose-animations)** · Apache License 2.0 — catálogo de referência para as animações do app: a expansão com limites compartilhados dos menus e a entrada/saída suave da barra e das trocas de aba.

## 👤 Autor

Feito por **[@odorizzioficial](https://www.youtube.com/@odorizzioficial)**.

📺 [YouTube](https://www.youtube.com/@odorizzioficial) · 💻 [GitHub](https://github.com/odorizzioficial)

## 📄 Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).
