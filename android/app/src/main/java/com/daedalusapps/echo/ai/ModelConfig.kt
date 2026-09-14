package com.daedalusapps.echo.ai

import android.content.Context
import java.io.File

data class LocalModel(
    val id: String,
    val displayName: String,
    val description: String,
    val downloadUrl: String,
    val filename: String,
    val sizeBytes: Long,
    val useGpu: Boolean = false
)

val GEMMA3_1B = LocalModel(
    id          = "gemma3_1b",
    displayName = "Gemma 3 1B",
    description = "약 555 MB · 최신 아키텍처 · 한국어 지시 이해 및 요약",
    downloadUrl = "https://huggingface.co/t-ghosh/gemma-tflite/resolve/main/gemma3-1B-it-int4.task",
    filename    = "gemma3-1B-it-int4.task",
    sizeBytes   = 555_000_000L,
    useGpu      = false
)

// Whisper Base 다국어 모델 (sherpa-onnx int8 양자화) — 개별 파일 다운로드
const val WHISPER_ENCODER_FILE = "base-encoder.int8.onnx"
const val WHISPER_DECODER_FILE = "base-decoder.int8.onnx"
const val WHISPER_TOKENS_FILE  = "base-tokens.txt"
private const val WHISPER_HF = "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-base/resolve/main"
const val WHISPER_ENCODER_URL  = "$WHISPER_HF/base-encoder.int8.onnx"
const val WHISPER_DECODER_URL  = "$WHISPER_HF/base-decoder.int8.onnx"
const val WHISPER_TOKENS_URL   = "$WHISPER_HF/base-tokens.txt"
const val WHISPER_TOTAL_BYTES  = 161_000_000L  // ~161 MB combined (multilingual base, int8)

// Universal Sentence Encoder Lite — used by EmbeddingService for semantic note search
const val EMBEDDING_MODEL_FILE = "universal_sentence_encoder.tflite"
const val EMBEDDING_MODEL_SIZE_BYTES = 26_000_000L  // ~26 MB
const val EMBEDDING_MODEL_URL =
    "https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/latest/universal_sentence_encoder.tflite"

fun embeddingModelFile(context: Context): File =
    File(modelsDir(context), EMBEDDING_MODEL_FILE)

fun modelsDir(context: Context): File =
    File(context.filesDir, "models").also { it.mkdirs() }

fun whisperModelDir(context: Context): File =
    File(modelsDir(context), "sherpa-whisper-base")

fun isWhisperReady(context: Context): Boolean {
    val dir = whisperModelDir(context)
    return File(dir, WHISPER_ENCODER_FILE).exists() &&
           File(dir, WHISPER_DECODER_FILE).exists() &&
           File(dir, WHISPER_TOKENS_FILE).exists()
}

fun modelFile(context: Context): File =
    File(modelsDir(context), GEMMA3_1B.filename)

fun areAllModelsReady(context: Context): Boolean {
    return modelFile(context).exists() &&
           isWhisperReady(context) &&
           embeddingModelFile(context).exists()
}

fun selectedModel(context: Context): LocalModel = GEMMA3_1B
