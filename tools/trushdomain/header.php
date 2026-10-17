<?php
require_once __DIR__ . '/config.php';

$currentPage = basename($_SERVER['PHP_SELF']);
$theme = 'light';
if (isset($_COOKIE['tp_theme']) && $_COOKIE['tp_theme'] === 'dark') {
    $theme = 'dark';
}

function navActive(string $page, string $current): string {
    return $page === $current ? ' class="active"' : '';
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><?php echo htmlspecialchars(APP_NAME); ?></title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="assets/css/style.css">
</head>
<body class="<?php echo $theme === 'dark' ? 'dark' : 'light'; ?>">

<nav class="navbar navbar-default navbar-static-top">
    <div class="container">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false">
                <span class="sr-only">Buka navigasi</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="index.php"><?php echo htmlspecialchars(APP_NAME); ?></a>
        </div>
        <div id="navbar" class="collapse navbar-collapse">
            <ul class="nav navbar-nav">
                <li<?php echo navActive('index.php', $currentPage); ?>><a href="index.php">Beranda</a></li>
                <li<?php echo navActive('stats.php', $currentPage); ?>><a href="stats.php">Statistik</a></li>
                <li<?php echo navActive('history.php', $currentPage); ?>><a href="history.php">Riwayat</a></li>
            </ul>
            <ul class="nav navbar-nav navbar-right">
                <li><a href="#" id="theme-toggle">Mode Gelap</a></li>
            </ul>
        </div>
    </div>
</nav>

<div class="container main-container">
