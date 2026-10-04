# Foldball Architecture Guidelines

Pedoman ini menjelaskan letak kode, arah dependency, dan pemilik state. Struktur berlaku untuk target Android, iOS, dan Desktop JVM yang sudah ada. Terapkan bertahap di satu module `shared`; pecah Gradle module hanya ketika batas fitur atau waktu build membutuhkannya.

## 1. Baseline Proyek

- Kotlin Multiplatform dengan target Android, iOS, dan Desktop JVM.
- Compose Multiplatform untuk UI yang dapat dibagi di `shared`.
- Kode yang tidak bergantung pada platform harus berada di `commonMain`.
- Integrasi native dan engine jaringan khusus platform berada di source set platform.
- Versi dependency dikelola dari `gradle/libs.versions.toml`; jangan menaruh versi dependency berulang di build file.

Paket baru dibuat ketika ada kode nyata yang menempatinya. Hindari folder kosong dan kelas pembungkus yang hanya meneruskan satu panggilan.

## 2. Batas Lapisan

```text
feature UI ──→ feature ViewModel ──→ domain rules / use case
     │                                    ↑
     └──→ presentation/player             │
data repository ──implements──→ domain repository interface
app/di ──merangkai semua dependency di atas
```

- **Feature UI:** screen, event input, dan tampilan `UiState`. UI tidak mengambil data langsung dari repository.
- **Presentation bersama:** renderer player dan resep animasi visual yang dipakai match maupun player lab.
- **Domain:** model, aturan pertandingan, dan physics yang murni Kotlin. Domain tidak mengenal Compose, Android, Coil, atau Koin.
- **Data:** implementasi repository, API, storage, mapper. Data bergantung pada interface dan model domain.
- **App/platform:** entry point, navigation, DI composition root, serta adapter yang hanya tersedia pada platform tertentu.

Dependency mengarah ke domain. Screen dapat memakai presentation bersama; domain tidak mengimpor feature, data, atau UI.

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
├── App.kt
├── app/
│   ├── di/
│   │   ├── AppModule.kt    # Menggabungkan module yang dimiliki fitur/data
│   │   └── initKoin.kt
│   └── navigation/
├── core/
│   └── ui/                  # Komponen umum tanpa aturan Foldball
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
├── presentation/
│   └── player/
│       ├── PaperPlayer.kt   # Body, jersey, nomor, shadow
│       ├── PlayerMotion.kt  # Resep tilt/squash/recoil
│       └── PlayerVisual.kt  # Input tampilan yang immutable
└── feature/
    ├── match/
    │   ├── MatchContract.kt
    │   ├── MatchViewModel.kt
    │   ├── MatchScreen.kt
    │   ├── MatchModule.kt
    │   └── components/
    └── playerlab/           # Preview animasi saat development
        └── PlayerLabScreen.kt

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

`feature/match` dan `feature/playerlab` memakai renderer yang sama dari `presentation/player`. Fitur lain tidak mengimpor implementasi UI dari fitur tetangganya. `core` hanya untuk kode benar-benar umum, bukan tempat menaruh seluruh kode yang belum jelas pemiliknya.

## 6. Penempatan Kode Player

| Tanggung jawab | Lokasi | Contoh |
| --- | --- | --- |
| Identitas dan kondisi gameplay | `domain/game/model/` | `Player`, `PlayerId`, posisi, velocity, team, collider |
| Status giliran dan aturan gerak | `domain/game/model/` + `domain/game/engine/` | `PlayerTurnStatus` (inactive/focused), reposition hanya untuk inactive saat formation, aim angle untuk kicker aktif |
| Aturan game murni lainnya | `domain/game/engine/` | pilih player terdekat, batas reposition, shot direction, collision |
| Aksi bisnis lintas engine/data | `domain/game/usecase/` | `StartMatch`, `RestoreMatch`, `SaveResult` jika memang diperlukan |
| Menggambar bidak reusable | `presentation/player/` | body, layer jersey, pattern, nomor, nama, shadow |
| Animasi kosmetik reusable | `presentation/player/` | tilt, press, spring, recoil, settle, impact |
| State dan input layar match | `feature/match/` | `MatchIntent.DragPlayer`, `MatchUiState`, `MatchViewModel` |
| Screen untuk melihat preset animasi | `feature/playerlab/` | tombol preview idle, aim, kick, collision |
| Profil/customization yang disimpan atau diambil dari server | `domain/repository/` + `data/repository/` | `PlayerProfileRepository` dan implementasinya |

