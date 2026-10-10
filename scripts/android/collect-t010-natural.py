#!/usr/bin/env python3
"""Collect allowlisted timing metadata only; no audio, transcripts or screenshots.

Each capture creates a separate run, so rebooted/restarted epoch numbers cannot
merge across runs. Summaries describe observed candidates, never certify natural
speech, correct target selection, or acoustic endpoint latency.
"""
import argparse
import json
import pathlib
import re
import subprocess
import threading
import time
import uuid

PATTERNS = {
    'DexVoiceTiming': [r'epoch=\d+ stage=(?:wake_detected|stt_ready|speech_begin|speech_end|stt_final|screen_prepared|command_dispatch|command_terminal|stt_error|wake_requested|wake_worker_started|model_reused|model_ready|audio_recording|capture_active|feedback_requested|feedback_terminal) atMs=\d+(?: elapsedMs=\d+)?(?: status=[A-Z_0-9]*)?'],
    'DexLatency': [r'epoch=\d+ metric=[A-Za-z]+ durationUs=\d+',
        r'requestId=[a-f0-9-]{36} path=(?:local|semantic|fallback|remote|direct) terminalReason=[A-Z_0-9]+',
        r'requestId=[a-f0-9-]{36} path=(?:local|semantic|fallback|remote|direct) metric=[A-Za-z]+ durationUs=\d+',
        r'epoch=\d+ requestId=[a-f0-9-]{36} stage=correlated'],
    'DexScreen': [r'gesture displayId=\d+ target=ScreenRect\(left=\d+, top=\d+, right=\d+, bottom=\d+\)',
        r'session.invalidated stage=display_identity_geometry'],
    'DexNaturalTap': [r'count=\d+ displayId=\d+ x=[0-9.]+ y=[0-9.]+'],
}


def safe_metadata(line):
    # Accept only the precise brief-logcat envelope or our normalized saved form.
    # A permissive search could retain metadata embedded in a private message.
    envelope = re.fullmatch(r'(?:[VDIWEF]/)?(DexVoiceTiming|DexLatency|DexScreen|DexNaturalTap)(?:\(\s*\d+\))?(?::\s*| )(.*)', line.strip())
    if not envelope:
        return None
    tag, payload = envelope.groups()
    if any(re.fullmatch(pattern, payload) for pattern in PATTERNS[tag]):
        return tag + ' ' + payload
    return None


