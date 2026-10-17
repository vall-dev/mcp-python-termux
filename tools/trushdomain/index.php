<?php require_once __DIR__ . '/header.php'; ?>

<div class="page-header">
    <h1>Pemeriksaan Domain TrustPositif</h1>
    <p class="lead">Cek status domain atau URL terhadap basis data TrustPositif Komdigi.</p>
</div>

<div class="row">
    <div class="col-md-8 col-md-offset-2">
        <div class="panel panel-default">
            <div class="panel-heading">Form Pencarian</div>
            <div class="panel-body">
                <form id="check-form" class="form-horizontal" method="post" action="check.php">
                    <div class="form-group">
                        <label for="domains" class="col-sm-3 control-label">Domain atau URL</label>
                        <div class="col-sm-9">
                            <textarea id="domains" name="domains" class="form-control" rows="5" placeholder="Satu domain per baris, contoh: google.com"></textarea>
                            <span class="help-block">Gunakan satu domain per baris. Jangan sertakan http:// atau garis miring di akhir.</span>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="col-sm-offset-3 col-sm-9">
                            <button type="submit" class="btn btn-primary" id="btn-check">Cari Data</button>
                            <button type="reset" class="btn btn-default">Bersihkan</button>
                        </div>
                    </div>
                </form>
            </div>
        </div>

        <div id="message" class="hidden"></div>

        <div class="panel panel-default hidden" id="result-panel">
            <div class="panel-heading">
                Hasil Pemeriksaan
                <button type="button" class="btn btn-sm btn-default pull-right" id="btn-export-csv">Ekspor CSV</button>
            </div>
            <div class="panel-body">
                <div class="table-responsive">
                    <table class="table table-striped table-bordered" id="result-table">
                        <thead>
                            <tr>
                                <th>Domain/URL</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
            </div>
        </div>

        <div class="well well-sm">
            Sumber data: <a href="https://trustpositif.komdigi.go.id/" target="_blank" rel="noopener noreferrer">trustpositif.komdigi.go.id</a>.
            Hasil pencarian otomatis tersimpan di halaman <a href="history.php">Riwayat</a>.
        </div>
    </div>
</div>

<?php require_once __DIR__ . '/footer.php'; ?>
