<?php require_once __DIR__ . '/header.php'; ?>

<?php
$statsFile = __DIR__ . '/stats_kategori.json';
$stats = [];
$lastDate = 'Tidak tersedia';

if (file_exists($statsFile)) {
    $json = file_get_contents($statsFile);
    $data = json_decode($json, true);
    if (isset($data[0])) {
        $stats = $data[0];
        $lastDate = isset($stats['lastdate']) ? $stats['lastdate'] : $lastDate;
    }
}

$items = [
    ['key' => 'TotalPerjudian', 'label' => 'Perjudian'],
    ['key' => 'TotalPornografi', 'label' => 'Pornografi'],
    ['key' => 'TotalRadikalisme', 'label' => 'Radikalisme'],
    ['key' => 'TotalPenipuanOnline', 'label' => 'Penipuan Online'],
    ['key' => 'TotalInvestasiIlegal', 'label' => 'Investasi Ilegal'],
    ['key' => 'TotalObatdanKosmetikIlegal', 'label' => 'Obat dan Kosmetik Ilegal'],
    ['key' => 'TotalHKI', 'label' => 'HKI'],
    ['key' => 'TotalPelanggaranKeamananInformasi', 'label' => 'Pelanggaran Keamanan Informasi'],
    ['key' => 'TotalKekerasan', 'label' => 'Kekerasan'],
    ['key' => 'TotalKekerasanPornografiAnak', 'label' => 'Kekerasan Pornografi Anak'],
    ['key' => 'TotalSARA', 'label' => 'SARA'],
    ['key' => 'TotalPelanggaranUUITE', 'label' => 'Pelanggaran UU ITE'],
    ['key' => 'TotalPerdaganganProdukAturanKhusus', 'label' => 'Perdagangan Produk Aturan Khusus'],
    ['key' => 'TotalKontenNegatifDirekomendasikanInstansiSektor', 'label' => 'Konten Negatif Rekomendasi Instansi Sektor'],
    ['key' => 'TotalKontenMeresahkanMasyarakat', 'label' => 'Konten Meresahkan Masyarakat'],
    ['key' => 'TotalFitnah', 'label' => 'Fitnah'],
    ['key' => 'TotalSeparatisme', 'label' => 'Separatisme'],
    ['key' => 'TotalKontenMelanggarNilaiSosialBudaya', 'label' => 'Konten Melanggar Nilai Sosial Budaya'],
    ['key' => 'TotalHOAKS', 'label' => 'Hoaks'],
    ['key' => 'TotalPemerasan', 'label' => 'Pemerasan'],
    ['key' => 'TotalKontenMemfasilitasiKontenNegatif', 'label' => 'Konten Memfasilitasi Konten Negatif'],
];

$max = 0;
foreach ($items as $item) {
    $value = isset($stats[$item['key']]) ? (int) $stats[$item['key']] : 0;
    if ($value > $max) {
        $max = $value;
    }
}
?>

<div class="page-header">
    <h1>Statistik Kategori</h1>
    <p class="lead">Jumlah record per kategori dari TrustPositif.</p>
</div>

<div class="row">
    <div class="col-md-8 col-md-offset-2">
        <div class="panel panel-default">
            <div class="panel-heading">
                Data Kategori
                <form method="post" action="refresh_stats.php" class="pull-right" style="margin-top: -4px;">
                    <button type="submit" class="btn btn-sm btn-default">Perbarui Statistik</button>
                </form>
            </div>
            <div class="panel-body">
                <p>Update terakhir: <strong><?php echo htmlspecialchars($lastDate); ?></strong></p>
                <?php if (empty($stats)): ?>
                    <div class="alert alert-warning">Data statistik belum tersedia.</div>
                <?php else: ?>
                    <?php foreach ($items as $item): ?>
                        <?php
                        $value = isset($stats[$item['key']]) ? (int) $stats[$item['key']] : 0;
                        $percent = $max > 0 ? round(($value / $max) * 100) : 0;
                        ?>
                        <div class="form-group">
                            <label><?php echo htmlspecialchars($item['label']); ?></label>
                            <div class="progress" style="margin-bottom: 6px;">
                                <div class="progress-bar" role="progressbar" aria-valuenow="<?php echo $value; ?>" aria-valuemin="0" aria-valuemax="<?php echo $max; ?>" style="width: <?php echo $percent; ?>%;">
                                    <?php echo $value; ?>
                                </div>
                            </div>
                        </div>
                    <?php endforeach; ?>
                <?php endif; ?>
            </div>
        </div>
    </div>
</div>

<?php require_once __DIR__ . '/footer.php'; ?>
