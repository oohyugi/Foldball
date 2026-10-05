# Product Requirements Document
## Foldball — Paper Soccer Game

## 1. Overview

**Foldball** adalah game casual berbasis permainan sepak bola dari lipatan kertas.

Game dimainkan di arena 2D dari sudut pandang atas dengan dua gawang di sisi atas dan bawah. Pemain mengontrol tim berupa bidak/lipatan kertas.

Permainan menggunakan sistem giliran yang ditentukan berdasarkan pemain yang paling dekat dengan posisi bola setelah bola berhenti.

Core experience:

**Atur formasi → atur arah tendangan → tentukan power → tendang → bola memantul → bola berhenti → giliran berikutnya.**

Target utama:

- mudah dimainkan anak usia 4–8 tahun;
- tetap seru untuk remaja dan dewasa;
- memiliki strategi ringan;
- sesi permainan pendek;
- dapat dipahami dengan minim teks;
- mempertahankan feel permainan sepak bola kertas.

---

## 2. Target User

### Primary

- Casual player semua umur
- Anak usia 4–8 tahun dengan kontrol sederhana

### Secondary

- Orang tua yang bermain bersama anak
- Pemain yang menyukai permainan tradisional
- Pemain casual sports game

---

## 3. Platform

### MVP

- Android
- Portrait orientation

### Future

- iOS
- Tablet

---

## 4. Core Gameplay

Arena menggunakan tampilan **top-down portrait**.

```text
┌─────────────────────┐
│       GAWANG B      │
│                     │
│       ▲ DEF         │
│   ▲ MID   ▲ MID     │
│       ▲ ATT         │
│                     │
│          ⚽          │
│                     │
│       ● ATT         │
│   ● MID   ● MID     │
│       ● DEF         │
│                     │
│       GAWANG A      │
└─────────────────────┘
```

Untuk MVP:

- 4 pemain Tim A: defender, left mid, right mid, attacker
- 4 pemain Tim B dengan susunan cermin Tim A
- 1 bola
- tidak ada kiper khusus

Defender dapat ditempatkan untuk menjaga gawang.

---

## 5. Match Objective

Tujuan permainan adalah memasukkan bola ke gawang lawan.

Setiap gol bernilai:

**1 point**

Default victory condition:

**First to 3 goals.**

Future:

- First to 5
- Time Attack
- 2 menit
- 3 menit

---

## 6. Opening Toss

Sebelum kickoff pertama, dilakukan **Opening Toss**.

Kertas diremas lewat sprite sheet PNG delapan frame sampai seukuran bola ketika halaman match dibuka, tanpa teks instruksi. Tombol **Start** muncul tepat di bawah bola. Saat ditekan, bola kertas memantul secara acak dan berhenti di sisi tim hasil undian. Hasil dipilih dengan peluang seimbang sebelum animasi dimulai; sesudah hasil terlihat, match otomatis masuk ke kickoff pertama. Jam pertandingan baru berjalan setelah undian selesai.

### Flow

```text
Start Match
↓
Tombol Toss
↓
User tap Toss
↓
Bola dilempar ke arena
↓
Bola bounce
↓
Bola berhenti di salah satu sisi
↓
Tentukan tim kickoff
↓
Reset bola ke tengah
↓
Kickoff
```

Arena dibagi dua:

```text
┌─────────────────────┐
│      AREA TIM B     │
│                     │
│---------------------│
│      AREA TIM A     │
│                     │
└─────────────────────┘
```

Jika bola berhenti di area Tim A:

**Tim A mendapat kickoff.**

Jika bola berhenti di area Tim B:

**Tim B mendapat kickoff.**

Setelah toss selesai, bola selalu kembali ke titik tengah.

---

## 7. Opening Toss Interaction

Pada mode Single Player vs Bot, user selalu menekan:

**Toss**

Bot tidak melakukan interaction secara otomatis.

Saat ditekan:

```text
Tap Toss
↓
bola naik
↓
bola jatuh
↓
bounce
↓
berhenti
```

---

## 8. Opening Toss Fairness

Hasil Opening Toss menggunakan probabilitas:

**50% Tim A / 50% Tim B**

Untuk menjaga fairness, hasil ditentukan terlebih dahulu.

```text
winner = random(A, B)
```

Animasi kemudian dibuat agar bola berhenti secara natural pada sisi pemenang toss.

---

## 9. Local Multiplayer Toss

Pada Local 2 Player:

