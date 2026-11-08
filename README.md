# Edgeloom — On-Device AI Dashboard & Mesh Cluster

Edgeloom runs open-weight GGUF quantized models locally on your Android devices, pools memory across devices on your local Wi-Fi, and hosts a standard OpenAI-compatible API endpoint for direct desktop and IDE integration.

## Key Capabilities

1. **Hugging Face Hub Integration**
   - Browse and search live GGUF and ONNX models on Hugging Face.
   - Chunked streaming downloads with HTTP Range header resume support and SHA-256 integrity verification.
   - Zero-cloud storage: Models reside exclusively in device-private app storage (`files/models/`).

2. **Hardware Profiling & Auto-Recommendation**
   - Real-time device memory assessment via Android `ActivityManager`.
   - Computes usable pool budget (`sum(RAM) * 0.7`) and tags models as `GREAT FIT`, `TIGHT FIT`, or `TOO BIG`.

3. **Multi-Device Cluster Mesh**
   - Discovers peer devices on the local `/24` Wi-Fi subnet over ports 8080 and 11434 (Ollama).
   - Proportional layer splitting (e.g. Host executes Layers 1–14, Worker executes Layers 15–28).
   - Real TCP socket health checking, round-trip latency reporting, and failover when peers disconnect.

4. **OpenAI-Compatible HTTP Server**
   - Native embedded server on port 8080.
   - Endpoints:
     - `GET /v1/models`
     - `POST /v1/chat/completions` (JSON and Server-Sent Events `text/event-stream`)
     - `POST /v1/images/generations`
     - `GET /health`
   - Memory-exhaustion protection (4MB maximum request body).
   - Optional API Key authentication for external LAN access.
   - Background survival guaranteed via Android Foreground Service (`EdgeloomServerService`).

## Honest Limitations

1. **Model Size Bounds:**
   - On a typical smartphone with 6GB–8GB of RAM, models up to 3B parameters (e.g. Llama 3.2 1B, Gemma 2 2B, SmolLM2 1.7B) in `Q4_K_M` quantization run comfortably.
   - Models larger than 7B require multi-device cluster pooling or 12GB+ RAM to avoid Android OOM killer termination.
2. **Thermal & Battery Throttling:**
   - Extended token generation under continuous heavy workloads generates heat and will trigger device thermal throttling. Keep sessions reasonable or connect to power.
3. **On-Device Image Diffusion:**
   - Full UNet/SD diffusion synthesis requires at least 6GB of free RAM and ~1.8GB model downloads. Under strict local privacy mode, cloud generation fallbacks are not used.

## Architecture

- **UI:** 100% Jetpack Compose with Material 3 theming (`#151413` Dark Canvas, `#D4FF32` Primary Lime).
- **Engine:** Pure Kotlin binary GGUF reader (`GgufReader`), Byte-Pair Encoding tokenizer (`GgufTokenizer`), and tensor forward pipeline.
- **Server:** Embedded non-blocking multi-threaded Java `ServerSocket` implementation.
- **Networking:** `OkHttpClient` with timeout enforcement, cleartext enabled for local LAN traffic only.
