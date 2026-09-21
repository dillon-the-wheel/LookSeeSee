#!/usr/bin/env python3
"""
Synthesizes the end-screen bell melody from scratch (pure stdlib, no numpy).

This is an ORIGINAL melody composed by the app's author, written in jianpu
(numbered notation) in the key of G major:

  2(q) 3(dq) 5(dq) rest, 2(q) 3(dq) 5(dq) rest, 5(q) 6(dq) 3(16th) 1(8th) 2(5 beats)

  q = quarter, dq = dotted quarter, 16th = sixteenth, 8th = eighth

Rendered as soft additive bell-tone synthesis (inharmonic partials + exponential
decay) rather than any sampled/real instrument, so there's no third-party
audio asset or copyrighted performance involved.
"""
import math
import os
import struct
import wave

OUTPUT_PATH = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "app", "src", "main", "res", "raw", "end_theme.wav",
)

SAMPLE_RATE = 44100
BPM = 88
BEAT = 60.0 / BPM  # seconds per quarter note

# Key of G major: scale degree -> semitones above G4 (392.00 Hz)
DEGREE_SEMITONES = {1: 0, 2: 2, 3: 4, 4: 5, 5: 7, 6: 9, 7: 11}
G4 = 392.00


def freq_for_degree(degree: int, octave_offset: int = 0) -> float:
    semitones = DEGREE_SEMITONES[degree] + 12 * octave_offset
    return G4 * (2.0 ** (semitones / 12.0))


# (degree, duration_in_beats, ring_seconds) - None degree = rest
NOTE = "note"
REST = "rest"
MELODY = [
    (NOTE, 2, 1.0, 1.8),
    (NOTE, 3, 1.5, 2.0),
    (NOTE, 5, 1.5, 2.2),
    (REST, None, 1.0, 0.0),
    (NOTE, 2, 1.0, 1.8),
    (NOTE, 3, 1.5, 2.0),
    (NOTE, 5, 1.5, 2.2),
    (REST, None, 1.0, 0.0),
    (NOTE, 5, 1.0, 1.8),
    (NOTE, 6, 1.5, 2.2),
    (NOTE, 3, 0.25, 1.0),
    (NOTE, 1, 0.5, 1.2),
    (NOTE, 2, 5.0, 4.5),
]

# Slightly inharmonic partials for a bell-like timbre: (ratio, amplitude)
PARTIALS = [
    (1.00, 1.00),
    (2.00, 0.55),
    (2.76, 0.32),
    (4.07, 0.20),
    (5.40, 0.12),
]

DETUNE_CENTS = 4.0  # a second, slightly detuned fundamental for a soft chorus/shimmer


def render_note(buffer, start_sample, freq, ring_seconds, gain):
    n_samples = int(ring_seconds * SAMPLE_RATE)
    decay_rate = 5.0 / ring_seconds  # ~99% decayed by the end of the ring window
    detune_ratio = 2.0 ** (DETUNE_CENTS / 1200.0)
    attack_samples = int(0.006 * SAMPLE_RATE)

    for i in range(n_samples):
        t = i / SAMPLE_RATE
        envelope = math.exp(-decay_rate * t)
        if i < attack_samples:
            envelope *= i / attack_samples

        sample = 0.0
        for ratio, amp in PARTIALS:
            partial_freq = freq * ratio
            sample += amp * math.sin(2.0 * math.pi * partial_freq * t)
            sample += amp * 0.6 * math.sin(2.0 * math.pi * partial_freq * detune_ratio * t)

        value = sample * envelope * gain
        idx = start_sample + i
        if idx >= len(buffer):
            break
        buffer[idx] += value


def main():
    total_beats = sum(beats for (_, _, beats, _) in MELODY)
    tail_seconds = 4.5  # let the final note ring out past the nominal timeline
    total_seconds = total_beats * BEAT + tail_seconds
    total_samples = int(total_seconds * SAMPLE_RATE) + 1
    buffer = [0.0] * total_samples

    cursor_beats = 0.0
    for kind, degree, beats, ring_seconds in MELODY:
        start_sample = int(cursor_beats * BEAT * SAMPLE_RATE)
        if kind == NOTE:
            freq = freq_for_degree(degree)
            render_note(buffer, start_sample, freq, ring_seconds, gain=0.22)
        cursor_beats += beats

    peak = max(1e-9, max(abs(s) for s in buffer))
    target_peak = 0.85
    scale = target_peak / peak

    frames = bytearray()
    for s in buffer:
        v = max(-1.0, min(1.0, s * scale))
        frames += struct.pack("<h", int(v * 32767))

    with wave.open(OUTPUT_PATH, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(SAMPLE_RATE)
        wf.writeframes(bytes(frames))

    print(f"Wrote end_theme.wav: {total_seconds:.2f}s, {total_samples} samples")


if __name__ == "__main__":
    main()
