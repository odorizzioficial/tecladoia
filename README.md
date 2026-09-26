# TecladoIA

<img width="1672" height="941" alt="CapaTecladoIAOdorizzi" src="https://github.com/user-attachments/assets/8886b141-8df7-44f2-88c2-478bb3fcf79d" />

App Android nativo (Kotlin + Jetpack Compose) que desenha uma **barra de IA direto acima do teclado que você já usa** (Gboard, Samsung Keyboard, etc.), em qualquer app do aparelho. Não troca seu teclado por outro: adiciona um overlay compacto de acessibilidade com atalhos de IA sobre o campo de texto em que você está digitando. Corrige, melhora, traduz, muda o tom, resume, reescreve e dita por voz — tudo aplicado na hora, sem tela de confirmação. Usa a **API do Google Gemini** com a chave da própria pessoa, sem servidor intermediário.

## ⚡ Na barra, sobre o teclado

- **Corrigir** — ortografia, gramática e pontuação, sem mudar o sentido.
- **Melhorar** — deixa o texto mais claro e natural.
- **Traduzir** — para o idioma configurado em Ajustes.
- **Mudar tom** — 8 tons: Profissional, Amigável, Casual, Formal, Romântico, Direto, Educado, Criativo.
- **Resumir** — preserva as informações essenciais.
- **Reescrever** — reescreve seguindo uma instrução padrão.
- **Voz** — ditado em tempo real: o texto aparece no campo enquanto você fala, com pontuação e correção automáticas.

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
| **Android** | 8.0 (API 26) até o mais recente |
| **Stack** | Kotlin · Jetpack Compose · Material 3 · Coroutines · DataStore |
| **Root** | Não é necessário |

## 🔐 Acessibilidade, sem ser ferramenta de acessibilidade

A barra existe graças a um `AccessibilityService` — é o único jeito de desenhar um overlay que sabe a altura do teclado e consegue ler e substituir o texto do campo focado em *qualquer* app. Mesmo assim, o serviço **não declara `isAccessibilityTool="true"`**, porque o app não é uma ferramenta de acessibilidade: usa o escopo mínimo possível (4 tipos de evento) e nunca registra o que é digitado — nada sai do aparelho até você tocar em um atalho.

Sem essa permissão ativada, o app avisa exatamente o que falta, com um botão que leva direto à tela de ativação nos Ajustes do Android.

## 🔑 Gemini API Key

A chave é sua, criada de graça no [Google AI Studio](https://aistudio.google.com/app/apikey), colada em **Ajustes › Gemini API Key** e cifrada com **AES/GCM** no **Android Keystore** do próprio aparelho — nunca sai daí, nunca é enviada para nenhum servidor além da própria API do Google. Sem chave configurada, o app abre e funciona normalmente; só os atalhos de IA ficam à espera dela.

## 🔒 Privacidade

- O texto só sai do aparelho quando você toca em um atalho de IA — vai direto para a API do Gemini, com a sua própria chave.
- Sem servidor intermediário, sem analytics, sem telemetria.
- Backup automático de conta desligado (`allowBackup="false"`); o backup de funções (acima) é manual e local, por escolha sua.

## 🙏 Créditos

- **[Google Gemini API](https://ai.google.dev/)** — o modelo de linguagem por trás de todos os atalhos de IA, usado com a chave gratuita que cada pessoa cria no [Google AI Studio](https://aistudio.google.com/app/apikey).
- **[skydoves/compose-animations](https://github.com/skydoves/compose-animations)** · Apache License 2.0 — catálogo de referência para as animações do app: a expansão com limites compartilhados dos menus e a entrada/saída suave da barra e das trocas de aba.

## 👤 Autor

Feito por **[@odorizzioficial](https://www.youtube.com/@odorizzioficial)**.

📺 [YouTube](https://www.youtube.com/@odorizzioficial) · 💻 [GitHub](https://github.com/odorizzioficial)

## 📄 Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).