**Use case player:** aksi yang mengubah hasil pertandingan ditempatkan di domain. Kalkulasi kecil berada di `GameEngine`/`PlayerRules`; buat kelas use case tersendiri hanya jika perlu mengorkestrasi beberapa operasi. Animasi visual tidak membutuhkan `AnimatePlayerUseCase` karena tidak mengubah aturan pertandingan.

Renderer `PaperPlayer` dan state animasi kosmetik `PlayerMotionState` berada di `presentation/player/`. Preset serta layar preview pengembangan berada di `feature/playerlab/`. `App.kt` hanya menjadi entry point yang merangkai theme dan screen yang aktif; tambahkan DI dan navigation di sana saat fitur tersebut mulai digunakan.

## 7. Koin untuk Dependency Injection

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

## 8. Coil untuk Gambar Remote

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

## 9. Asset Lokal, SVG, dan Animasi Player

Compose Multiplatform Resources mengenali gambar raster dan Android XML VectorDrawable. SVG drawable **tidak didukung pada Android** oleh `painterResource`; untuk akses drawable yang sama di semua target, gunakan PNG/WebP atau XML vector dengan fitur yang didukung.

Pilihan untuk pack player:

1. Simpan file SVG asli sebagai sumber desain di `paper_soccer_player_assets/`.
2. Masukkan hanya aset yang benar-benar dibaca runtime ke `shared/src/commonMain/composeResources/`. Jika dipakai sebagai drawable lintas target, konversi path SVG yang kompatibel menjadi XML VectorDrawable, misalnya `drawable/player_body.xml`. Periksa hasilnya karena SVG filter seperti blur tidak otomatis menjadi VectorDrawable.
3. Untuk shadow, pattern, teks runtime, dan clipping jersey, gambar sebagai layer Compose/Canvas terpisah jika memang perlu recolor atau transform independen.
4. Bila ingin menampilkan SVG mentah tanpa konversi, `painterResource` bukan jalur Android. Simpan file di `composeResources/files/`, lalu muat dengan Coil 3 + `coil-svg` (misalnya melalui `Res.getUri("files/player.svg")`). Ini cocok untuk gambar utuh yang tidak perlu recolor per layer.
5. Animasi spring player menggunakan transform procedural pada body; shadow tetap berada di ground layer. Collider dan posisi physics tidak mengikuti squash/tilt kosmetik.

Pack SVG sekarang tetap menjadi sumber desain modular. Renderer Canvas yang saat ini mengikuti path aset dapat diteruskan untuk animasi procedural; konversi XML diperlukan hanya jika renderer memilih memakai `painterResource` untuk layer vector. Shadow dipisah dari body agar tidak ikut squash/tilt. `Paper Soccer Player Asset Sheet.png` adalah referensi pose, bukan sprite final.

## 10. Aturan Dependency dan Pertumbuhan Proyek

- Tambahkan dependency hanya di source set yang memakainya.
- Simpan versi dan alias di `gradle/libs.versions.toml`; gunakan BOM jika library menyediakannya dan proyek memilih BOM.
- Jaga compatibility Kotlin, Compose Multiplatform, AGP, Coil, Ktor, dan Koin. Periksa matriks kompatibilitas resmi sebelum menaikkan versi.
- Jangan menambah wrapper/use case/abstraction hanya untuk meneruskan satu panggilan tanpa perilaku atau manfaat batas yang jelas.
- Pecah satu `shared` menjadi Gradle module terpisah ketika ada batas dependency yang nyata, kebutuhan reuse di produk lain, atau waktu build yang terbukti menjadi hambatan. Package boundary cukup untuk tahap sekarang.
- Satu perubahan fitur mengikuti urutan: model/aturan domain → data jika perlu → ViewModel/MVI → screen → DI registration. Renderer player reusable dipakai oleh screen, tanpa screen lain menjadi dependency.

## 11. Rujukan Resmi

- [Compose Multiplatform resources: format image dan dukungan SVG](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html#images)
- [Koin setup](https://insert-koin.io/docs/setup/)
- [Koin Compose](https://insert-koin.io/docs/reference/koin-compose/compose/)
- [Koin KMP integration](https://insert-koin.io/docs/reference/koin-mp/kmp/)
- [Coil getting started](https://coil-kt.github.io/coil/getting_started/)
- [Coil network image engines](https://coil-kt.github.io/coil/network/)
- [Coil SVG decoder](https://coil-kt.github.io/coil/svgs/)