- tombol Toss berada di tengah;
- siapa saja boleh menekan;
- pemain yang menekan tidak mendapat keuntungan;
- hasil tetap 50:50.

---

## 10. Turn System

Giliran tidak menggunakan pola:

```text
A → B → A → B
```

Setelah bola berhenti:

1. sistem mencari satu pemain terdekat dari masing-masing tim berdasarkan posisi fisik terhadap bola;
2. jika selisih kedua jarak kurang dari 6 unit lapangan, giliran diberikan kepada tim yang bukan penendang sebelumnya;
3. jika tidak seri, tim dengan pemain terdekat mendapat giliran;
4. pemain terdekat dari tim terpilih otomatis menjadi penendang. Jarak persis sama dalam satu tim dipecahkan dengan ID pemain yang stabil.

```text
Bola berhenti
↓
Cari pemain terdekat tiap tim
↓
Bandingkan jarak dan aturan seri
↓
Turn Tim A
↓
A2 menjadi penendang
```

Tim yang sama dapat memperoleh giliran berturut-turut jika pemainnya lebih dekat dari ambang seri. Zona hanya membatasi penempatan pemain; posisi fisik menentukan giliran.

---

## 11. Penendang

Penendang tidak dipilih manual.

Penendang selalu:

**pemain yang paling dekat dengan bola saat bola berhenti.**

Setelah ditentukan:

1. pemain otomatis bergerak menuju bola;
2. pemain menjadi **aktif/fokus** dan ditempatkan tetap di belakang bola;
3. pemain masuk ke mode aiming;
4. arah dan power diatur melalui control aim/power, bukan dengan memindahkan penendang;
5. penendang tidak dapat direposition selama gilirannya aktif.

```text
●             ⚽

↓ nearest player

          ●⚽
```

---

## 12. Status dan Pergerakan Player

Setiap player memiliki status interaksi yang mengikuti fase permainan:

| Status | Peran | Gerakan yang diizinkan | Tampilan |
| --- | --- | --- | --- |
| **Inactive** | Player selain penendang yang sedang fokus | Maksimal dua pemain dari tim bertahan dapat di-drag ke posisi legal sebelum tendangan | Pose netral, tanpa highlight fokus |
| **Active / Focused** | Penendang yang dipilih otomatis karena paling dekat dengan bola | Bergerak otomatis ke belakang bola; posisi gameplay terkunci. Drag horizontal mengatur aim; meter power bergerak otomatis selama drag | Highlight fokus dan animasi fokus ringan |

Status Active/Focused dimulai ketika nearest player dipilih dan berakhir setelah tendangan serta physics selesai. Setelah bola berhenti, nearest player berikutnya menjadi fokus. Status menentukan interaksi yang tersedia; animasi visual tidak mengubah posisi physics atau collider.

## 13. Formation Phase

Sebelum tendangan pada giliran biasa, tim yang tidak menendang mendapat **10 detik** untuk mengatur maksimal dua pemain **Inactive**. Tombol Selesai dapat mengakhiri fase ini lebih awal. Undo hanya tersedia selama waktu bertahan. Setelah itu tim penendang mendapat **8 detik** untuk aim dan menendang. Pada formasi kickoff pembuka, tim bertahan boleh memindah pemain tetapi hanya di setengah lapangannya sendiri sampai bola dilepas. Kedua countdown berhenti saat pertandingan dijeda. Angka 10/8 detik adalah nilai awal `MatchTimingConfig` per pertandingan; pengaturan durasi di masa depan mengisi config sebelum match dimulai.

Player **Active/Focused** tidak dapat direposition oleh user. Sistem menempatkannya otomatis di belakang bola. Saat user mulai drag untuk membidik, meter power mulai bergerak bolak-balik. Posisi gameplay dan collider tetap di tempat. Saat release, nilai meter saat itu menentukan power; bidak snap ke depan dan sedikit overshoot lalu settle, sementara bola mulai bergerak tepat ketika snap dimulai.

Maksimal dua pemain Inactive dari tim bertahan dapat di-drag selama Formation Phase.

Jika countdown menyerang habis saat power sudah terisi, sistem melepas tendangan otomatis memakai power terakhir yang terisi. Jika belum ada power yang terisi, bola tetap di posisi terakhir; penendang lama mundur dari bola terlebih dahulu, lalu pemain tim bertahan yang paling dekat dengan bola bergerak ke belakang bola sebagai penendang baru. Countdown giliran baru dimulai setelah animasi pendekatan selesai.

