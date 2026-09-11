# Echo 회의록 — Korean fork

This fork changes the original Daedalus Echo transcription path to use the
multilingual sherpa-onnx Whisper Base model and forces Korean (`ko`) transcription.

## Changes in v1

- English-only `base.en` model -> multilingual `base` model
- Whisper language -> Korean (`ko`)
- Model is still downloaded on first use; it is not bundled into the APK
- App label -> `Echo 회의록`
- GitHub Actions now runs unit tests and builds/uploads a Debug APK artifact

## Model

The multilingual model is downloaded from:

https://huggingface.co/csukuangfj/sherpa-onnx-whisper-base

The int8 encoder/decoder plus tokens are about 161 MB combined.

## Next steps

1. Build/install and test Korean recordings on a real Android phone.
2. Improve Korean UI strings.
3. Add timestamped transcript segments.
4. Add tap-to-seek from transcript to audio.
5. Add meeting-oriented summary/action-item formatting.
6. Evaluate Base vs Small accuracy/performance before changing the default model.
