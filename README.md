# AI Assistant for Android — self-hosted fork

> **This is a maintained fork of [mx37/gos-ai](https://github.com/mx37/gos-ai)** with a
> self-hosted-first direction: your phone as a privacy-preserving assistant **front-end**,
> your own server as the brain. All credit for the original design and implementation goes
> upstream; we rebranded the primary experience around running your own LLM backend and
> keep this line building signed, reproducible APKs via CI.

⚠️ DISCLAIMER: THIS IS AN UNOFFICIAL, COMMUNITY-MADE PROJECT. IT IS NOT AFFILIATED WITH, ENDORSED BY, OR OFFICIALLY SUPPORTED BY THE GRAPHENEOS PROJECT OR ITS TEAM.

A privacy-focused AI assistant built as an alternative to Google Gemini on GrapheneOS. Works on any Android device; tested on Android 16 (Pixel 9 Pro).

> **Why this exists**: Always-available AI assistant without sacrificing privacy — ideally
> **no third-party cloud at all**. Point the app at your own OpenAI-compatible server
> (llama.cpp, llama-server, Hermes API, Ollama, vLLM, …) with just a base URL and an API
> key; the phone stays a sensor/actuator UI and the agent logic lives on hardware you own.

## Project Direction

This fork exists because the upstream project has been quiet since August 2026 (open
release-packaging bug since July; a collaboration inquiry posted 2026-09-28 awaits reply —
see mx37/gos-ai#12). Goals, in priority order:

1. **Self-hosted endpoint is the default provider** — set your server's URL + API key and
   the app talks only to it. No model catalogs, no fallback cloud, no telemetry-shaped
   decisions made by a hosted provider.
2. **On-device agent tools** — the local agent loop (currently web search, weather) grows
   toward device actions (intents, timers, clipboard, media) so the phone behaves like a
   real assistant with a remote brain.
3. **On-device LLM as fallback** — Local AI (llama.cpp GGUF) for offline use, built into
   CI APKs so it works out of the box on arm64 phones.
4. **OpenRouter kept as an advanced/testing fallback**, never the default.

Releases are built by GitHub Actions from tagged commits; the APK's SHA-256 is published
with each release (upstream once shipped a stale APK under a new tag — we verify).

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
| **Self-hosted endpoint** (default) | Your own OpenAI-compatible server — base URL + API key. Works with llama-server, Hermes API, Ollama, vLLM, LM Studio, … |
| **On-device (offline)** | Local GGUF via bundled llama.cpp — no internet needed |
| **OpenRouter (advanced)** | Hosted cloud fallback — useful for testing vision models |

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

1. **Download** APK from [Releases](../../releases) (each release lists its APK SHA-256) or build locally
2. **Install** on device
3. **Configure the AI backend** in Settings:
   - **Self-hosted endpoint** (default): set your server URL (e.g.
     `http://192.168.1.10:8080/v1` or a tailnet address) and your API key. Done.
   - OpenRouter (advanced): an [OpenRouter](https://openrouter.ai) key — only if you want cloud models
   - Search API — optional, for the agent's live-web tools
   - [Groq](https://groq.com) — optional, for Whisper STT
4. **Set as default assistant**:
   ```
   Settings → Apps → Default apps → Digital assistant app → AI Assistant
   ```
6. **Enable accessibility** (for volume button shortcut):
   ```
   Settings → Accessibility → AI Assistant → Enable
   ```

## Build from Source

```bash
git clone https://github.com/th3cavalry/gos-ai.git
cd gos-ai
./gradlew assembleDebug
```

For **local AI**, native libraries are required:

```bash
./scripts/build-llama-android.sh --download-ndk
./gradlew assembleDebug
```

See [docs/LOCAL_AI_SETUP.md](docs/LOCAL_AI_SETUP.md).

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

- **[mx37 / gos-ai](https://github.com/mx37/gos-ai)** — the upstream project this fork builds on
- **GrapheneOS** — privacy-focused Android
- **OpenRouter** — unified model API
- **Brave / Exa / LangSearch** — search APIs
- **Vosk** — on-device speech recognition
- **llama.cpp** — on-device LLM inference
