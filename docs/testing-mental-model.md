# Testing Mental Model
### Unit Test vs Integration Test — cara berpikir, bukan sekadar cara menulis

---

## Satu hal yang harus dipahami sebelum segalanya

Unit test dan integration test **bukan versi besar-kecil dari hal yang sama**.
Mereka menjawab pertanyaan yang berbeda.

```
Unit Test       → "Apakah logika ini benar?"
Integration Test → "Apakah sistem ini bekerja?"
```

Ketika kamu menulis unit test untuk `register()`, kamu membuktikan bahwa:
password di-hash, token di-generate, refresh token disimpan.

Ketika kamu menulis integration test untuk `register()`, kamu membuktikan bahwa:
Flyway migration benar (tabel ada), BCrypt real bisa verify hash-nya,
cookie di-set dengan benar di HTTP response, DB constraint benar-benar di-enforce.

Keduanya menguji `register()` — tapi pertanyaannya berbeda sama sekali.

---

## Tabel perbedaan yang perlu diingat

| Dimensi | Unit Test | Integration Test |
|---|---|---|
| Pertanyaan | Apakah logika ini benar? | Apakah sistem ini bekerja? |
| Scope | Satu class, semua dependency di-mock | Beberapa layer nyata |
| Musuh | Logic bug | Integration bug, config bug |
| Kecepatan | Milidetik | Detik |
| Maintenance cost | Rendah | Tinggi |
| Isolasi | Total | Minimal |

---

## Framework berpikir: pertanyaan yang harus ditanyakan

Setiap kali selesai menulis fitur, tanya **tiga pertanyaan ini secara berurutan**:

### Pertanyaan 1 — Ada branching logic yang kompleks?

Banyak `if/else`, banyak validasi, banyak kemungkinan error path?

**Ya →** Unit test harus ada. Tulis dulu sebelum apapun.
Kamu perlu cover semua edge case dengan cepat.

**Tidak →** Pertimbangkan skip unit test, atau tulis minimal.
Logic trivial tidak butuh banyak unit test.

---

### Pertanyaan 2 — Ada hal yang hanya bisa diverifikasi di sistem nyata?

Hal-hal seperti:
- DB constraint (unique email, FK violation)
- Cookie behavior di HTTP response
- Filter chain (JWT, rate limit)
- Format JSON response
- Flyway migration schema
- Security config (endpoint mana yang public, mana yang protected)

**Ya →** Integration test wajib ada, fokus pada hal itu saja.

**Tidak →** Unit test sudah cukup.

---

### Pertanyaan 3 — Seberapa mahal kalau fitur ini salah di production?

**Sangat mahal** (auth salah → user tidak bisa login, order salah → stock corrupt)
→ Pastikan keduanya ada, unit test dan integration test.

**Recoverable** (GET product by ID error → user lihat pesan error)
→ Unit test saja sudah cukup.

---

## Dua kategori fitur, dua pendekatan

### Kategori 1 — Kompleksitas ada di business logic

Contoh: `createOrder()`, `cancelOrder()`, `createReview()`.

Ada banyak validasi, banyak branching, banyak kemungkinan gagal.
Bug paling berbahaya ada di **logika**, bukan di koneksi antar layer.

**Pendekatan yang benar:**
1. Tulis unit test dulu — cover semua edge case
2. Setelah unit test solid, tulis **satu** integration test untuk happy path
3. Tujuan integration test di sini bukan mengulang edge case —
   tapi memverifikasi bahwa semua layer terhubung dengan benar

---

### Kategori 2 — Kompleksitas ada di integrasi antar komponen

Contoh: `SecurityConfig`, `RateLimitFilter`, `JwtAuthenticationFilter`.

Logic di masing-masing class sederhana.
Yang berbahaya adalah **apakah mereka terhubung dengan benar**.

**Pendekatan yang benar:**
1. Langsung tulis integration test — unit test hampir tidak berguna di sini
2. Karena bug yang paling mungkin adalah misconfiguration, bukan logic error
3. `@WebMvcTest` pun tidak cukup — harus full Spring context dengan container nyata

