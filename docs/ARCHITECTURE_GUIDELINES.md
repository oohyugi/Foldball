# Foldball Architecture Guidelines

Pedoman ini menjelaskan letak kode, arah dependency, dan pemilik state. Struktur berlaku untuk target Android, iOS, dan Desktop JVM yang sudah ada. Terapkan bertahap di satu module `shared`; pecah Gradle module hanya ketika batas fitur atau waktu build membutuhkannya.

## Sebelum Menulis Kode: Rancang untuk Perubahan

Sebelum implementasi fitur, tentukan bagian yang mungkin berubah, siapa pemiliknya, kapan nilainya dipilih, dan bagaimana UI membaca hasilnya. Pilih batas konfigurasi yang kecil dan bertipe; jangan menanam angka aturan di screen atau menduplikasi keputusan domain di komponen visual. Tetapkan nilai awal dan validasi input. Buat abstraksi hanya untuk variasi yang nyata atau sudah direncanakan, lalu catat jalur perubahan berikutnya.

Contoh countdown pertandingan: `MatchTimingConfig` memuat durasi bertahan dan menyerang dengan nilai awal serta batas validasi. Pembuat pertandingan memberikan config kepada `GameEngine` saat match dimulai. Engine mengelola perpindahan fase dan mengirim sisa serta total waktu melalui `GameSnapshot`; UI hanya menampilkannya. Kelak layar pengaturan cukup menghasilkan config sebelum match baru dibuat, tanpa mengubah rumus fase atau tampilan timer. Durasi match yang berjalan tetap konsisten dengan config saat match dibuat.

Panel status pertandingan berada di `feature/match/presentation/components`; ia menerima snapshot dan callback, sedangkan warna dan tekstur kertas memakai token `core/ui/theme`. Aturan possession, timeout, dan Undo tetap dimiliki engine.

## 1. Baseline Proyek

- Kotlin Multiplatform dengan target Android, iOS, dan Desktop JVM.
- Compose Multiplatform untuk UI yang dapat dibagi di `shared`.
- Kode yang tidak bergantung pada platform harus berada di `commonMain`.
- Integrasi native dan engine jaringan khusus platform berada di source set platform.
- Versi dependency dikelola dari `gradle/libs.versions.toml`; jangan menaruh versi dependency berulang di build file.

Paket baru dibuat ketika ada kode nyata yang menempatinya. Hindari folder kosong dan kelas pembungkus yang hanya meneruskan satu panggilan.

## 2. Batas Lapisan

```text
feature/<name>/presentation ──→ feature ViewModel ──→ domain rules / use case
          │                              ↑
          ├──→ gamekit/presentation ──→ domain/game
          └──→ core/ui
data repository ──implements──→ domain repository interface
app/di ──merangkai semua dependency di atas
```

- **Feature presentation:** screen, komponen khusus fitur, event input, dan tampilan `UiState`, disimpan di dalam `feature/<name>/presentation/`. UI tidak mengambil data langsung dari repository.
- **GameKit presentation:** renderer ball, field, dan player serta resep animasi visual yang digunakan oleh lebih dari satu fitur. GameKit tidak mengetahui screen atau navigasi fitur.
- **Core UI:** theme tokens, paper shapes, dan kontrol visual Foldball yang dapat digunakan lintas fitur. Komponen di sini tidak memuat aturan pertandingan.
- **Domain:** model, aturan pertandingan, dan physics yang murni Kotlin. Domain tidak mengenal Compose, Android, Coil, atau Koin.
- **Data:** implementasi repository, API, storage, mapper. Data bergantung pada interface dan model domain.
- **App/platform:** entry point, navigation, DI composition root, serta adapter yang hanya tersedia pada platform tertentu.

Dependency mengarah ke domain. Feature screen dapat memakai GameKit presentation dan Core UI; domain tidak mengimpor feature, data, atau Compose UI. `gamekit/presentation` boleh membaca model domain, tetapi tidak boleh bergantung pada feature.

## 3. MVI untuk Fitur