Tujuannya:

- membuka ruang;
- menutup jalur lawan;
- menjaga gawang;
- mempersiapkan pantulan;
- menciptakan posisi serangan berikutnya.

---

## 14. Zone Reposition Rules

Lapangan dibagi menjadi **18 zona** dalam 3 kolom × 6 baris. Nomor dimulai dari kiri bawah dan berurutan ke kanan, lalu naik ke baris berikutnya.

```text
          GAWANG B
┌──────┬──────┬──────┐
│  16  │  17  │  18  │
├──────┼──────┼──────┤
│  13  │  14  │  15  │
├──────┼──────┼──────┤
│  10  │  11  │  12  │
├──────┼──────┼──────┤
│  7   │  8   │  9   │
├──────┼──────┼──────┤
│  4   │  5   │  6   │
├──────┼──────┼──────┤
│  1   │  2   │  3   │
└──────┴──────┴──────┘
          GAWANG A
```

Susunan awal mengikuti zona masing-masing tim. Posisi gameplay pemain berada di pusat sprite dan area tabrakannya agar susunan kedua tim tampak simetris. Defender kedua tim memakai offset maju yang sama besar di dalam zonanya; defender Tim A di zona 2 dan defender Tim B di zona 17. Left mid dan right mid kedua tim juga memakai offset maju yang sama besar, mengikuti arah serangan timnya: Tim A di zona 4 dan 6, Tim B di zona 13 dan 15. Striker tim yang tidak mengambil kickoff mundur dengan offset yang sama; kicker memulai dari zona 8 untuk Tim A atau zona 11 untuk Tim B. Setelah approach, kicker berdiri di belakang bola.

Sebelum tim penendang menembak, tim bertahan boleh mereposisi **maksimal dua pemain berbeda**. Setiap pemain dapat berpindah maksimal **satu zona** ke salah satu dari delapan zona tetangga (horizontal, vertikal, atau diagonal), atau tetap di zona asal. Pemain dapat pindah ke zona mana pun di grid; tidak ada batas zona berdasarkan peran.

Zona tujuan legal jika kosong dari **semua pemain lain**, baik teman maupun lawan, memiliki titik yang aman dari bola, pemain lain, dan tepi lapangan, serta dapat dicapai lewat lintasan drag yang aman. Pilih satu pemain untuk melihat zona tetangga yang legal dengan garis tepi putus-putus; jangan gabungkan tujuan dari beberapa pemain atau menggambar garis penghubung. Pemain mengikuti jari di dalam zona asal atau zona tetangga yang legal. Jika pusat zona terhalang tetapi tepinya aman, drag ke zona tersebut menempatkan pemain di titik aman itu. Gerakan berhenti pada titik legal terakhir bila mencapai zona ilegal, tepi lapangan, bola, atau pemain lain. Pemain yang sudah bergerak tidak dapat dipindahkan lagi pada giliran itu. Setelah pemain dipindahkan dan dilepas, satu jatah reposisi terpakai. Tombol Undo mengembalikan perpindahan terakhir beserta jatahnya dan dapat dipakai berulang hingga semua perpindahan dibatalkan; Undo tidak tersedia setelah tendangan.

Benturan bola tetap menggerakkan pemain secara halus melalui physics; perpindahan karena benturan tetap dibatasi oleh radius dari posisi terakhir yang ditetapkan. Jarak antar badan pemain dijaga saat benturan dan pendekatan penendang agar bidak tidak saling menumpuk. MVP dimulai dengan 4 pemain per tim. Rekomendasi batas atas roster untuk lapangan 18 zona adalah 6 pemain per tim.

---

## 15. Aiming Mechanic

Setelah formation phase, player aktif/fokus masuk ke **Aiming Mode** dan tetap berada di belakang bola.

Aim guide tidak tampil terus saat giliran dimulai. Guide muncul saat user mulai menggeser penendang aktif. Drag horizontal mengubah sudut aim; selama gesture berjalan, meter power bergerak otomatis bolak-balik. Gerakan vertikal tidak mengubah aim maupun power, dan posisi gameplay penendang tetap.

**Drag horizontal pada penendang aktif menentukan arah awal tendangan.** Bidak tetap terkunci di posisi gameplay. Badannya boleh miring sedikit ke arah bidikan sebagai feedback visual, tetapi kemiringan ini tidak menentukan arah tendangan dan badan tidak diputar mengikuti sudut bidik secara penuh.

