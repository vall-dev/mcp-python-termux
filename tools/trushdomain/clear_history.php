<?php
require_once __DIR__ . '/db.php';

clear_search_history();
header('Location: history.php?cleared=1');
exit;
