package com.daedalusapps.echo.ai

import android.content.Context
import com.daedalusapps.echo.data.model.Recording

const val OFFLINE_GUARDRAIL = "주의: 당신은 인터넷에 연결되지 않은 오프라인 비서입니다. 웹 검색이나 최신 정보 조회를 할 수 없습니다. 제공된 노트와 대화만 근거로 답변하세요. 사실을 임의로 추가하지 마세요. 모든 사용자에게 보이는 답변은 자연스러운 한국어로 작성하세요."

const val AI_TEXT_BUDGET_KEY = "ai_text_budget_chars"
// Text longer than the budget is split into chunks before LLM analysis.
// Default: 4096 total tokens − ~135 prompt − ~800 output = ~3160 tokens ≈ 12,600 chars.
const val AI_TEXT_BUDGET_DEFAULT = 12_000
private const val AI_TEXT_BUDGET_MIN = 2_000
// Ceiling keeps derived values (chunk size, todo batch = budget * 3 / 4) clear of Int overflow
// when a restored backup carries an out-of-range value.
private const val AI_TEXT_BUDGET_MAX = 100_000
// Headroom subtracted from the budget so a chunk plus its prompt still fits.
private const val CHUNK_HEADROOM_CHARS = 2_000
private const val CHUNK_OVERLAP_CHARS = 500

/** Reads the configured AI text budget (chars), clamped to a sane range. */
fun aiTextBudget(context: Context): Int =
    context.getSharedPreferences("daedalus_prefs", Context.MODE_PRIVATE)
        .getInt(AI_TEXT_BUDGET_KEY, AI_TEXT_BUDGET_DEFAULT)
        .coerceIn(AI_TEXT_BUDGET_MIN, AI_TEXT_BUDGET_MAX)

const val CHUNK_SUMMARY_PROMPT = OFFLINE_GUARDRAIL + "\n\n" + """이 회의 음성 기록 구간을 핵심만 담은 간결한 한국어 글머리표로 요약하세요.

한국어 글머리표만 반환하세요. 주요 내용은 "- "로, 하위 내용은 "  - "로 시작하세요. 실행할 일이 있다면 포함하세요. JSON, 제목, 서론은 반환하지 마세요.

Section:"""

fun chunkTranscript(transcript: String, budget: Int = AI_TEXT_BUDGET_DEFAULT): List<String> {
    if (transcript.length <= budget) return listOf(transcript)
    val chunkSize = (budget - CHUNK_HEADROOM_CHARS).coerceAtLeast(1_000)
    val chunks = mutableListOf<String>()
    var start = 0
    while (start < transcript.length) {
        val end = minOf(start + chunkSize, transcript.length)
        val splitAt = if (end < transcript.length) {
            val wb = transcript.lastIndexOf(' ', end)
            if (wb > start + chunkSize / 2) wb else end
        } else end
        chunks.add(transcript.substring(start, splitAt))
        if (splitAt >= transcript.length) break
        val overlapStart = transcript.indexOf(' ', maxOf(start + 1, splitAt - CHUNK_OVERLAP_CHARS))
        start = if (overlapStart in (start + 1) until splitAt) overlapStart + 1 else splitAt
    }
    return chunks
}

const val DEFAULT_PROMPT = OFFLINE_GUARDRAIL + "\n\n" + """아래 음성 기록을 읽고 내용을 분석하세요. 모든 결과는 한국어로 작성하세요.

정확히 다음 5개 키를 가진 JSON 객체 하나만 반환하세요. 마크다운이나 코드 펜스는 사용하지 마세요. 키 이름은 그대로 유지하되 모든 값은 한국어로 작성하세요.

- "title": 실제로 논의된 내용을 설명하는 구체적인 제목(최대 8단어)을 한국어로 작성
- "shortSummary": 발화자가 말한 내용을 구체적인 표현을 살려 한 문장의 한국어로 요약
- "topics": 음성 기록에서 뽑은 3~5개의 한국어 핵심 키워드 JSON 배열
- "mindMap": 음성 기록의 주요 내용을 한국어 글머리표로 작성. 각 항목은 "- "로 시작하고 하위 항목은 "  - "로 시작
- "fullSummary": 논의된 내용을 구체적인 주제를 살려 2~3개의 한국어 문장으로 요약

Transcript:"""