```text
             ↑ arah bola
             │
             ⚽
           ●

       ↙         ↘
   aim kiri    aim kanan
```

Menggeser penendang aktif ke kiri atau kanan mengubah sudut tendangan. Arah umum tetap menuju sisi lapangan lawan. Selama drag, user dapat mengoreksi aim sambil melihat guide dan meter power. Lepas gesture untuk menendang dengan nilai power yang sedang ditunjuk meter.

Guide line mengikuti sudut aim dan menunjukkan arah awal bola. Pose aim kiri/kanan pada aset diterapkan sebagai kemiringan visual ringan saja.

## 16. Aim Guide

Saat user mulai menggeser penendang aktif, sistem menampilkan **guide line**. Guide mengikuti aim yang tersimpan selama fase persiapan dan hilang setelah tendangan dimulai.

Guide line menunjukkan arah awal bola.

```text
             ───────►
          ⚽
        ●
```

Guide harus:

- jelas;
- cukup tebal;
- mudah dilihat;
- tidak terlalu teknis;
- bergerak real-time mengikuti posisi penendang.

Guide line hanya menampilkan:

**arah awal tendangan.**

MVP tidak menampilkan seluruh prediksi lintasan bola.

---

## 17. Wall Bounce Prediction

Pada MVP, guide line tidak perlu menunjukkan pantulan.

Future enhancement:

```text
⚽────────►│
          │
          ╲
           ╲
```

Dapat ditambahkan opsi untuk menampilkan:

- first bounce;
- approximate reflection angle.

Full trajectory prediction tidak termasuk MVP.

---

## 18. Shot Power

Power ditentukan dengan timing. Saat user mulai drag penendang aktif, penunjuk pada meter power bergerak bolak-balik dari rendah ke tinggi dan kembali. User mengatur aim dengan gerakan horizontal; gerakan vertikal tidak memengaruhi aim atau power. Release mengunci nilai power yang sedang ditunjuk meter dan langsung menendang.

```text
       arah bola
          ↑
          │
         ⚽
         ●
    meter power ↔
```

User memilih momen release untuk menentukan kekuatan tendangan. Meter berhenti saat release; indikator dan angka power menunjukkan nilai yang dipakai.

---

## 19. Shooting Interaction

Full shooting interaction:

```text
Bola berhenti
↓
Nearest player menjadi active/focused
↓
Penendang bergerak otomatis ke belakang bola
↓
Formation phase
↓
Tim bertahan: 10 detik, maksimal dua player inactive; Undo atau Selesai
↓
Tim menyerang: 8 detik untuk drag horizontal penendang aktif
↓
Guide line mengikuti aim; meter power bergerak bolak-balik
↓
Release saat meter menunjukkan power yang diinginkan
↓
Tendangan dilakukan
```

Jika 8 detik menyerang habis tanpa tendangan: penendang lama mundur, bola tetap, pemain bertahan terdekat menjadi penendang baru, lalu fase bertahan berikutnya dimulai.

Selama drag aktif, gerakan horizontal hanya mengatur aim dan meter power bergerak otomatis. Gerakan vertikal diabaikan. Release mengakhiri gesture, mengambil nilai power saat itu, lalu menendang.

Gesture utama:

### Aim dan timing power

```text
drag focused kicker left/right → aim
power indicator sweeps low ↔ high automatically
release → shoot at the current power
```

### Shoot

```text
release
```

---

## 20. Power Indicator

Saat drag aim aktif, indikator power tampil sebagai overlay vertikal di sisi kanan lapangan, mengikuti referensi visual: bingkai hitam bertepi putih, tujuh blok warna merah di atas hingga hijau di bawah, panah putih di sisi kanan yang menunjuk ke blok aktif, dan label kertas `POWER` di bawah. Penunjuk bergerak naik-turun; merah menandakan power tinggi dan hijau power rendah. Overlay hanya tampil selama gesture aktif, lalu menghilang setelah release atau gesture dibatalkan. Nilai indikator saat release menentukan kekuatan tendangan.

Power dapat menggunakan 3 level visual:

```text
LOW
MEDIUM
HIGH
```

atau meter sederhana:

```text
▰▰▰▱▱
```

Untuk anak kecil, indikator tidak perlu menggunakan angka.

---

## 21. Shot Feedback

Saat user melepas gesture aim:

- penendang melakukan animasi kick/spring dengan recoil singkat berlawanan arah tendangan;
- bola mendapat velocity;
- guide line menghilang;
- formation mode terkunci;
- physics phase dimulai.

---

## 22. Player Asset System

Pemain menggunakan bentuk **lipatan kertas sederhana**, tanpa gambar manusia atau wajah.

Asset harus modular agar mendukung customization.

Layer yang direkomendasikan:

```text
Player
├── back paper panel
├── front paper panel
├── jersey color layer
├── pattern layer
├── player number
├── player name
├── outline
└── shadow
```

Nomor dan nama dirender secara runtime.

Warna dan pattern jersey juga diterapkan secara runtime.

Pada kamera lapangan yang tetap, Tim A/Merah di bawah terlihat dari belakang saat menyerang ke atas, sedangkan Tim B/Biru di atas terlihat dari depan saat menyerang ke bawah. Detail panel dan posisi nama membedakan kedua sisi; tulisan tetap tegak dan mudah dibaca.

Bagian bawah pemain tetap terbuka tanpa lipatan tambahan agar sesuai dengan bentuk permainan kertas asli dan mendukung animasi pegas.

---

## 23. Player Animation

Tidak menggunakan sprite sheet sebagai sistem utama.

Animasi menggunakan transform procedural.

Contoh:

```text
Idle:
scaleY = 1.0

Pressed:
scaleY = 0.55–0.65

Release:
scaleY = 1.10–1.16

Settle:
scaleY = 1.0
```

Transform dilakukan dengan pivot di bagian bawah pemain.

Animasi player MVP:

- **Idle / settle:** setelah bergerak atau berbenturan, player wobble kecil lalu kembali ke pose tegak. Pose idle tidak memakai gerak berulang yang mencolok.
- **Inactive / reposition:** player nonaktif mengikuti drag selama Formation Phase; badan miring ringan mengikuti arah gerak dan melakukan bounce kecil saat dilepas.
- **Active / focus:** player aktif memiliki highlight di ground dan pulse/spring halus saat fokus berpindah kepadanya. Player nonaktif tidak memakai highlight atau pulse fokus.
- **Auto-move kicker:** player terpilih bergerak ke posisi tetap di belakang bola, lalu settle. Gunakan gerak translasi, bukan pose berjalan.
- **Aim:** player fokus tetap di belakang bola. Drag horizontal menentukan arah; badan miring sedikit mengikuti arah bidik sebagai feedback tanpa bergeser dari posisi gameplay atau diputar penuh.
- **Power timing:** saat aim gesture aktif, deformasi bidak mengikuti nilai meter yang bergerak bolak-balik. Posisi gameplay dan collider tidak berubah.
- **Kick / release:** saat release, bidak snap ke depan dengan overshoot kecil lalu settle. Bola mulai bergerak tepat pada awal fase snap; nilai meter saat release menentukan tenaga dan jarak tendangan, sementara animasi visual tidak menunda physics.
- **Collision:** player menerima dorongan visual kecil, miring sesaat, lalu spring kembali ke posisi fisiknya.

Animasi focus, aim, power timing, dan kick harus mempertahankan perbedaan yang jelas antara status fokus, arah bidik, nilai meter, dan gerak kick saat release. Terapkan transform badan dengan pivot di bagian bawah pemain. Scale, bend, squash, stretch, tilt, dan offset visual saat snap tidak mengubah posisi physics atau collider.

---

## 24. Ball Visual

Bola menggunakan konsep:

**crumpled paper ball / bola dari kertas yang diremas.**

Bentuk visual boleh sedikit tidak sempurna, namun collider physics tetap berbentuk lingkaran.

Future customization:

- white paper
- notebook paper
- newspaper
- colored paper
- patterned paper

---

## 25. Ball Animation System

Animasi bola bersifat **physics-driven**, bukan fixed sprite animation.

### Rolling

Rotasi bola mengikuti jarak tempuh.

```text
deltaAngleRad = distanceTravelled / ballRadius
```

Semakin cepat bola bergerak, semakin cepat rotasinya.

### Optional Rotation Variants

Dapat menggunakan sprite sheet 8 frame sebagai variasi tekstur berdasarkan angle.

Frame tidak diganti berdasarkan timer, tetapi berdasarkan sudut rotasi.

```text
0–45°     → frame 1
45–90°    → frame 2
90–135°   → frame 3
...
```

### Air / Bounce

