# Koreanization build fix

The previous Koreanization pass accidentally translated Kotlin identifiers and Android API/import names, causing compilation errors. This version restores all source-code identifiers/imports from the known-good build while retaining the Korean UI strings and Korean AI prompts.

Also retains the multilingual Whisper model configuration and GitHub Actions Gradle wrapper permission fix.
