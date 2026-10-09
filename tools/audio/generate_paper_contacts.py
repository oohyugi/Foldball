"""Create paper-ball contact sounds from the bundled crumple texture and filtered noise."""
from pathlib import Path
import math
import random
import struct
import wave

ROOT = Path(__file__).resolve().parents[2]
AUDIO = ROOT / 'shared/src/commonMain/composeResources/files/audio'
with wave.open(str(AUDIO / 'crumple.wav')) as source:
    assert source.getnchannels() == 1 and source.getsampwidth() == 2
    rate = source.getframerate()
    raw = source.readframes(source.getnframes())
    texture = [x / 32768 for x in struct.unpack('<' + 'h' * (len(raw) // 2), raw)]

for name, duration, offset, texture_gain, thud_gain in [
    ('bounce', .145, .22, .40, .28),
    ('impact', .175, .48, .52, .38),
]:
    rng = random.Random(921 if name == 'bounce' else 922)
    low = 0.0
    texture_low = 0.0
    samples = []
    start = int(offset * rate)
    for i in range(int(duration * rate)):
        t = i / rate
        low += .025 * (rng.uniform(-1, 1) - low)
        paper = texture[(start + i) % len(texture)]
        texture_low += .15 * (paper - texture_low)
        # A short crinkle transient sits above a muffled, non-pitched contact.
        attack = min(1.0, t / .002)
        tail = max(0.0, 1 - t / duration) ** 2
        rustle = (paper - texture_low) * math.exp(-t / .035)
        contact = low * math.exp(-t / .018)
        samples.append(attack * tail * (texture_gain * rustle + thud_gain * contact))
    peak = max(abs(x) for x in samples)
    target = .65 if name == 'bounce' else .75
    pcm = [round(max(-1, min(1, x * target / peak)) * 32767) for x in samples]
    with wave.open(str(AUDIO / (name + '.wav')), 'wb') as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(rate)
        output.writeframes(struct.pack('<' + 'h' * len(pcm), *pcm))
    print(f'{name}: {duration * 1000:.0f} ms, mono PCM, peak {target}')