Ketika bola melambung:

```text
renderY = groundY - visualHeight
```

Shadow tetap berada di ground position.

Semakin tinggi bola:

- shadow semakin kecil;
- opacity shadow semakin rendah.

### Landing

Gunakan squash & stretch ringan.

```text
normal      1.00 x 1.00
impact      1.15 x 0.85
recover     0.96 x 1.08
normal      1.00 x 1.00
```

Collider physics tidak ikut berubah.

### Effects

Gunakan sprite/particle terpisah untuk:

- dust
- impact
- speed trail
- landing puff

Effects tidak memengaruhi physics.

---

## 26. Physics

Object yang memiliki physics:

- bola;
- pemain;
- arena wall.

Physics harus:

- responsif;
- mudah diprediksi;
- sedikit exaggerated;
- tidak terlalu realistis.

---

## 27. Arena Wall

Tidak ada bola keluar lapangan.

Seluruh sisi arena menggunakan **wall bounce** seperti padel atau air hockey.

```text
⚽ → │

     ↙ ⚽
```

Keuntungan:

- tidak ada throw-in;
- tidak ada corner;
- gameplay terus berjalan;
- pantulan menjadi bagian strategi.

---

## 28. Ball Friction

Setiap pergerakan dan pantulan mengurangi velocity.

```text
100%
↓
80%
↓
60%
↓
40%
↓
STOP
```

Bola harus berhenti secara natural.

Tidak boleh ada infinite bounce.

---

## 29. Player Collision

Bola dapat mengenai pemain.

```text
⚽ → ●
```

Bola memantul sesuai collision angle.

Pemain dapat sedikit bergeser akibat benturan.

Displacement dibuat kecil agar formasi tidak terlalu chaotic.

---

## 30. Player vs Player Collision

Pemain dapat bertabrakan.

```text
● → ●
```

Keduanya dapat bergeser sedikit.

Tidak ada:

- foul;
- kartu;
- tackle;
- pelanggaran.

---

## 31. End of Turn

Turn selesai ketika semua objek hampir berhenti.

```text
ballVelocity < minimumVelocity
AND
allPlayerVelocity < minimumVelocity
```

Setelah itu:

```text
Freeze physics
↓
Cari nearest player
↓
Tentukan turn
↓
Snap kicker ke bola
↓
Formation Phase
```

---

## 32. Goal

Goal terjadi ketika bola melewati garis gawang.

Setelah goal:

1. physics berhenti;
2. animasi goal dimainkan;
3. skor bertambah;
4. arena reset;
5. kickoff diberikan kepada tim yang kebobolan.

---

## 33. Kickoff

Opening Toss hanya digunakan untuk kickoff pertama.

Setelah gol:

**tim yang kebobolan otomatis mendapat kickoff.**

Tidak ada toss ulang.

Pada kickoff:

- bola berada di tengah;
- formasi reset;
- penendang kickoff otomatis ditentukan.

---

## 34. Formation Reset

Formasi tidak di-reset setiap turn.

Posisi setelah collision tetap digunakan.

Reset hanya saat:

- pertandingan dimulai;
- setelah goal.

---

## 35. Full Match Flow

```text
Start Match
↓
Opening Toss
↓
User Tap Toss
↓
Determine Kickoff Team
↓
Reset Ball to Center
↓
Kickoff
↓
Formation Phase
↓
Aim
↓
Set Power
↓
Shoot
↓
Physics
↓
Ball Stops
↓
Find Nearest Player
↓
Determine Turn
↓
Move Nearest Player to Ball
↓
Formation Phase
↓
Aim
↓
Shoot
↓
...
↓
GOAL
↓
Reset
↓
Kickoff for Team That Conceded
```

---

## 36. Controls

Semua gameplay dapat dilakukan menggunakan satu jari.

### Toss

```text
tap
```

### Reposition

```text
touch player
→ drag
→ release
```

### Aim dan power

```text
drag focused kicker left/right
guide appears and follows the aim
power meter sweeps low ↔ high during the drag
release to shoot at the indicated power
```

### Shoot

```text
release
```

---

## 37. Visual Style

Visual:

**paper craft / handmade tabletop game.**

Referensi visual:

- kertas buku tulis;
- kertas origami;
- meja kayu;
- lapangan digambar spidol;
- gawang dari karton/kertas;
- bola kertas diremas.

Target feel:

**permainan masa kecil yang hidup di atas meja.**

---