Setiap screen produksi yang punya alur bisnis memakai satu kontrak MVI: `UiState`, `Intent`, dan `Effect`. ViewModel menerima intent, meminta keputusan domain, lalu menerbitkan state baru atau effect sekali jalan.

```kotlin
data class MatchUiState(
    val phase: MatchPhase = MatchPhase.Loading,
    val score: Score = Score(),
    val selectedPlayerId: PlayerId? = null,
)

sealed interface MatchIntent {
    data object Toss : MatchIntent
    data class DragPlayer(val id: PlayerId, val position: Position) : MatchIntent
    data class Aim(val angle: Float) : MatchIntent
    data class SetPower(val value: Float) : MatchIntent
    data object Shoot : MatchIntent
}

sealed interface MatchEffect {
    data class AnnounceGoal(val team: TeamId) : MatchEffect
    data class ShowError(val message: UiMessage) : MatchEffect
}
```

Konvensi:

- `UiState` immutable dan tersedia sebagai `StateFlow`; screen mengoleksinya sesuai lifecycle.
- Intent masuk melalui satu fungsi `onIntent(intent)`; ViewModel tidak berisi rumus physics atau path gambar.
- Effect sekali jalan, seperti navigasi atau snackbar, memakai `Channel`/`Flow`. Screen mengoleksinya dari `LaunchedEffect`.
- Posisi, velocity, score, dan phase yang memengaruhi gameplay berasal dari snapshot `GameEngine`.
- Gesture aim/power dapat memakai state sementara di UI agar respons jari halus. Saat validasi/commit diperlukan, kirim command ke engine melalui ViewModel.
- Tilt, squash, stretch, dan recoil adalah state animasi lokal pada renderer player. Jangan kirim satu intent atau satu `UiState` baru untuk setiap frame animasi.
- `GameEngine` menjalankan loop physics dan mengeluarkan snapshot render seperlunya; reducer MVI menangani transisi dan aksi pengguna, bukan tiap substep physics.

Komponen kecil menerima data dan callback. `PlayerLab` adalah screen preview untuk pengembangan; state preset animasinya boleh lokal sampai ada alur bisnis yang membutuhkan ViewModel.

## 4. Domain dan Repository Pattern

Interface berada di domain; implementasinya berada di data.

```text
domain/repository/PlayerProfileRepository.kt
data/repository/PlayerProfileRepositoryImpl.kt
```

- Repository menyembunyikan sumber data persisten atau remote dari domain.
- Model domain tidak memakai DTO jaringan, Compose `ImageBitmap`, `Painter`, atau tipe platform.
- Gunakan use case untuk aksi bisnis yang mengorkestrasi engine/repository atau dipakai ulang. Aturan murni seperti jarak terdekat dan batas aim arc menjadi fungsi/kelas di game engine; jangan membuat satu kelas use case untuk setiap hitungan kecil.
- Untuk satu operasi baca sederhana, ViewModel boleh memanggil interface repository domain langsung.
- Hasil/error data dipetakan ke tipe domain yang dapat dipahami fitur; jangan kirim exception mentah ke UI.
- Posisi sementara pertandingan disimpan di `GameEngine`. Repository diperlukan ketika match atau customization perlu disimpan, disinkronkan, atau diambil dari luar proses. Model `Player` untuk pertandingan tidak perlu repository hanya karena ia sebuah entity.

## 5. Struktur File yang Disarankan

Struktur ini menunjukkan pemilik kode. Buat folder saat implementasi dipindahkan ke sana.

