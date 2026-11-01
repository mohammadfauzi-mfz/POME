# POME Monitor

Aplikasi Android pemantauan pengaliran POME dari PKS ke blok kebun.

## Langkah
1. **Backend**: buat Google Sheet baru > Extensions > Apps Script > tempel `apps-script/Code.gs` > jalankan fungsi `setup` sekali > Deploy > New deployment > Web app (Execute as: Me, Who has access: Anyone) > salin URL.
2. **Isi URL** di `app/src/main/assets/config.js` (field `URL`), supaya semua user langsung pakai tanpa input.
3. **Upload** seluruh isi folder ini ke repo GitHub baru (branch `main`). Workflow "Build APK" jalan otomatis; APK ada di Releases (tag `latest`) dan Actions > Artifacts.
4. Login awal: `admin` / `admin123`. Segera ganti lewat Atur > User > Reset PW. Admin menambah user PKS dan Kebun di sana.
5. Peta: Atur > Peta (GeoJSON) > pilih file di HP. Pilih kolom yang berisi nama blok.

## Aturan
- PKS: input HM awal/akhir dan volume, tambah pompa.
- Kebun: alokasi ke blok, tambah blok.
- Admin: semuanya, plus kelola user.
- Target 250 m³/ha per rotasi, 750 per tahun. Di atas 100% peta berwarna hitam.

## Pembaruan dari dalam aplikasi
- Atur `REPO` (pemilik/nama repo GitHub, harus publik) di `config.js`.
- Tiap build memberi nomor build otomatis. Di menu Atur ada tombol Cek pembaruan; jika ada build lebih baru, tombol Perbarui mengunduh APK dari rilis `latest` dan membuka installer.
- Saat pertama kali, Android meminta izin Install aplikasi dari sumber ini: izinkan, lalu tekan Perbarui lagi.

## GPS dan mode offline
- Input PKS dan Alokasi otomatis mencatat koordinat GPS (lat, lng, akurasi, waktu) dan tersimpan di kolom baru sheet `Input` dan `Alokasi`. GPS tetap jalan tanpa internet. Izin lokasi diminta saat pertama kali.
- Saat sinyal lemah atau offline, data disimpan sementara di HP (antrean, tanda ⏳ di riwayat) dan dikirim otomatis begitu ada sinyal: saat sinyal kembali, saat aplikasi dibuka lagi, dan tiap 30 detik. Tombol status di header menampilkan Offline / jumlah antrean / Tersinkron.
- **Wajib**: tempel ulang `apps-script/Code.gs` yang baru, lalu Deploy > Manage deployments > Edit > New version (URL tetap). Kolom lat/lng/akurasi/waktu ditambahkan otomatis ke sheet lama.

## Bagikan rekap
Di Dashboard (paling bawah) ada kartu **Bagikan rekap**, mengikuti pilihan pompa dan rotasi:
- **Kirim ringkasan ke WhatsApp**: teks ringkas (masuk PKS, dialokasi, outstanding, persen per blok), dibuka lewat menu bagikan Android.
- **Bagikan file Excel**: 3 sheet (Rekap Blok, Input PKS, Alokasi) lengkap dengan koordinat GPS. Dibuat langsung di HP, tidak perlu internet.
