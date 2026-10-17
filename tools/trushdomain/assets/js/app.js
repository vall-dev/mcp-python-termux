$(document).ready(function () {
    var lastResults = [];

    function getTheme() {
        return document.body.className.indexOf('dark') !== -1 ? 'dark' : 'light';
    }

    function setTheme(theme) {
        if (theme === 'dark') {
            document.body.className = 'dark';
            $('#theme-toggle').text('Mode Terang');
        } else {
            document.body.className = 'light';
            $('#theme-toggle').text('Mode Gelap');
        }
        document.cookie = 'tp_theme=' + theme + '; path=/; max-age=31536000';
    }

    $('#theme-toggle').on('click', function (e) {
        e.preventDefault();
        var next = getTheme() === 'dark' ? 'light' : 'dark';
        setTheme(next);
    });

    if (getTheme() === 'dark') {
        $('#theme-toggle').text('Mode Terang');
    } else {
        $('#theme-toggle').text('Mode Gelap');
    }

    if ($('#check-form').length > 0) {
        $('#check-form').on('submit', function (e) {
            e.preventDefault();

            var domains = $.trim($('#domains').val());
            if (domains === '') {
                showMessage('warning', 'Domain tidak boleh kosong.');
                return;
            }

            var lines = domains.split(/\r?\n|,/).filter(function (line) {
                return $.trim(line) !== '';
            });

            if (lines.length > 50) {
                showMessage('warning', 'Maksimal 50 domain per pencarian.');
                return;
            }

            var btn = $('#btn-check');
            btn.prop('disabled', true).text('Memproses...');
            hideMessage();
            $('#result-panel').addClass('hidden');
            $('#result-table tbody').empty();
            lastResults = [];

            $.ajax({
                url: 'check.php',
                type: 'POST',
                data: { domains: domains },
                dataType: 'json',
                success: function (response) {
                    btn.prop('disabled', false).text('Cari Data');

                    if (response.error) {
                        showMessage('danger', response.error);
                        return;
                    }

                    if (response.results && response.results.length > 0) {
                        lastResults = response.results;
                        var tbody = $('#result-table tbody');
                        $.each(response.results, function (index, row) {
                            var tr = $('<tr>');
                            var tdDomain = $('<td>').text(row.domain);
                            var tdStatus = $('<td>').text(row.status);
                            if (row.status.toLowerCase() === 'ada' || row.status.toLowerCase() === 'terblokir') {
                                tdStatus.addClass('text-danger');
                            } else if (row.status.toLowerCase().indexOf('tidak') !== -1) {
                                tdStatus.addClass('text-success');
                            } else {
                                tdStatus.addClass('text-warning');
                            }
                            tr.append(tdDomain).append(tdStatus);
                            tbody.append(tr);
                        });
                        $('#result-panel').removeClass('hidden');

                        if (response.errors && response.errors.length > 0) {
                            showMessage('warning', response.errors.join(' '));
                        }
                    } else {
                        showMessage('info', 'Tidak ada hasil yang ditemukan.');
                    }
                },
                error: function (xhr, status, error) {
                    btn.prop('disabled', false).text('Cari Data');
                    showMessage('danger', 'Permintaan gagal: ' + error);
                }
            });
        });
    }

    $('#btn-export-csv').on('click', function (e) {
        e.preventDefault();
        if (lastResults.length === 0) {
            return;
        }

        var rows = [['Domain', 'Status']];
        $.each(lastResults, function (index, row) {
            rows.push([row.domain, row.status]);
        });

        var csv = rows.map(function (row) {
            return row.map(function (cell) {
                var value = String(cell).replace(/"/g, '""');
                return '"' + value + '"';
            }).join(',');
        }).join('\r\n');

        var blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        var link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = 'hasil_trustpositif.csv';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(link.href);
    });

    function showMessage(type, text) {
        var el = $('#message');
        el.removeClass('hidden alert-warning alert-danger alert-info alert-success')
            .addClass('alert alert-' + type)
            .text(text)
            .show();
    }

    function hideMessage() {
        var el = $('#message');
        el.addClass('hidden').hide();
    }
});