```text
shared/src/commonMain/kotlin/com/yogi/foldball/
├── Main.kt                 # Entrypoint aplikasi: Home → Match atau Sandbox
├── app/
│   ├── di/
│   │   ├── AppModule.kt    # Menggabungkan module yang dimiliki fitur/data
│   │   └── initKoin.kt
│   └── navigation/
├── core/
│   └── ui/
│       └── theme/           # Foldball tokens, theme, paper shapes, dan controls
├── gamekit/
│   └── presentation/
│       ├── ball/            # PaperBall dan ukuran visual bersama
│       ├── field/           # ClassroomStadium
│       └── player/          # PaperPlayer dan animasi visual
├── domain/
│   ├── game/
│   │   ├── model/           # Player, Ball, Team, MatchState
│   │   ├── engine/          # GameEngine, PlayerRules, physics
│   │   └── usecase/         # Aksi bisnis yang perlu orkestrasi
│   └── repository/          # Interface data persisten/remote
├── data/
│   ├── remote/
│   ├── local/
│   ├── mapper/
│   ├── repository/
│   └── DataModule.kt
└── feature/
    ├── home/
    │   └── presentation/
    │       └── screen/HomeScreen.kt
    ├── match/
    │   ├── MatchModule.kt
    │   └── presentation/
    │       ├── screen/MatchScreen.kt
    │       ├── components/         # MatchScoreboard, PowerTimingMeter, komposisi HUD
    │       └── viewmodel/          # Contract, UiState, ViewModel saat alur bisnis memakai MVI
    ├── balllab/
    │   └── presentation/
    │       └── screen/BallLabScreen.kt
    └── playerlab/           # Preview animasi saat development
        └── presentation/
            └── screen/PlayerLabScreen.kt

shared/src/commonMain/composeResources/
├── drawable/       # PNG/WebP/JPG dan Android XML VectorDrawable
├── font/
├── values/
└── files/          # File mentah yang dibaca sendiri

shared/src/commonTest/   # Aturan game dan MVI yang perlu diuji
shared/src/androidMain/   # Adapter Android jika memang diperlukan
shared/src/iosMain/       # Adapter iOS jika memang diperlukan
shared/src/jvmMain/       # Adapter Desktop jika memang diperlukan
```

`feature/match/presentation` dan `feature/playerlab/presentation` memakai renderer yang sama dari `gamekit/presentation/player`. Fitur lain tidak mengimpor implementasi UI dari fitur tetangganya. `core/ui` berisi aturan visual Foldball bersama; `gamekit/presentation` berisi renderer gameplay lintas fitur. Keduanya tetap package di `:shared`, bukan Gradle module terpisah.

## 6. UI Design System

Before any UI implementation or visual change, read [`Foldball Design System`](Foldball%20Design%20System.md). It is the authoritative guide for the product's visual language, design tokens, shared components, accessibility requirements, and UI implementation workflow.

- The design system is implemented inside the existing `:shared` module at `shared/src/commonMain/kotlin/com/yogi/foldball/core/ui/theme/`.
- Keep reusable Foldball tokens and components in `core/ui/theme/`. Keep cross-feature gameplay rendering in `gamekit/presentation/`; keep match-specific compositions in `feature/match/presentation/components/`.
- Use Compose Resources under `shared/src/commonMain/composeResources/` for bundled images, vectors, and fonts. Keep original source assets under `referensi_assets/`.
- Reuse shared tokens and components instead of defining screen-local colors, typography, paper surfaces, shadows, or press effects. Update the design-system guide when adding a new reusable visual rule.
- UI components receive presentation data, accessibility descriptions, and callbacks from their owners. They do not own match rules or navigation.
- Preserve arena coordinates, gesture regions, and physics when changing the visual layer.

## 7. Penempatan Kode Player

| Tanggung jawab | Lokasi | Contoh |
| --- | --- | --- |
| Identitas dan kondisi gameplay | `domain/game/model/` | `Player`, `PlayerId`, posisi, velocity, team, collider |
| Status giliran dan aturan gerak | `domain/game/model/` + `domain/game/engine/` | `PlayerTurnStatus` (inactive/focused), reposition hanya untuk inactive saat formation, aim angle untuk kicker aktif |
| Aturan game murni lainnya | `domain/game/engine/` | pilih player terdekat, batas reposition, shot direction, collision |
| Aksi bisnis lintas engine/data | `domain/game/usecase/` | `StartMatch`, `RestoreMatch`, `SaveResult` jika memang diperlukan |
| Menggambar bidak reusable | `gamekit/presentation/player/` | body, layer jersey, pattern, nomor, nama, shadow |
| Animasi kosmetik reusable | `gamekit/presentation/player/` | tilt, press, spring, recoil, settle, impact |
| State dan input layar match | `feature/match/presentation/` | `MatchIntent.DragPlayer`, `MatchUiState`, `MatchViewModel` |
| Screen untuk melihat preset animasi | `feature/playerlab/presentation/` | tombol preview idle, aim, kick, collision |
| Profil/customization yang disimpan atau diambil dari server | `domain/repository/` + `data/repository/` | `PlayerProfileRepository` dan implementasinya |

