# Korean Whisper model fix

Fixed two issues in the previous build:
1. Whisper multilingual model URLs now use the actual `base-encoder.int8.onnx`,
   `base-decoder.int8.onnx`, and `base-tokens.txt` filenames from
   `csukuangfj/sherpa-onnx-whisper-base`.
2. GitHub Actions makes `android/gradlew` executable before running Gradle.

The app continues to force Whisper language to Korean (`ko`).
