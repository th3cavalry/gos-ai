# AI Assistant for Android

⚠️ DISCLAIMER: THIS IS AN UNOFFICIAL, COMMUNITY-MADE PROJECT. IT IS NOT AFFILIATED WITH, ENDORSED BY, OR OFFICIALLY SUPPORTED BY THE GRAPHENEOS PROJECT OR ITS TEAM.

A privacy-focused AI assistant built as an alternative to Google Gemini on GrapheneOS. Works on any Android device; tested on Android 16 (Pixel 9 Pro).

> **Why this exists**: Always-available AI assistant without sacrificing privacy — you choose the backend and what data leaves the device.

## What It Does

- **Quick activation**: Volume Up + Down, triple power press, accessibility button, default assistant, Quick Settings tile
- **Voice & text input**: Vosk (offline), Android system STT, or cloud Whisper
- **Images & PDFs**: Vision via cloud models (OpenRouter)
- **Web search & weather**: Brave, Exa, or LangSearch + OpenMeteo
- **Local offline LLM**: llama.cpp GGUF models on-device
- **Privacy-first**: Encrypted keys, optional fully offline operation

## AI Backends

| Provider | Description |
|----------|-------------|
| **OpenRouter** | 100+ cloud models (Claude, GPT, Gemini, Llama, …) |
| **Local AI** | On-device GGUF via llama.cpp — no internet needed |

## Voice Recognition

| Method | Notes |
|--------|-------|
| **Vosk** | Offline, multilingual — recommended on GrapheneOS |
| **Android built-in** | System speech recognition |
| **Whisper (cloud)** | Groq or OpenAI API — high accuracy |

## Web Search

| Engine | API key |
|--------|---------|
| **Brave Search** | [brave.com/search/api](https://brave.com/search/api/) |
| **Exa** | [exa.ai](https://exa.ai/) |
| **LangSearch** | [langsearch.com](https://langsearch.com/) |

## Quick Start

1. **Download** APK from [Releases](../../releases) or build locally
2. **Install** on device
3. **Get API keys** (as needed):
   - [OpenRouter](https://openrouter.ai) — required for cloud AI
   - Search API — optional, for live web data
   - [Groq](https://groq.com) — optional, for Whisper STT
4. **Configure** in the app Settings
5. **Set as default assistant**:
   ```
   Settings → Apps → Default apps → Digital assistant app → AI Assistant
   ```
6. **Enable accessibility** (for volume button shortcut):
   ```
   Settings → Accessibility → AI Assistant → Enable
   ```

## Build from Source

```bash
git clone https://github.com/mx37/grapheneos-ai.git
cd grapheneos-ai
./gradlew assembleDebug
```

For **local AI**, native libraries are required:

```bash
./scripts/build-llama-android.sh --download-ndk
./gradlew assembleDebug
```

See [docs/LOCAL_AI_SETUP.md](docs/LOCAL_AI_SETUP.md).

## Code Analysis

One command regenerates all reports (needs [Graphviz](https://graphviz.org/) besides the Android toolchain: `brew install graphviz` or `apt install graphviz`):

```bash
./gradlew analysisReport
open app/build/reports/index.html
```

| Report | Tool |
|--------|------|
| Package and class dependency graphs | jdeps (ships with the JDK) + Graphviz |
| Complexity and code smells | Detekt (report only, never fails the build) |
| Test coverage | Kover |
| API documentation | Dokka |

The graphs and API docs are byte-identical across runs, so you can compare them before and after a change. The coverage report also contains a timestamp, and Detekt may list its findings in a different order.

## Tested On

- **Android 16** (Pixel 9 Pro, GrapheneOS)
- Android 12+ should work (minSdk 26)

## Privacy

- On-device speech (Vosk) and on-device LLM available
- API keys stored in Android Keystore (AES-256-GCM)
- PII redaction before cloud requests (device IDs, phones, emails)
- No tracking or analytics
- Minimal permissions: microphone, internet, foreground service

## Known Issues

- Local AI requires building or obtaining native `arm64-v8a` llama libraries (see LOCAL_AI_SETUP)
- Local models have no web search, weather, or vision
- Free OpenRouter models may refuse tool calling — use a paid mini model for reliable web search
- TTS quality depends on the engine installed on the device

## Documentation

- [Architecture](docs/ARCHITECTURE.md) — code structure and data flow
- [Local AI Setup](docs/LOCAL_AI_SETUP.md) — models and native build
- [Documentation Guide](docs/DOCUMENTATION.md) — navigation index

## Contributing

Bug reports, feature suggestions, and pull requests welcome.

## License

MIT

## Acknowledgments

- **GrapheneOS** — privacy-focused Android
- **OpenRouter** — unified model API
- **Brave / Exa / LangSearch** — search APIs
- **Vosk** — on-device speech recognition
- **llama.cpp** — on-device LLM inference
