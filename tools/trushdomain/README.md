# TrustPositif Checker

Aplikasi web PHP + Bootstrap 3 untuk memeriksa status domain/URL terhadap basis data TrustPositif Komdigi.

Sumber data: https://trustpositif.komdigi.go.id/

## Fitur web

- Cek domain/URL, mendukung batch hingga 50 domain per pencarian (dikirim per 5 ke sumber)
- Toggle mode gelap dan terang
- Halaman statistik kategori
- Riwayat pencarian tersimpan di SQLite
- Ekspor riwayat ke CSV
- Bersihkan riwayat
- Tanpa emoji pada elemen UI

## File utama

| File | Isi |
|---|---|
| `index.php` | Halaman utama web |
| `check.php` | Backend PHP, cek domain ke endpoint publik TrustPositif |
| `stats.php` | Halaman statistik kategori |
| `history.php` | Halaman riwayat pencarian |
| `export_history.php` | Ekspor riwayat CSV |
| `refresh_stats.php` | Perbarui statistik dari sumber |
| `clear_history.php` | Bersihkan riwayat |
| `db.php` | Koneksi SQLite dan fungsi database |
| `config.php` | Konfigurasi aplikasi |
| `assets/css/style.css` | Tema Bootstrap 3 terang dan gelap |
| `assets/js/app.js` | Interaksi form, AJAX, toggle tema |
| `data/trustpositif.sqlite` | Database riwayat |
| `blacklist_trustpositif.txt.gz` | Daftar blacklist resmi |
| `scrape_trustpositif.py` | Scraper Python |

## Menjalankan server

```bash
cd ~/mcp/tools/trushdomain
php -S 127.0.0.1:8008
```

Buka di browser:

```text
http://127.0.0.1:8008/index.php
```

## Endpoint TrustPositif yang digunakan

- Pencarian: `/welcome?domains=...`
- Statistik: `/Rest_server/gettotalrecordsperkategori`
- File blacklist: `/assets/db/domains`
