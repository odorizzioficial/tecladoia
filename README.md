<div align="center">

# ✨ TecladoIA

**Uma barra de IA que aparece direto acima do teclado que você já usa.**

Sem trocar de teclado. Sem copiar e colar. Sem sair do app.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![minSdk](https://img.shields.io/badge/minSdk-26%20(Android%208.0)-3DDC84?logo=android&logoColor=white)](#)
[![Licença](https://img.shields.io/badge/Licença-MIT-lightgrey)](LICENSE)

[Canal no YouTube](https://www.youtube.com/@odorizzioficial) · [Reportar um problema](../../issues)

</div>

---

## O que é

O **TecladoIA** desenha uma barrinha compacta **por cima** do teclado do seu aparelho (Gboard, Samsung Keyboard, etc.), com atalhos de IA pra usar em qualquer campo de texto, em qualquer app — WhatsApp, Gmail, Instagram, onde for. Ele não é um teclado novo pra você aprender: é uma camada de acessibilidade leve que só aparece quando você precisa dela.

Toca num atalho, o texto do campo é corrigido/melhorado/traduzido **na hora**, sem tela de confirmação — e sempre com um **Desfazer** à mão.

## ⚡ Funções

| Atalho | O que faz |
| --- | --- |
| 🩹 **Corrigir** | Ortografia, gramática e pontuação, sem mudar o sentido |
| ✨ **Melhorar** | Deixa o texto mais claro e natural |
| 🌍 **Traduzir** | Pro idioma que você configurar em Ajustes |
| 🎭 **Mudar tom** | 8 tons: Profissional, Amigável, Casual, Formal, Romântico, Direto, Educado, Criativo |
| ✂️ **Resumir** | Preserva o essencial |
| 🔁 **Reescrever** | Reescreve seguindo uma instrução |
| 🎙️ **Voz** | Ditado em tempo real, com pontuação e correção automáticas |
| 🧩 **Minhas funções** | Prompts personalizados seus — crie, edite, reordene, fixe na barra |

Puxando a barra pra baixo (ou tocando no chip **✨ IA**), os atalhos padrão dão lugar às suas próprias funções, na mesma barra. Segurar o chip por 2 segundos leva direto pro app, na aba Funções.

## 📱 Dentro do app

- **Assistente** — status do serviço, testador de IA embutido e o aviso de chave da API quando faltar.
- **Funções** — suas funções personalizadas: criar, editar, reordenar por arrastar, fixar na barra, escolher qualquer emoji como ícone.
- **Ajustes**
  - **Gemini API Key** — sua chave, o modelo e a temperatura.
  - **Aparência** — tema, altura da barra flutuante (com preview ao vivo), animações e idioma da interface.
  - **Backup** — exporta/restaura suas funções personalizadas em um arquivo na pasta Downloads.
  - **Permissões** e **Sobre** — o essencial pra deixar tudo funcionando, incluindo o que fazer se o Android bloquear o app.

## 🛠️ Stack técnica

Kotlin · Jetpack Compose · Material 3 · Gradle Kotlin DSL (KTS) · Coroutines · DataStore · OkHttp · kotlinx.serialization

`minSdk 26` (Android 8.0) → testado até o Android mais recente · `compileSdk`/`targetSdk 35` · Java 17

## 🚀 Como rodar localmente

```bash
git clone https://github.com/odorizzioficial/tecladoia.git
```

1. Abra a pasta no **Android Studio** e aguarde o Gradle Sync (o wrapper baixa tudo sozinho).
2. Rode em um dispositivo ou emulador com Android 8.0+.
3. Em **Ajustes › Gemini API Key**, crie sua chave gratuita em [Google AI Studio](https://aistudio.google.com/app/apikey), cole no app e toque em **Testar conexão**.

Sem chave configurada, o app abre e funciona normalmente — só os atalhos de IA ficam indisponíveis até você configurar a sua.

### Gerar o APK

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/TecladoIA-debug.apk
./gradlew assembleRelease   # precisa do keystore.properties preenchido (não versionado)
```

## 🔒 Privacidade

- A chave da API fica **cifrada no Android Keystore**, só no seu aparelho.
- O texto só sai do aparelho quando você toca em um atalho de IA — vai direto pra API do Google, com a sua própria chave.
- Sem servidor intermediário, sem analytics, sem telemetria, sem backup automático da conta.

## 🙏 Créditos

Esse projeto não existiria sem essas peças:

- **[Google Gemini API](https://ai.google.dev/)** — o modelo de linguagem por trás de todos os atalhos de IA. O app usa a API pública do Gemini com a chave que cada pessoa cria gratuitamente no [Google AI Studio](https://aistudio.google.com/app/apikey); nada é processado em servidor próprio.
- **[skydoves/compose-animations](https://github.com/skydoves/compose-animations)** (licença Apache 2.0) — catálogo de referência para as animações do app: a expansão com limites compartilhados dos menus de Ajustes e a entrada/saída suave da barra e das trocas de aba.

## 👤 Autor

Feito por **[@odorizzioficial](https://www.youtube.com/@odorizzioficial)**.

📺 [YouTube](https://www.youtube.com/@odorizzioficial) · 💻 [GitHub](https://github.com/odorizzioficial)

## 📄 Licença

Distribuído sob a licença **MIT** — veja [LICENSE](LICENSE) para o texto completo.
