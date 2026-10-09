"""Original, quiet 16-bar tabletop loop; no external music assets."""
import math,random,struct,wave
from pathlib import Path
rate=22050;beat=.5;duration=32;data=[0.0]*int(rate*duration)
progression=[(60,64,67),(57,60,64),(53,57,60),(55,59,62)]
def note(start,midi,length,gain):
 f=440*2**((midi-69)/12)
 for i in range(int(length*rate)):
  t=i/rate;idx=int(start*rate)+i
  if idx>=len(data):break
  envelope=min(1,t/.008)*math.exp(-t*6)*min(1,(length-t)/.08)
  data[idx]+=gain*envelope*(math.sin(2*math.pi*f*t)+.2*math.sin(4*math.pi*f*t))
for bar in range(16):
 chord=progression[(bar//2)%4];start=bar*4*beat
 for step in range(4):note(start+step*beat,chord[[0,1,2,1][step]]+12,.45,.12)
 note(start,chord[0]-12,.65,.12)
 note(start+2*beat,chord[0]-12,.65,.08)
 # Tiny filtered-noise paper percussion, not a sharp kick or clap.
 for step in [1,3]:
  rng=random.Random(bar*4+step);low=0
  for i in range(int(.08*rate)):
   t=i/rate;low+=.12*(rng.uniform(-1,1)-low);data[int((start+step*beat)*rate)+i]+=.08*low*math.exp(-t*65)*min(1,t/.003)
peak=max(abs(x) for x in data)
p=Path(__file__).resolve().parents[2]/'shared/src/commonMain/composeResources/files/audio/bgm.wav'
with wave.open(str(p),'wb') as w:
 w.setnchannels(1);w.setsampwidth(2);w.setframerate(rate);w.writeframes(struct.pack('<'+'h'*len(data),*[round(x*.7/peak*32767) for x in data]))
