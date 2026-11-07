BUDGETLENS
IF570L - Lab Mobile Application Programming
Semester Ganjil Tahun Akademik 2026/2027

1. IDENTITAS KELOMPOK
Nama kelompok : Certified
Mata kuliah   : Mobile Application Programming
Kode / kelas  : IF570 / C
Nama aplikasi : BudgetLens

Anggota kelompok:
1. Marcelline Yulencia - 00000107780
2. Richard Liu - 00000107431
3. Dwiky Christian Febriano - 00000106196
4. Michael Christopher Salim - 00000105527

2. DESKRIPSI SINGKAT APLIKASI
BudgetLens adalah aplikasi pencatatan keuangan pribadi dan pembagian tagihan bersama berbasis Kotlin dan Jetpack Compose. Pengguna dapat mencatat 
pemasukan/pengeluaran, memantau budget, melihat rincian transaksi, mengelola Split Bill dan pengeluaran grup, serta mengatur profil.

Aplikasi menerapkan custom theme Material 3, state management, navigasi dengan pengiriman argumen, validasi input, dan dummy data lokal. Tersedia
layar list, detail, form, Scan Struk, serta Beranda sebagai layar bebas. Kamera menggunakan CameraX; ekstraksi isi struk masih berupa simulasi.
Pembayaran sesungguhnya dilakukan di luar aplikasi.

3. SISTEM KERJA KELOMPOK
- Bagi pekerjaan berdasarkan modul dan sepakati komponen UI, model data, serta hubungan navigasi yang digunakan bersama.
- Koordinasikan perubahan file bersama agar pekerjaan tidak saling menimpa.
- Gabungkan pekerjaan secara bertahap ke satu project acuan, kemudian periksa build dan jalankan pengujian modul yang terpengaruh.
- Lakukan review silang terhadap layout, validasi, loading/seleksi, pemulihan state, perilaku kembali, dan konsistensi dummy data.
- Catat pekerjaan nyata tiap anggota, hasil uji, serta perbaikan yang dibuat.
- Siapkan source project, APK utama, README, dan video maksimal 5 menit. Video memperlihatkan jenis layar wajib, theme, state, navigasi dengan
  argumen, dan validasi input sesuai rubrik UTS.
- Kemas seluruh berkas dalam Certified_BudgetLens.zip. Setiap anggota mengunggah file akhir yang identik sesuai ketentuan pengumpulan.

4. PEMBAGIAN TUGAS DAN KONTRIBUSI INDIVIDU
A. Marcelline Yulencia
- Split Bill dan pengeluaran Grup: pembagian rata, per item, dan persen, pemilihan anggota, serta rincian tagihan per orang.
- State anggota, draft, alokasi, status pembayaran, dan pemulihan layar.
- Integrasi modul, konsistensi perhitungan, dan pengujian alur lintas fitur.

B. Richard Liu
- List Transaksi, Tambah Transaksi, dan Detail Transaksi.
- Pencarian/filter, tambah/hapus catatan, serta navigasi dengan transactionId.
- Validasi nominal, tanggal, field wajib, keyboard, dan pengujian form.

C. Dwiky Christian Febriano
- Beranda, daftar/detail Budget, dan form tambah/edit budget.
- Filter bulan/tahun/tanggal, kategori, limit, dan ringkasan pemakaian budget.
- Model dummy data, konsistensi data antarlayar, serta pengujian ringkasan.

D. Michael Christopher Salim
- Masuk/Daftar, Profil, pengaturan, notifikasi, dan alur UI Scan Struk.
- Validasi akun, state loading, serta konsistensi theme terang/gelap.
- Pengujian modul terkait, ekspor PDF, dan pemeriksaan berkas pengumpulan.

