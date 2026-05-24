# 🃏 Rummikub Online Multiplayer

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-F2F4F9?style=for-the-badge&logo=spring-boot)
![LibGDX](https://img.shields.io/badge/libGDX-E34F26?style=for-the-badge&logo=libgdx&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)

Sebuah implementasi board game **Rummikub** secara online *multiplayer*, dibangun menggunakan Java dengan arsitektur pemisahan *Client-Server*.

**[Itch.io Link](https://ement.itch.io/rummikub-online)**

---

## Fitur Utama

- **Real-Time Multiplayer:** Bermain melawan pemain lain secara *online* melalui sistem *Room/Lobby* yang tersinkronisasi.
- **Anti-Cheat Validation:** Sistem validasi integritas berlapis di *backend* (menggunakan transaksi atomik) untuk mencegah duplikasi dan manipulasi kartu.
- **Rummikub Ruleset:** Algoritma yang mendeteksi susunan sah *Group*, *Run*, penanganan ubin Joker, dan aturan wajib *Initial Meld* 30 Poin.
- **Undo / Redo System:** Memungkinkan pemain untuk mengembalikan semua posisi ubin lokal mereka sebelum menekan tombol *End Turn*.
- **Drag-and-Drop UI:** *Interface* responsif yang dibangun menggunakan LibGDX.


## Panduan Menjalankan Secara Lokal

### Prasyarat Instalasi:
- Java JDK 17 atau lebih baru
- Sistem pembangunan berbasis Gradle
- PostgreSQL Server

### Konfigurasi Server (Backend):
1. Buka *root folder* `Backend/` menggunakan IDE Anda.
2. Buat database PostgreSQL dengan nama `rummikub`.
3. (*Opsional*) Lakukan import skema/data awal dari file `rummikub_dump.sql` di *root backend*.
4. Konfigurasikan kredensial basis data Anda (username/password) di dalam file konfigurasi Spring Boot `application.properties`.
5. Jalankan aplikasi REST API via Gradle (server akan menyala di port `8080`):
   ```bash
   ./gradlew bootRun
   ```

### Konfigurasi Game Client (Frontend):
1. Buka *root folder* `Game/` (Proyek LibGDX).
2. Secara *default*, aplikasi terkonfigurasi untuk berkomunikasi ke API lokal (*localhost*).
3. **Versi Web/HTML:**
   ```bash
   ./gradlew html:superDev
   ```
4. **Versi Desktop Murni (LWJGL):**
   ```bash
   ./gradlew desktop:run
   ```

---

## Dokumentasi
Login:
![Login](https://hackmd.io/_uploads/HkAfJolgGe.png)
Lobby:
![Lobby](https://hackmd.io/_uploads/B11Q1oggfl.png)
Waiting Room:
![Waiting Room](https://hackmd.io/_uploads/SkeQJjgxzl.png)
Game Screen:
![Game Screen](https://hackmd.io/_uploads/Hy8fkoegGl.png)
End Screen:
![End Screen](https://hackmd.io/_uploads/HkuGJoglfg.png)

## Kredit

Fonts:
https://www.dafont.com/upheaval.font
https://www.dafont.com/determination.font

Sound Effect:
https://pixabay.com/sound-effects/musical-level-win-6416/
https://pixabay.com/sound-effects/film-special-effects-080047-lose-funny-retro-video-game-80925/
https://pixabay.com/sound-effects/film-special-effects-drop-itemstaplerclick-sound-effect-322959/
https://pixabay.com/sound-effects/immersivecontrol-button-click-sound-463065/
