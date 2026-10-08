package com.khuongnd.dexassistant.ui

import android.app.UiAutomation
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.routing.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Parser-only local aggregate scoring. Never constructs execution/controller/network classes. */
class SherpaDryRunScoreTest {
    private fun engine(): LocalCommandEngine {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return LocalCommandEngine(CommandRegistry.fromJson(context.assets.open("commands.json").bufferedReader().use { it.readText() }, BuildConfig.VERSION_CODE))
    }
    @Test fun fixtureUnparseableReferencesNeverCountAsCorrect() {
        val e = engine()
        val refs = mapOf("sample001" to "đặt âm lượng năm mươi phần trăm", "sample002" to "đặt âm lượng 50 phần trăm")
        assertNull(e.resolve(refs.getValue("sample001"), null))
        assertNotNull(e.resolve(refs.getValue("sample002"), null))
        val rows = JSONArray().put(row("sample001", refs.getValue("sample001"))).put(row("sample002", "đặt âm lượng 60 phần trăm"))
        val score = aggregate(e, refs, rows).getJSONObject("1")
        assertEquals(1, score.getInt("referenceUnscorableN"))
        assertEquals(1, score.getInt("scorableReferenceN"))
        assertEquals(1, score.getInt("exactCommandIdN"))
        assertEquals(0, score.getInt("exactTypedActionN"))
        assertEquals(0, score.getInt("exactTypedParamsGivenSameCommandN"))
    }
    @Test fun fixtureOppositionAndFirstRepetitionAreCounted() {
        val rows = JSONArray().put(row("sample001", "tắt wifi")).put(row("sample001", "bật wifi").put("repeat", 2))
        val score = aggregate(engine(), mapOf("sample001" to "bật wifi"), rows).getJSONObject("1")
        assertEquals(1, score.getInt("samplesN"))
        assertEquals(1, score.getInt("wrongOppositionN"))
        assertEquals(0, score.getInt("exactCommandIdN"))
    }
    private fun row(id: String, hypothesis: String) = JSONObject().put("sampleId", id).put("threads", 1).put("repeat", 1).put("hypothesis", hypothesis)

