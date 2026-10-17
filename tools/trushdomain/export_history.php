<?php
require_once __DIR__ . '/db.php';

error_reporting(E_ALL & ~E_DEPRECATED);

header('Content-Type: text/csv; charset=utf-8');
header('Content-Disposition: attachment; filename="riwayat_trustpositif.csv"');

$rows = get_all_searches();

$output = fopen('php://output', 'w');
fwrite($output, "\xEF\xBB\xBF");
fputcsv($output, ['ID', 'Domain', 'Status', 'Waktu'], ',', '"', '\\');
foreach ($rows as $row) {
    fputcsv($output, [$row['id'], $row['domain'], $row['status'], $row['created_at']], ',', '"', '\\');
}
fclose($output);
exit;
