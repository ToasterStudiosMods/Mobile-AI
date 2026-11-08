# Architecture Decisions (DECISIONS.md)

### D01: 100% Local On-Device Execution (Zero Cloud AI)
- **Context:** The prompt strictly forbids cloud AI fallbacks (Firebase, Gemini, Pollinations) and simulated/canned mock strings.
- **Decision:** Remove all references to Gemini API, Google generative language endpoints, and Pollinations. Implement a real Kotlin binary GGUF reader (`GgufReader`) and real Byte-Pair Tokenizer (`GgufTokenizer`) with Quantized Vector arithmetic.
- **Consequence:** App is self-contained, air-gapped capable, privacy-guaranteeing, and compliant with DoD items 1, 2, and 8.

### D02: Image Generation Availability Contract
- **Context:** Mobile devices cannot run 2GB SD UNet diffusion weights in RAM without dedicated NPU drivers or huge downloads.
- **Decision:** As prescribed by DoD item 7 ("Image generation is either real and on-device, or clearly marked unavailable"), when diffusion model weights are not loaded locally, the app explicitly marks Image Generation as unavailable on-device, stating the hardware requirement (minimum 6GB RAM + Vulkan compute), instead of faking it with cloud APIs or canvas drawings.

### D03: Foreground Service for Background Survival
- **Context:** When the app is backgrounded, Android OS kills background sockets and HTTP servers.
- **Decision:** Implement `EdgeloomServerService` with a persistent Foreground Notification (`FOREGROUND_SERVICE_DATA_SYNC` / `CONNECTED_DEVICE`). When the OpenAI server is running, the service keeps the process awake and handles requests uninterrupted.

### D04: Cluster Mesh Security and Failover
- **Context:** Peer communication across LAN must be authenticated, resilient, and handle node drops.
- **Decision:** Use HMAC-SHA256 token verification for cluster node communication. If a worker node disconnects or drops packets mid-generation, the host automatically triggers instant layer failover to local execution without dropping the client stream.
- **Consequence:** Network latency overhead is tracked, and the UI displays a warning when cluster network RTT makes distributed splitting slower than single-device execution.

### D05: Data Exclusions in Backup Rules
- **Context:** GGUF model files (hundreds of megabytes) and API keys must not bloat Android Auto Backup or Google Drive cloud backups.
- **Decision:** Update `data_extraction_rules.xml` and `backup_rules.xml` to explicitly exclude `files/models/` and preferences containing secrets.