    @Test fun scoreRealCorpusWithoutExecution() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Explicit dry-run scoring opt-in required", args.getString("runSherpaDryRunScore") == "true")
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val session = requireNotNull(args.getString("sherpaRealCorpusSession"))
        val filename = requireNotNull(args.getString("sherpaWavReport"))
        require(session.matches(Regex("session-[0-9]{13}")))
        require(filename.matches(Regex("wav-benchmark-[0-9]{13}\\.json")))
        val files = instrumentation.targetContext.filesDir
        val referenceFile = File(files, "sherpa-vi-real-corpus/$session/references.csv")
        val reportFile = File(files, "sherpa-vi-benchmark/sherpa-vi-results/$filename")
        require(reportFile.length() in 1..50_000_000)
        val report = JSONObject(reportFile.readText())
        require(report.getJSONObject("metadata").getString("corpusKind") == "consented_real_pilot")
        require(report.getJSONObject("metadata").getString("realCorpusSession") == session)
        val references = privateReferences(referenceFile)
        val summary = JSONObject().put("task", "T-017").put("status", "PARSER_ONLY_REAL_PILOT_NOT_ACCEPTANCE")
            .put("foregroundAppId", JSONObject.NULL).put("execution", "NONE")
            .put("referenceCount", references.size).put("byThreads", aggregate(engine(), references, report.getJSONArray("measurements")))
            .put("methods", "First measured repeat only; unparseable references excluded from accuracy denominator; exact command ID and typed-action data equality; slot equality conditioned on same command ID; known opposition pairs only; no routing/execution acceptance")
        val output = File(files, "sherpa-vi-benchmark/sherpa-vi-results/dry-run-score-${System.currentTimeMillis()}.json")
        output.writeText(summary.toString(2))
        instrumentation.sendStatus(0, android.os.Bundle().apply { putString("dryRunAggregateReport", output.absolutePath) })
    }

    private fun privateReferences(file: File): Map<String, String> {
        require(file.length() in 1..1_000_000)
        val lines = file.readLines()
        val header = lines.first().split(',')
        val idColumn = header.indexOf("sampleId"); val refColumn = header.indexOf("reference")
        val consentColumn = header.indexOf("consent"); val sourceColumn = header.indexOf("source")
        require(idColumn >= 0 && refColumn >= 0 && consentColumn >= 0 && sourceColumn >= 0)
        val result = linkedMapOf<String, String>()
        lines.drop(1).filter { it.isNotBlank() }.forEach {
            // Collector intentionally disallows commas/newlines in reviewed reference text.
            val cells = it.split(','); require(cells.size == header.size)
            require(cells[consentColumn] == "yes" && cells[sourceColumn] == "human_recording")
            val id = cells[idColumn]; val reference = cells[refColumn]
            require(id.matches(Regex("sample[0-9]{3}")) && reference.isNotBlank() && !result.containsKey(id))
            result[id] = reference
        }
        require(result.size in 1..200)
        return result
    }

    internal fun aggregate(engine: LocalCommandEngine, references: Map<String, String>, measurements: JSONArray): JSONObject {
        val first = linkedMapOf<Pair<Int, String>, JSONObject>()
        for (i in 0 until measurements.length()) {
            val row = measurements.getJSONObject(i)
            if (row.getInt("repeat") != 1) continue
            val key = row.getInt("threads") to row.getString("sampleId")
            require(key.first in listOf(1, 2, 4) && references.containsKey(key.second))
            require(first.put(key, row) == null) { "Duplicate first repetition" }
        }
        require(first.isNotEmpty())
        val result = JSONObject()
        first.keys.map { it.first }.distinct().sorted().forEach { threads ->
            var scorable = 0; var refUnparsed = 0; var hypUnparsed = 0; var parsed = 0
            var commandExact = 0; var typedExact = 0; var slotExact = 0; var opposition = 0
            val selected = first.filterKeys { it.first == threads }
            selected.forEach { (key, row) ->
                val reference = engine.resolve(references.getValue(key.second), null)
                val hypothesis = engine.resolve(row.getString("hypothesis"), null)
                if (reference == null) refUnparsed++ else {
                    scorable++
                    if (hypothesis == null) hypUnparsed++ else {
                        parsed++
                        if (reference.commandId == hypothesis.commandId) {
                            commandExact++
                            if (reference.typedAction.params == hypothesis.typedAction.params) slotExact++
                        }
                        if (reference.typedAction == hypothesis.typedAction) typedExact++
                        if (opposed(reference.typedAction, hypothesis.typedAction)) opposition++
                    }
                }
            }
            fun rate(n: Int, denominator: Int): Any = if (denominator == 0) JSONObject.NULL else n.toDouble() / denominator
            result.put(threads.toString(), JSONObject().put("samplesN", selected.size)
                .put("missingReferenceMeasurementsN", references.size - selected.size)
                .put("scorableReferenceN", scorable).put("referenceUnscorableN", refUnparsed)
                .put("parsedHypothesisScorableN", parsed).put("unparsedHypothesisScorableN", hypUnparsed)
                .put("exactCommandIdN", commandExact).put("exactCommandIdRate", rate(commandExact, scorable))
                .put("exactTypedActionN", typedExact).put("exactTypedActionRate", rate(typedExact, scorable))
                .put("sameCommandSlotDenominatorN", commandExact).put("exactTypedParamsGivenSameCommandN", slotExact)
                .put("exactTypedParamsGivenSameCommandRate", rate(slotExact, commandExact))
                .put("wrongOppositionN", opposition).put("wrongOppositionRate", rate(opposition, scorable)))
        }
        return result
    }

    private fun opposed(reference: TypedAction, hypothesis: TypedAction): Boolean {
        if (setOf(reference.action, hypothesis.action) == setOf(ActionId.FEED_NEXT, ActionId.FEED_PREVIOUS)) return true
        if (reference.action != hypothesis.action) return false
        val a = reference.params; val b = hypothesis.params
        if (reference.action in setOf(ActionId.WIFI_SET, ActionId.BLUETOOTH_SET, ActionId.YOUTUBE_FULLSCREEN)) return a["enabled"] != b["enabled"]
        val opposite = setOf(setOf("INCREASE", "DECREASE"), setOf("MUTE", "UNMUTE"), setOf("AUTO_ON", "AUTO_OFF"), setOf("PLAY", "PAUSE"), setOf("NEXT", "PREVIOUS"), setOf("FORWARD", "BACKWARD"))
        val key = if (reference.action == ActionId.SCROLL) "direction" else "operation"
        return setOf(a[key], b[key]) in opposite
    }
}