---

## Contoh konkrit: `register()` di AuthService

### Apa yang unit test buktikan

```java
@Test
void register_success() {
    when(passwordEncoder.encode("password")).thenReturn("hashed");
    when(userRepository.save(any())).thenReturn(savedUser);
    when(jwtTokenProvider.generateAccessToken(any(), any())).thenReturn("token");

    AuthServiceResponse response = authService.register(request);

    assertThat(response.getAccess()).isEqualTo("token");
    verify(refreshTokenRepository).save(any());
}
```

✅ Password di-encode sebelum disimpan  
✅ Access token di-generate dan dikembalikan  
✅ Refresh token disimpan ke repository  

❌ Apakah tabel `users` benar-benar ada di DB (Flyway migration)  
❌ Apakah unique constraint email di-enforce di DB level  
❌ Apakah BCrypt real bisa verify hash yang dihasilkan  
❌ Apakah HttpOnly cookie di-set benar di HTTP response  
❌ Apakah request melewati RateLimitFilter dengan benar  

---

### Apa yang integration test buktikan (hal yang berbeda)

```java
@Test
void register_success() {
    mockMvc.perform(post("/auth/register").content(...))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access").isNotEmpty())
        .andExpect(cookie().httpOnly("refresh", true)); // unit test tidak bisa cek ini

    // Verifikasi side effect di DB nyata
    assertThat(userRepository.findByEmail("test@gmail.com")).isPresent();
    assertThat(refreshTokenRepository.findAll()).hasSize(1);
}
```

Ini bukan pengulangan. Ini verifikasi hal yang berbeda.

---

## Trade-off yang sering diabaikan

**Integration test itu mahal untuk dimaintain.**

Ketika kamu rename field di `RegisterRequest`, unit test memberitahumu dalam satu detik.
Integration test — kalau kamu punya 30 test yang parsing JSON response —
kamu harus update 30 tempat sekaligus.

Ini kenapa integration test harus **sedikit tapi strategis**, bukan banyak tapi redundant.
Setiap integration test yang kamu tulis adalah maintenance cost yang kamu ambil selamanya.

---

## Decision tree praktis

```
Selesai nulis fitur
        │
        ▼
Ada branching logic kompleks?
   Ya  ──► Tulis unit test dulu, cover semua edge case
   Tidak ──► Skip atau minimal saja
        │
        ▼
Ada hal yang hanya bisa diverifikasi di sistem nyata?
   Ya  ──► Tulis integration test, fokus pada hal itu saja
   Tidak ──► Unit test sudah cukup
        │
        ▼
Seberapa mahal kalau salah di production?
   Sangat mahal  ──► Pastikan keduanya ada
   Recoverable   ──► Unit test saja sudah cukup
```

---

## Penerapan di project TechGadget

| Fitur | Unit Test | Integration Test | Alasan |
|---|---|---|---|
| `AuthService` (register, login, refresh) | Wajib | Wajib | Logic kompleks + cookie/token end-to-end |
| `OrderService` (createOrder, cancelOrder) | Wajib | Happy path saja | 12 langkah validasi + stock side effect |
| `CartService` | Wajib | Opsional | Logic solid, integrasi sederhana |
| `SecurityConfig` | Skip | Wajib | Tidak ada logic — semua ada di config |
| `RateLimitFilter` | Skip | Wajib | Harus ditest dengan Redis nyata |
| `UserProfileService` | Minimal | Skip | Logic trivial, integrasi sederhana |
| `PaymentService` (dummy) | Minimal | Skip | Logic trivial, belum production-ready |

---

## Satu kalimat untuk diingat

> Unit test membuktikan bahwa kode kamu **benar**.
> Integration test membuktikan bahwa sistem kamu **bekerja**.
> Keduanya perlu ada — tapi untuk alasan yang berbeda,
> di tempat yang berbeda, dengan prioritas yang berbeda.