fun activePrompt(context: Context): String {
    val custom = context.getSharedPreferences("daedalus_prefs", Context.MODE_PRIVATE)
        .getString("custom_prompt", null) ?: return DEFAULT_PROMPT
    // A saved custom prompt is usually an edit of DEFAULT_PROMPT, so it already carries the
    // guardrail — appending unconditionally would repeat it.
    return if (custom.contains(OFFLINE_GUARDRAIL) && custom.contains("모든 사용자에게 보이는 답변")) custom else "$custom\n\n$OFFLINE_GUARDRAIL"
}

fun buildNoteQuestionPrompt(title: String, summary: String): String =
    "특정 노트에 대한 질문에 답변하는 한국어 비서입니다. " +
        "Note title: $title. " +
        "Note summary: $summary. " +
        "노트 내용만 근거로 한국어로 간결하게 답변하세요. " +
        "답이 노트에 없으면 한국어로 명확하게 없다고 말하세요.\n\n$OFFLINE_GUARDRAIL"

fun buildLibraryQuestionPrompt(sources: List<Recording>): String = buildString {
    append("아래 노트를 사용하여 질문에 한국어로 답변하세요. ")
    append("필요하면 노트 제목을 언급하세요. ")
    append("답이 노트에 없으면 한국어로 그렇다고 말하세요.\n\n")
    sources.forEachIndexed { i, r ->
        append("Note ${i + 1}: ${r.title.ifBlank { r.filename }}\n")
        append(r.shortSummary.ifBlank { r.summary.take(200) })
        append("\n\n")
    }
    append(OFFLINE_GUARDRAIL)
}

private val ACTION_PHRASES = listOf(
    "to do", "todo", "need to", "should ", "want to", "going to",
    "remember", "remind", "add ", "create ", "implement", "aggregate",
    "extract", "figure out", "look into", "try to", "have to"
)

fun extractActionItems(transcript: String): List<String> {
    val lower = transcript.lowercase()
    if (ACTION_PHRASES.none { lower.contains(it) }) return emptyList()

    return transcript
        .split(Regex("[,.]\\s+|\\s+(?:and then|but then|also|so |then )"))
        .map { it.trim() }
        .filter { chunk ->
            chunk.length in 15..120 &&
            ACTION_PHRASES.any { chunk.lowercase().contains(it) }
        }
        .map { it.replaceFirstChar { c -> c.uppercaseChar() }.trimEnd('.', ',') }
        .distinctBy { it.lowercase().take(25) }
        .take(5)
}

/**
 * Checks whether a transcript contains readable, meaningful speech suitable for summarization.
 *
 * Filters out:
 * - Empty or blank transcripts
 * - Transcripts containing only non-speech descriptors (e.g. `[laughter]`, `(music)`)
 * - Very short transcripts (< 3 words)
 * - Whisper loop hallucinations where a 1 to 4 word phrase repeats across the entire transcript
 *
 * @param transcript The raw transcript text to validate.
 * @return `true` if the transcript is readable and suitable for AI analysis; `false` otherwise.
 */
fun isTranscriptReadable(transcript: String): Boolean {
    val trimmed = transcript.trim()
    if (trimmed.isEmpty()) return false

    // Remove bracket descriptors like [laughter], (music)
    val clean = trimmed.replace(Regex("\\[.*?\\]|\\(.*?\\)"), "").trim()
    if (clean.isEmpty()) return false

    val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.size < 3) return false

    // Normalize words for repetition checks (strip punctuation, lowercase)
    val normalizedWords = words.map { word ->
        word.filter { it.isLetterOrDigit() }.lowercase()
    }.filter { it.isNotBlank() }

    if (normalizedWords.size < 3) return false

    // Check for Whisper loop hallucinations (repeating word sequences)
    if (normalizedWords.size >= 8) {
        for (phraseLen in 1..4) {
            val chunk = normalizedWords.take(phraseLen)
            var isRepeating = true
            var index = 0
            while (index < normalizedWords.size) {
                val remaining = normalizedWords.size - index
                if (remaining >= phraseLen) {
                    val nextChunk = normalizedWords.subList(index, index + phraseLen)
                    if (nextChunk != chunk) {
                        isRepeating = false
                        break
                    }
                    index += phraseLen
                } else {
                    val tailChunk = normalizedWords.subList(index, normalizedWords.size)
                    if (tailChunk != chunk.subList(0, remaining)) {
                        isRepeating = false
                        break
                    }
                    index += remaining
                }
            }
            if (isRepeating) return false
        }
    }

    return true
}


