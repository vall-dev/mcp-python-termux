<?php
require_once __DIR__ . '/db.php';
require_once __DIR__ . '/header.php';

$limit = isset($_GET['limit']) ? (int) $_GET['limit'] : 100;
if ($limit < 1 || $limit > 1000) {
    $limit = 100;
}

$rows = get_recent_searches($limit);
$total = count_search_history();
?>
<div class="page-header">
    <h1>Riwayat Pencarian</h1>
    <p class="lead">Hasil pemeriksaan yang tersimpan di database lokal.</p>
</div>

<div class="row">
    <div class="col-md-10 col-md-offset-1">
        <?php if (isset($_GET['cleared'])): ?>
            <div class="alert alert-success">Riwayat berhasil dibersihkan.</div>
        <?php endif; ?>

        <div class="panel panel-default">
            <div class="panel-heading">
                Riwayat
                <span class="label label-default pull-right"><?php echo $total; ?> record</span>
            </div>
            <div class="panel-body">
                <div class="btn-toolbar" role="toolbar">
                    <a href="export_history.php" class="btn btn-primary">Ekspor CSV</a>
                    <form method="post" action="clear_history.php" class="pull-right" onsubmit="return confirm('Bersihkan semua riwayat?');">
                        <button type="submit" class="btn btn-danger">Bersihkan Riwayat</button>
                    </form>
                </div>

                <div class="table-responsive" style="margin-top: 16px;">
                    <table class="table table-striped table-bordered">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Domain/URL</th>
                                <th>Status</th>
                                <th>Waktu</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php if (empty($rows)): ?>
                                <tr>
                                    <td colspan="4">Belum ada riwayat pencarian.</td>
                                </tr>
                            <?php else: ?>
                                <?php foreach ($rows as $row): ?>
                                    <tr>
                                        <td><?php echo (int) $row['id']; ?></td>
                                        <td><?php echo htmlspecialchars($row['domain']); ?></td>
                                        <td>
                                            <?php
                                            $statusLower = strtolower($row['status']);
                                            if ($statusLower === 'ada' || $statusLower === 'terblokir') {
                                                echo '<span class="label label-danger">' . htmlspecialchars($row['status']) . '</span>';
                                            } else {
                                                echo '<span class="label label-success">' . htmlspecialchars($row['status']) . '</span>';
                                            }
                                            ?>
                                        </td>
                                        <td><?php echo htmlspecialchars($row['created_at']); ?></td>
                                    </tr>
                                <?php endforeach; ?>
                            <?php endif; ?>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<?php require_once __DIR__ . '/footer.php'; ?>
