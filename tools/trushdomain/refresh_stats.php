<?php
error_reporting(E_ALL & ~E_DEPRECATED);

$url = 'https://trustpositif.komdigi.go.id/Rest_server/gettotalrecordsperkategori';

$ch = curl_init();
curl_setopt_array($ch, [
    CURLOPT_URL => $url,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_FOLLOWLOCATION => true,
    CURLOPT_TIMEOUT => 30,
    CURLOPT_USERAGENT => 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124 Safari/537.36',
]);
$response = curl_exec($ch);
$error = curl_error($ch);
curl_close($ch);

if ($response === false) {
    header('Location: stats.php?error=' . urlencode($error));
    exit;
}

$data = json_decode($response, true);
if (json_last_error() !== JSON_ERROR_NONE) {
    header('Location: stats.php?error=' . urlencode('Format JSON tidak valid'));
    exit;
}

file_put_contents(__DIR__ . '/stats_kategori.json', json_encode($data, JSON_PRETTY_PRINT));
header('Location: stats.php?refreshed=1');
exit;