## 38. Scene Asset System

Scene tidak dibuat sebagai satu PNG besar.

Gunakan layer modular:

```text
Arena
├── table texture
├── field base
├── field lines
├── boundary wall
├── goal top
├── goal bottom
├── decorations
├── shadows
└── effects
```

Dengan sistem ini, theme arena dapat diganti tanpa mengganti seluruh scene.

Contoh future arena:

- Classroom Desk
- Wooden Table
- Cardboard Field
- Notebook Field
- Canteen Table
- School Tournament Table

---

## 39. Audio

Required SFX:

- paper movement;
- spring/flick;
- kick;
- ball collision;
- wall bounce;
- toss;
- ball landing;
- goal;
- whistle;
- UI tap.

Optional:

- suara anak bersorak;
- tepuk tangan;
- small crowd.

BGM:

- ringan;
- cheerful;
- tidak terlalu ramai.

---

## 40. Sportsmanship / Educational Element

Setelah pertandingan, karakter/game menyampaikan:

> “Terima kasih sudah bermain.”

Tidak ada:

- mengejek;
- rage animation;
- punishment karena kalah.

Nilai edukasi disampaikan secara tidak langsung melalui perilaku sportif.

---

## 41. Game Modes

### MVP

#### Single Player vs Bot

User bermain melawan AI sederhana.

### Future

#### Local 2 Player

Dua pemain menggunakan satu device.

#### Tournament

Beberapa pertandingan berurutan.

#### Challenge

Contoh:

- cetak gol melalui wall bounce;
- cetak gol dalam 3 shot;
- jangan kebobolan.

---

## 42. AI — MVP

AI mampu:

1. menentukan penendang;
2. reposition maksimal dua pemain saat bertahan;
3. menentukan aim;
4. menentukan power;
5. melakukan shoot.

### Easy

- aim kurang presisi;
- power lebih random;
- formation sederhana.

### Normal

- dapat menggunakan wall bounce;
- positioning defensif lebih baik;
- shot selection lebih baik.

---

## 43. Game Duration

Target:

**2–5 menit per match.**

---

## 44. MVP Scope

### Gameplay

- 1 arena
- 2 tim
- 4 pemain per tim
- 18-zone field (3 × 6)
- 1 bola
- Opening Toss
- kickoff
- formation phase
- nearest-player turn system
- auto kicker
- kicker snap to ball
- active/focused kicker state
- inactive player reposition pada formation phase
- drag horizontal pada kicker aktif untuk arah tendangan
- aim guide
- timing power meter yang bergerak bolak-balik selama gesture aim aktif
- shooting
- procedural paper-player spring animation
- physics-driven ball animation
- collision
- wall bounce
- goal
- score
- first to 3

### Customization Foundation

- runtime jersey color
- runtime jersey pattern
- runtime player number
- runtime player name

### Mode

- Single Player vs Bot

### UI

- Home
- Play
- Opening Toss
- Match
- Result

### Audio

- basic SFX
- 1 BGM

---

## 45. Out of Scope MVP

Tidak termasuk:

- online multiplayer;
- ranking;
- login;
- account;
- progression;
- loot box;
- tournament besar;
- complex AI;
- voice recognition;
- commentary;
- weather;
- foul;
- offside;
- corner;
- throw-in;
- full trajectory prediction.

---

## 46. Technical Direction

Recommended:

**Native Android — Kotlin + Jetpack Compose**

Compose untuk:

- menu;
- HUD;
- score;
- dialog;
- navigation.

Gameplay:

```text
Compose Canvas
+
custom game loop
+
simple 2D physics
```

Asset pemain:

```text
SVG / vector source
+
runtime color
+
runtime pattern
+
runtime text
```

Ball:

```text
transparent PNG
+
procedural rotation / height / squash
+
optional angle-based rotation variants
+
particle/sprite effects
```

---

## 47. Basic Game Entities

```text
Game
 ├── Arena
 ├── Ball
 ├── Team A
 │    ├── Player A1
 │    ├── Player A2
 │    └── Player A3
 │
 ├── Team B
 │    ├── Player B1
 │    ├── Player B2
 │    └── Player B3
 │
 ├── Goal A
 └── Goal B
```

---

## 48. Core Game State

```text
OpeningToss
Kickoff
Formation
Aiming
PowerSelection
BallMoving
ResolvingTurn
Goal
MatchFinished
```

Flow:

```text
OpeningToss
↓
Kickoff
↓
Formation
↓
Aiming
↓
PowerSelection
↓
BallMoving
↓
ResolvingTurn
```

---

## 49. Turn Resolution

Pseudo logic:

```text
whenPhysicsStopped():

    if goalDetected:
        handleGoal()
        return

    nearestRed = findNearestPlayer(RED, ball.position, stableIdTieBreak)
    nearestBlue = findNearestPlayer(BLUE, ball.position, stableIdTieBreak)
    if abs(distance(nearestRed) - distance(nearestBlue)) < 6 and lastKickingTeam exists:
        currentTeam = opposite(lastKickingTeam)
    else:
        currentTeam = teamWithShorterDistance(nearestRed, nearestBlue, stableIdTieBreak)

    kicker = nearestPlayerFrom(currentTeam)

    moveKickerToBall()

    startFormationPhase()
```

Selama Formation Phase 10 detik, maksimal dua player Inactive dari tim bertahan dapat di-reposition; perpindahan terakhir dapat di-undo atau fase diakhiri dengan Selesai. Kicker Active/Focused terkunci di belakang bola. Saat masuk Aiming 8 detik, drag horizontal pada kicker mengubah `kickerAimAngle` tanpa mengubah posisi fisiknya. Jika countdown berakhir dengan power terisi, tendangan otomatis dilepas memakai power terakhir; jika belum ada power, bola tetap di tempat dan giliran diserahkan kepada pemain bertahan terdekat setelah penendang lama mundur.

Opening Toss:

```text
startOpeningToss():

    kickoffTeam = random(TEAM_A, TEAM_B)

    playTossAnimation(kickoffTeam)

    resetBallToCenter()

    startKickoff(kickoffTeam)
```

---

## 50. Shooting State

```text
Formation
↓
Aiming
↓
PowerSelection
↓
Shoot
↓
BallMoving
```

During aiming:

```text
kickerAimAngle =
    clamp(
        horizontalDragAngle,
        minAimAngle,
        maxAimAngle
    )
```

Guide direction:

```text
shotDirection =
    directionFromAimAngle(kickerAimAngle, attackingDirection)
```

The focused kicker stays behind the ball. Show the guide after the user begins dragging the focused kicker. Horizontal drag changes `kickerAimAngle`; vertical movement is ignored. While this drag is active, the power indicator sweeps repeatedly between low and high. Release uses the current power value and starts the shot; neither gesture changes gameplay position. Player tilt is a small visual response and does not rotate through the full angle.

Shot power:

```text
shotPower = currentPowerMeterValue
```

The power meter oscillates between `0f` and `1f` while the focused kicker is being dragged. It freezes on release and that value is used for the shot.

---

## 51. Key Design Principle

**Easy to understand, hard to master.**

Pemain hanya perlu memahami:

> “Atur posisi pemain → geser untuk membidik → lepas saat power pas → gol.”

Pemain yang lebih lama bermain mulai memahami:

- positioning;
- defending;
- blocking;
- shot angle;
- power;
- wall bounce;
- setup untuk turn berikutnya.

---

## 52. MVP Success Criteria

MVP berhasil jika:

- pemain memahami Opening Toss;
- pemain memahami drag untuk mengatur formasi;
- pemain memahami bahwa drag horizontal pada kicker mengubah arah tendangan;
- guide line cukup jelas tanpa tutorial panjang;
- anak dapat melakukan tendangan sendiri;
- aiming tidak terasa terlalu sensitif;
- ball movement terlihat sinkron dengan physics;
- wall bounce mudah dipahami;
- match selesai kurang dari 5 menit;
- tidak ada bola stuck;
- tidak ada turn deadlock;
- formation phase tidak membuat match lambat;
- pemain ingin melakukan rematch.

---

## 53. Future Potential

Setelah core gameplay terbukti fun:

- berbagai model pemain kertas;
- lebih banyak jersey pattern;
- arena meja kelas;
- meja rumah;
- lapangan kardus;
- arena sekolah;
- tournament antar kampung;
- customization;
- sticker;
- local multiplayer;
- online multiplayer;
- challenge harian;
- first-bounce trajectory guide;
- cup/tournament mode;
- unlockable ball skins;
- unlockable paper themes.

Identitas game tetap:

**sepak bola lipatan kertas dengan physics sederhana, positioning ringan, aiming yang mudah dipahami, dan visual handmade yang bisa dimainkan semua orang.**
