<?php
header('Content-Type: application/json; charset=utf-8');

error_reporting(E_ALL & ~E_DEPRECATED);

require_once __DIR__ . '/db.php';

$raw = '';
if (isset($_POST['domains'])) {
    $raw = $_POST['domains'];
} elseif (isset($_GET['domains'])) {
    $raw = $_GET['domains'];
}

if (trim($raw) === '') {
    echo json_encode(['error' => 'Domain tidak boleh kosong.']);
    exit;
}

$lines = preg_split('/[\r\n,]+/', $raw);
$domains = [];
$invalid = [];

foreach ($lines as $line) {
    $line = trim($line);
    if ($line === '') {
        continue;
    }
    $original = $line;
    $line = preg_replace('#^https?://#i', '', $line);
    $line = rtrim($line, '/');
    $line = strtolower($line);
    if (preg_match('/^[a-z0-9.-]+$/i', $line)) {
        $domains[] = $line;
    } else {
        $invalid[] = $original;
    }
}

$domains = array_values(array_unique($domains));
$totalDomains = count($domains);

if ($totalDomains > 50) {
    echo json_encode(['error' => 'Maksimal 50 domain per pencarian.']);
    exit;
}

$results = [];
$errors = [];

foreach (array_chunk($domains, 5) as $batch) {
    $query = implode("\n", $batch);
    $url = 'https://trustpositif.komdigi.go.id/welcome?domains=' . urlencode($query);

    $ch = curl_init();
    curl_setopt_array($ch, [
        CURLOPT_URL => $url,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_TIMEOUT => 30,
        CURLOPT_USERAGENT => 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124 Safari/537.36',
        CURLOPT_SSL_VERIFYPEER => true,
    ]);
    $html = curl_exec($ch);
    $curlError = curl_error($ch);
    curl_close($ch);

    if ($html === false) {
        $errors[] = 'Gagal mengambil data untuk: ' . implode(', ', $batch) . ' (' . $curlError . ')';
        continue;
    }

    $parsed = parse_result_rows($html);
    if (empty($parsed)) {
        foreach ($batch as $d) {
            $errors[] = 'Tidak dapat memproses hasil untuk: ' . $d;
        }
        continue;
    }

    foreach ($parsed as $row) {
        $results[] = $row;
        save_search_result($row['domain'], $row['status']);
    }
}

foreach ($invalid as $d) {
    $results[] = ['domain' => $d, 'status' => 'Input tidak valid'];
}

echo json_encode([
    'success' => true,
    'count' => count($results),
    'results' => $results,
    'errors' => $errors,
    'history_count' => count_search_history(),
]);

function parse_result_rows($html) {
    $rows = [];
    libxml_use_internal_errors(true);
    $doc = new DOMDocument();
    $doc->loadHTML($html);
    libxml_clear_errors();

    $xpath = new DOMXPath($doc);
    $trNodes = $xpath->query('//table[@id="daftar-block"]/tbody/tr');
    if ($trNodes === false || $trNodes->length === 0) {
        return $rows;
    }

    foreach ($trNodes as $tr) {
        $tds = $xpath->query('./td', $tr);
        if ($tds->length < 2) {
            continue;
        }
        $domain = trim($tds->item(0)->textContent);
        $status = trim($tds->item(1)->textContent);
        if ($domain !== '' && $status !== '') {
            $rows[] = ['domain' => $domain, 'status' => $status];
        }
    }

    return $rows;
}
