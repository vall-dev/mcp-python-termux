<?php
require_once __DIR__ . '/config.php';

function db(): PDO {
    static $pdo = null;
    if ($pdo === null) {
        $dir = __DIR__ . '/data';
        if (!is_dir($dir)) {
            mkdir($dir, 0755, true);
        }
        $pdo = new PDO('sqlite:' . $dir . '/trustpositif.sqlite');
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $pdo->exec(
            'CREATE TABLE IF NOT EXISTS searches (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                domain TEXT NOT NULL,
                status TEXT NOT NULL,
                created_at TEXT NOT NULL
            )'
        );
        $pdo->exec(
            'CREATE INDEX IF NOT EXISTS idx_searches_created_at ON searches(created_at)'
        );
    }
    return $pdo;
}

function save_search_result(string $domain, string $status): void {
    $stmt = db()->prepare(
        'INSERT INTO searches (domain, status, created_at) VALUES (:domain, :status, :created_at)'
    );
    $stmt->execute([
        ':domain' => $domain,
        ':status' => $status,
        ':created_at' => date('Y-m-d H:i:s'),
    ]);
}

function get_recent_searches(int $limit = 50): array {
    $stmt = db()->prepare(
        'SELECT id, domain, status, created_at
         FROM searches
         ORDER BY id DESC
         LIMIT :limit'
    );
    $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
    $stmt->execute();
    return $stmt->fetchAll(PDO::FETCH_ASSOC);
}

function get_all_searches(): array {
    $stmt = db()->query(
        'SELECT id, domain, status, created_at
         FROM searches
         ORDER BY id DESC'
    );
    return $stmt->fetchAll(PDO::FETCH_ASSOC);
}

function clear_search_history(): void {
    db()->exec('DELETE FROM searches');
}

function count_search_history(): int {
    return (int) db()->query('SELECT COUNT(*) FROM searches')->fetchColumn();
}