**Use case player:** aksi yang mengubah hasil pertandingan ditempatkan di domain. Kalkulasi kecil berada di `GameEngine`/`PlayerRules`; buat kelas use case tersendiri hanya jika perlu mengorkestrasi beberapa operasi. Animasi visual tidak membutuhkan `AnimatePlayerUseCase` karena tidak mengubah aturan pertandingan.

Renderer `PaperPlayer` dan state animasi kosmetik `PlayerMotionState` berada di `gamekit/presentation/player/`. Preset serta layar preview pengembangan berada di `feature/playerlab/presentation/`. `Main.kt` menyediakan entrypoint `Main()` yang membuka Home dan `SandboxMain()` yang langsung membuka tool sandbox. Root merangkai screen; pindahkan routing yang membesar ke navigation package saat alur bertambah.

## 8. Koin untuk Dependency Injection

- Definisi Koin diletakkan dekat pemiliknya: `MatchModule.kt` untuk ViewModel/use case match, `DataModule.kt` untuk implementasi repository, dan module platform untuk adapter native. `app/di/AppModule.kt` hanya menggabungkan module tersebut.
- `commonMain` merangkai use case, repository interface → implementation, ViewModel, dan service bersama. Domain tidak mengimpor Koin.
- Jalankan `initKoin()` sekali dari entry point Android, iOS, dan Desktop sebelum UI dibuat. Hindari memulai ulang global Koin pada recomposition.
- Gunakan `koinViewModel()` untuk ViewModel di screen Compose; komponen anak menerima state/callback, bukan meminta dependency sendiri.
- Gunakan `single` untuk service/repository bersama, `factory` untuk objek singkat, dan definisi ViewModel untuk lifecycle state layar. Render composable murni seperti `PaperPlayer` tidak perlu didaftarkan ke Koin.
- Pengujian ViewModel/domain dapat memasukkan fake dependency langsung; Koin diuji hanya untuk graph yang memang perlu diverifikasi.

Contoh arah graph:

```text
MatchScreen → MatchViewModel → GameEngine
                             ↓
                       SaveMatchUseCase → MatchRepository (interface)
                                          ↑
                              MatchRepositoryImpl
```

## 9. Coil untuk Gambar Remote

Gunakan Coil 3 dari UI Compose untuk gambar yang URL-nya berasal dari jaringan.

- `commonMain`: `coil-compose` dan `coil-network-ktor3` bila gambar remote dibutuhkan.
- Tambahkan engine Ktor pada source set pemakai: Android, iOS/Darwin, dan Desktop JVM. Versinya dikelola bersama di version catalog.
- `AsyncImage` berada di UI. Repository mengirim metadata atau URL gambar; repository tidak membuat `Painter` atau `ImageBitmap`.
- Berikan placeholder/error state dan ukuran target agar gambar tidak menyebabkan layout meloncat atau decode berlebihan.
- Konfigurasikan satu `ImageLoader` tingkat aplikasi; jangan buat loader baru untuk setiap item/composition.
- Gambar bundled seperti jersey, field, dan player bukan gambar remote: simpan sebagai resource lokal.
- Jika Coil memuat SVG, tambahkan artifact `coil-svg`; jangan menganggap Coil network aktif hanya karena `coil-compose` ada.

Contoh pemuatan aset remote dari Compose:

```kotlin
AsyncImage(
    model = player.avatarUrl,
    contentDescription = "Foto pemain ${player.name}",
)
```

