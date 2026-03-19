# Testing Roadmap — TechGadget E-Commerce

> **Legend:**
> - ✅ Sudah ada & cukup
> - ⚠️ Sudah ada tapi incomplete / ada bug
> - ⬜ Belum ada, perlu dibuat

---

## Phase 1 — Perbaiki & lengkapi yang sudah ada

Target coverage: **~80%** pada file yang sudah ada.

### AuthTest.java

| Status | Test | Keterangan |
|--------|------|------------|
| ✅ | `register_success` | Registrasi berhasil, token dikembalikan, refreshToken disimpan |
| ✅ | `register_duplicateEmail_throwConflict` | DataIntegrityViolation diconvert ke ConflictException |
| ✅ | `login_success` | Login berhasil, token valid, refresh token disimpan |
| ✅ | `login_emailNotFound_throwNotFound` | Email tidak ditemukan di DB |
| ✅ | `login_passwordNotMatch_throwUnauthorized` | Password salah |
| ⚠️ | **BUG — `test_login_success` baris 80** | `request.setPassword("email@gmail.com")` dipanggil 2x, harusnya `setEmail()`. Test pass by accident. |
| ⬜ | `refresh_success` | Token refresh valid → access token baru + refresh token baru |
| ⬜ | `refresh_invalidToken_throwUnauthorized` | Refresh token tidak ada di DB → UnauthorizedException |

### ProductTest.java

| Status | Test | Keterangan |
|--------|------|------------|
| ✅ | `getProductById_success` | Product ditemukan, response tidak null |
| ✅ | `getProductById_throwNotFound` | Product tidak ada → NotFoundException |
| ⚠️ | `advancedSearch_withoutCategory` | Method body kosong, tidak ada assertion |
| ⬜ | `createProduct_success` | Admin membuat product baru, category valid |
| ⬜ | `createProduct_categoryNotFound_throwNotFound` | Category tidak ada → NotFoundException |

### AdminOrderControllerTest.java

| Status | Test | Keterangan |
|--------|------|------------|
| ⚠️ | 4 test methods | Semua body kosong. Nama class juga keliru — ini test `OrderService` bukan Controller. **Refactor ke `OrderServiceTest.java`** dan isi di Phase 2. |

---

## Phase 2 — Core business logic

Buat **2 file baru**. Ini domain paling penting untuk di-test.

### OrderServiceTest.java *(buat baru)*

| Status | Test | Keterangan |
|--------|------|------------|
| ⬜ | `createOrder_success` | Order berhasil dibuat, snapshot harga tersimpan, stock berkurang |
| ⬜ | `createOrder_emptyCartItemIds_throwBadRequest` | cartItemIds kosong/null → BadRequestException |
| ⬜ | `createOrder_insufficientStock_throwConflict` | Stock < quantity → ConflictException |
| ⬜ | `createOrder_snapshotPrice_notCurrentPrice` | `OrderItem.priceAtOrder` harus sama dengan harga product saat order dibuat |
| ⬜ | `createOrder_addressNotBelongToUser_throwNotFound` | Address milik user lain → NotFoundException |
| ⬜ | `createOrder_invalidPaymentMethod_throwBadRequest` | Payment method tidak valid → BadRequestException |
| ⬜ | `cancelOrder_success_stockRestored` | Cancel order PENDING berhasil, stock dikembalikan |
| ⬜ | `cancelOrder_notPending_throwConflict` | Status bukan PENDING → ConflictException |
| ⬜ | `cancelOrder_notOwner_throwNotFound` | User lain tidak bisa cancel order yang bukan miliknya |

### CartServiceTest.java *(buat baru)*

| Status | Test | Keterangan |
|--------|------|------------|
| ⬜ | `addToCart_newItem_success` | Produk baru → CartItem baru terbuat |
| ⬜ | `addToCart_existingItem_quantityIncremented` | Produk sudah ada di cart → quantity bertambah, bukan buat CartItem baru |
| ⬜ | `addToCart_insufficientStock_throwConflict` | Stock tidak cukup → ConflictException |
| ⬜ | `addToCart_productNotFound_throwNotFound` | Product tidak ada → NotFoundException |
| ⬜ | `removeCartItem_success` | CartItem berhasil dihapus dari cart |
| ⬜ | `removeCartItem_notOwner_throwNotFound` | CartItem bukan milik user → NotFoundException |
| ⬜ | `clearCart_success` | Semua item cart berhasil dihapus |

---

## Phase 3 — Security layer

Buat **2 file baru**. Butuh mock Redis dan JWT — paling menarik untuk dipelajari.

### RateLimitServiceTest.java *(buat baru)*

| Status | Test | Keterangan |
|--------|------|------------|
| ⬜ | `isAllowed_underLimit_returnsTrue` | Request ke-1 s/d ke-(limit-1) harus lolos |
| ⬜ | `isAllowed_atLimit_returnsFalse` | Tepat di limit → request ditolak (boundary condition) |
| ⬜ | `isAllowed_luaScript_calledWithCorrectArgs` | Verifikasi Lua script dipanggil dengan KEYS dan ARGV yang benar |
| ⬜ | `getRetryAfterSeconds_returnsPositiveValue` | Nilai retry after harus positif saat limit terlampaui |
| ⬜ | `isAllowed_redisReturnsNull_returnsFalse` | Defensive: jika Redis return null → request ditolak (fail-safe) |

### JwtTokenProviderTest.java *(buat baru)*

| Status | Test | Keterangan |
|--------|------|------------|
| ⬜ | `generateAccessToken_containsCorrectClaims` | Token mengandung userId sebagai subject dan email sebagai claim |
| ⬜ | `validateToken_validToken_returnsClaims` | Token valid → claims berhasil diparse |
| ⬜ | `validateToken_expiredToken_throwsExpiredJwtException` | Token expired → ExpiredJwtException |
| ⬜ | `validateToken_tamperedToken_throwsException` | Token dimodifikasi setelah signing → gagal validasi |

---

## Summary

| Phase | File | Tests | Priority |
|-------|------|-------|----------|
| Phase 1 | `AuthTest.java`, `ProductTest.java` | ~10 | Sekarang — fix bug dulu |
| Phase 2 | `OrderServiceTest.java`, `CartServiceTest.java` | ~16 | Core domain — paling penting |
| Phase 3 | `RateLimitServiceTest.java`, `JwtTokenProviderTest.java` | ~9 | Security layer |
| **Total** | **6 file** | **~35 tests** | |

> Setelah Phase 1–3 selesai → lanjut ke **Integration Testing** dengan Testcontainers.