def summarize(lines):
    epochs, requests, metrics = {}, {}, {}
    gestures = taps = invalidations = 0
    for raw in lines:
        line = safe_metadata(raw)
        if line is None:
            continue
        fields = dict(re.findall(r'(\w+)=([^ ]+)', line))
        if 'epoch' in fields:
            epoch = epochs.setdefault(fields['epoch'], {'stages': {}, 'requests': set()})
            if 'stage' in fields and 'atMs' in fields:
                epoch['stages'].setdefault(fields['stage'], int(fields['atMs']))
            if 'requestId' in fields:
                epoch['requests'].add(fields['requestId'])
        if 'requestId' in fields and 'terminalReason' in fields:
            requests[fields['requestId']] = fields['terminalReason']
        if 'metric' in fields and 'durationUs' in fields:
            metrics.setdefault(fields['metric'], []).append(int(fields['durationUs']))
        gestures += line.startswith('DexScreen gesture ')
        taps += line.startswith('DexNaturalTap ')
        invalidations += line == 'DexScreen session.invalidated stage=display_identity_geometry'
    latency = {}
    for label, start, end in [('recognizerSpeechEndToFinalMs', 'speech_end', 'stt_final'),
                              ('finalToVoiceTerminalMs', 'stt_final', 'command_terminal'),
                              ('wakeToRecognizerReadyMs', 'wake_detected', 'stt_ready'),
                              ('finalToDispatchMs', 'stt_final', 'command_dispatch'),
                              ('recognizerSpeechEndToDispatchMs', 'speech_end', 'command_dispatch')]:
        values = sorted(e['stages'][end] - e['stages'][start] for e in epochs.values()
                        if start in e['stages'] and end in e['stages'] and e['stages'][end] >= e['stages'][start])
        if values:
            latency[label] = {'samples': len(values), 'p50': values[(len(values)-1)//2],
                              'p95': values[max(0, (95*len(values)+99)//100-1)], 'max': max(values)}
    reasons = {}
    for reason in requests.values():
        reasons[reason] = reasons.get(reason, 0) + 1
    return {'observedEpochs': len(epochs),
            'finalEpochs': sum('stt_final' in e['stages'] for e in epochs.values()),
            'correlatedTerminalEpochs': sum('stt_final' in e['stages'] and any(r in requests for r in e['requests']) for e in epochs.values()),
            'requestTerminalReasons': reasons, 'gestureEvents': gestures,
            'fixtureTapEvents': taps, 'displayInvalidations': invalidations,
            'latency': latency, 'metricSampleCounts': {k: len(v) for k, v in metrics.items()},
            'naturalAcceptanceCertified': False,
            'limitations': 'Owner observation must establish natural speech, intended target and outcome. Recognizer speech_end is not independently measured acoustic end. Taps alone do not prove sessions; samples across capture gaps may be incomplete.'}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--serial')
    p.add_argument('--output', required=True, help='Capture parent directory, or existing run directory with --summarize')
    p.add_argument('--summarize', action='store_true')
    a = p.parse_args()
    output = pathlib.Path(a.output)
    if a.summarize:
        runs = [output] if (output / 'timing.log').is_file() else sorted(output.glob('run-*'))
        summaries = {run.name: summarize((run / 'timing.log').read_text().splitlines())
                     for run in runs if (run / 'timing.log').is_file()}
        print(json.dumps({'runs': summaries, 'captureRuns': len(summaries),
                          'correlatedTerminalCandidatesAcrossRuns': sum(s['correlatedTerminalEpochs'] for s in summaries.values()),
                          'naturalAcceptanceCertified': False}, indent=2))
        return
    if not a.serial:
        p.error('--serial is required for capture')
    output = output / ('run-' + time.strftime('%Y%m%dT%H%M%S') + '-' + uuid.uuid4().hex[:8])
    output.mkdir(parents=True)
    print('Metadata run directory: ' + str(output), flush=True)
    adb = ['adb', '-s', a.serial]
    stop = threading.Event()
    def resources():
        with (output / 'resources.jsonl').open('w') as file:
            while not stop.is_set():
                record = {'hostTime': time.time()}
                for name, command, pattern in [
                    ('pssKb', ['shell', 'dumpsys', 'meminfo', 'com.khuongnd.dexassistant.ui'], r'TOTAL PSS:\s*(\d+)'),
                    ('battery', ['shell', 'dumpsys', 'battery'], r'(?m)^\s*(level|temperature):\s*(\d+)\s*$'),
                    ('cpu', ['shell', 'dumpsys', 'cpuinfo'], r'([0-9.]+)%\s+\d+/com\.khuongnd\.dexassistant\.ui:'),
                    ('thermal', ['shell', 'dumpsys', 'thermalservice'], r'Thermal Status:\s*(\d+)')]:
                    if stop.is_set():
                        break
                    try:
                        result = subprocess.run(adb + command, capture_output=True, text=True, timeout=10)
                        record[name] = re.findall(pattern, result.stdout.split('Battery History')[0]) if result.returncode == 0 else {'error': 'adb_failed'}
                    except subprocess.TimeoutExpired:
                        record[name] = {'error': 'timeout'}
                file.write(json.dumps(record) + '\n'); file.flush()
                stop.wait(15)
    worker = threading.Thread(target=resources, daemon=True)
    worker.start()
    process = subprocess.Popen(adb + ['logcat', '-T', '1', '-v', 'brief', 'DexVoiceTiming:I', 'DexLatency:I', 'DexScreen:I', 'DexNaturalTap:I', '*:S'], stdout=subprocess.PIPE, text=True)
    interrupted = False
    try:
        with (output / 'timing.log').open('w') as file:
            for line in process.stdout:
                safe = safe_metadata(line)
                if safe:
                    file.write(safe + '\n'); file.flush()
    except KeyboardInterrupt:
        interrupted = True
    finally:
        stop.set()
        if process.poll() is None:
            process.terminate()
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill(); process.wait()
        worker.join(timeout=11)
        summary = summarize((output / 'timing.log').read_text().splitlines())
        (output / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    if process.returncode not in (0, -15) and not (interrupted and process.returncode == -2):
        raise SystemExit('ADB log stream failed; partial metadata retained, acceptance incomplete')

if __name__ == '__main__':
    main()