## 10. Asset Lokal, SVG, dan Animasi Player

Compose Multiplatform Resources mendukung gambar raster dan XML vector di `commonMain`. Walaupun API mengenali ekstensi `.svg`, implementasi Android Compose Multiplatform `1.12.1` melempar `IllegalStateException` saat SVG dimuat lewat `painterResource`. Jangan gunakan SVG langsung sebagai drawable runtime lintas platform yang mencakup Android.

Aturan aset lokal:

1. Simpan sumber desain dan referensi di `referensi_assets/`; jangan jadikan seluruh folder referensi sebagai resource runtime.
2. Simpan hanya aset runtime di `shared/src/commonMain/composeResources/drawable/`. Gunakan nama resource sederhana dengan huruf kecil dan underscore, lalu akses melalui `Res.drawable.<nama>` dan `painterResource`.
3. Untuk ikon dan ilustrasi berbasis path, konversi SVG sumber menjadi XML vector yang didukung Compose Resources; pertahankan SVG asli di folder referensi. Periksa hasil konversi karena filter, mask, gradient kompleks, font eksternal, atau fitur browser-only mungkin tidak terbawa.
4. Gunakan PNG/WebP untuk tekstur raster, lukisan kompleks, dan efek bertekstur. Jangan masukkan SVG langsung ke drawable bersama Android kecuali renderer dan target pemakaiannya memang sudah dipastikan mendukungnya.
5. Coil `coil-svg` hanya diperlukan bila SVG memang dimuat melalui pipeline Coil, misalnya gambar remote. Untuk aset lokal runtime lintas target, gunakan resource XML vector atau raster Compose Resources.
6. Untuk shadow, pattern, teks runtime, dan clipping jersey, gambar sebagai layer Compose/Canvas terpisah bila perlu recolor atau transform independen.
7. Animasi spring player menggunakan transform procedural pada body; shadow tetap berada di ground layer. Collider dan posisi physics tidak mengikuti squash/tilt kosmetik.

Untuk animasi player yang perlu mengubah bagian tubuh secara independen, gunakan path/layer modular atau renderer Canvas; satu SVG statis tidak otomatis menyediakan pose yang dapat dianimasikan per bagian. Shadow dipisah dari body agar tidak ikut squash/tilt. `Paper Soccer Player Asset Sheet.png` adalah referensi pose, bukan sprite final.

## 11. Aturan Dependency dan Pertumbuhan Proyek

- Tambahkan dependency hanya di source set yang memakainya.
- Simpan versi dan alias di `gradle/libs.versions.toml`; gunakan BOM jika library menyediakannya dan proyek memilih BOM.
- Jaga compatibility Kotlin, Compose Multiplatform, AGP, Coil, Ktor, dan Koin. Periksa matriks kompatibilitas resmi sebelum menaikkan versi.
- Jangan menambah wrapper/use case/abstraction hanya untuk meneruskan satu panggilan tanpa perilaku atau manfaat batas yang jelas.
- Pecah satu `shared` menjadi Gradle module terpisah ketika ada batas dependency yang nyata, kebutuhan reuse di produk lain, atau waktu build yang terbukti menjadi hambatan. Package boundary cukup untuk tahap sekarang.
- Satu perubahan fitur mengikuti urutan: model/aturan domain → data jika perlu → ViewModel/MVI → screen → DI registration. Renderer player reusable dipakai oleh screen, tanpa screen lain menjadi dependency.

## 12. Rujukan Resmi

- [Compose Multiplatform resources: format image dan dukungan SVG](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html#images)
- [Koin setup](https://insert-koin.io/docs/setup/)
- [Koin Compose](https://insert-koin.io/docs/reference/koin-compose/compose/)
- [Koin KMP integration](https://insert-koin.io/docs/reference/koin-mp/kmp/)
- [Coil getting started](https://coil-kt.github.io/coil/getting_started/)
- [Coil network image engines](https://coil-kt.github.io/coil/network/)
- [Coil SVG decoder](https://coil-kt.github.io/coil/svgs/)
