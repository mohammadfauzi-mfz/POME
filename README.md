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

## Alokasi cepat
- Di Dashboard, pilih blok di peta lalu tekan **Alokasikan ke blok ini**, atau tekan **Alokasi cepat** di kartu pompa.
- Lembar alokasi menampilkan outstanding pompa, daftar blok, tombol **Sisa** per blok, dan **Isi otomatis** (membagi outstanding ke blok dengan capaian terendah lebih dulu). Bisa mengalokasikan banyak blok sekaligus; tersimpan offline juga.
- Perbaikan: tanggal dari Google Sheet kini selalu tampil dd/mm (perlu deploy ulang `Code.gs`), dan tanggal input memakai zona waktu HP.

## Tanggal, pompa fleksibel, dan impor riwayat
- Form **Input PKS** punya kolom *Tanggal alir*, form **Alokasi** dan lembar alokasi cepat punya *Tanggal alokasi* (default hari ini, bisa diganti untuk input data lama). Tanggal juga bisa dikoreksi lewat tombol Edit. Riwayat tampil dd/mm/yyyy, urut dari tanggal terbaru.
- **Pompa boleh kosong**: blok bisa ditambah tanpa pompa dan dikaitkan nanti lewat Atur › Daftar blok › Edit (luas dan pompa). Dashboard punya filter dan kartu *Tanpa pompa*. Luas bisa direvisi kapan saja; capaian dihitung ulang otomatis.
- **Impor CSV** (Atur › Impor data blok & riwayat): kolom `blok, luas, pompa, r1, r2, r3, tgl1, tgl2, tgl3`. Riwayat ditandai 📜 dan tidak mengurangi saldo outstanding pompa. Impor ulang file yang sama tidak membuat data ganda (ID tetap `rw-<blok>-R<n>`). Tempel ulang `Code.gs` agar kolom `riwayat` dan impor massal aktif.

## Peta: cubit zoom dan lapisan
- Dua jari untuk zoom in/out, satu jari menggeser (di tampilan biasa satu jari tetap menggulir halaman; pakai ⛶ layar penuh untuk geser bebas). Ketuk dua kali untuk zoom in. Roda mouse juga bisa di browser.
- Peta GeoJSON bisa berisi tiga lapisan lewat properti `jenis`: `Batas` (batas blok), `Existing` (area LA, diwarnai sesuai capaian), `Potensi` (garis putus-putus, tidak dihitung). Chip di atas peta menyalakan/mematikan Label, Batas blok, Area LA, Potensi. File tanpa `jenis` tetap dipakai seperti biasa (blok diwarnai penuh).

## Luas per periode dan filter tahun
- Tiap blok punya **luas dasar** dan daftar **perubahan luas** (Atur › Daftar blok › Edit): luas baru berlaku mulai rotasi dan tahun tertentu. Capaian tiap rotasi dihitung dengan luas yang berlaku saat itu, jadi data lama tidak berubah. Target 1 Tahun = jumlah 250 m³/ha per rotasi × luas rotasinya (mis. 2026: R1, R2 pakai 197 ha, R3 pakai 404 ha).
- Dashboard punya filter **Tahun** (diambil dari tanggal alir/alokasi). Outstanding tetap saldo keseluruhan lintas tahun.
- Peta: pada rotasi yang luasnya sudah bertambah, poligon Potensi ikut diwarnai bersama Existing.
- Sheet baru `Luas` (id, blok, tahun, rotasi, luas) dibuat otomatis oleh `Code.gs`. Impor CSV mendukung kolom `luas_baru` dan `mulai` (mis. `2026-R3`).
