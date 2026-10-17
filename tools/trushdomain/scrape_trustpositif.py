#!/usr/bin/env python3
"""
Scraper TrustPositif Komdigi (https://trustpositif.komdigi.go.id/)
Fitur:
  - Simpan halaman beranda
  - Simpan statistik kategori
  - Download daftar blacklist resmi (file teks besar, otomatis di-gzip)
  - Cek status domain/URL via endpoint pencarian publik /welcome?domains=...

Pemakaian:
  python3 scrape_trustpositif.py --home --stats
  python3 scrape_trustpositif.py --download-blacklist
  python3 scrape_trustpositif.py --check "google.com,detik.com,facebook.com"
  python3 scrape_trustpositif.py --check-file domains.txt --out hasil.csv
"""

import argparse
import csv
import gzip
import html
import re
import sys
from pathlib import Path

import requests

BASE = "https://trustpositif.komdigi.go.id"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8",
}
OUT_DIR = Path(__file__).resolve().parent


def fetch(url: str, **kwargs) -> requests.Response:
    r = requests.get(url, headers=HEADERS, timeout=kwargs.pop("timeout", 60), **kwargs)
    r.raise_for_status()
    return r


def save_homepage() -> Path:
    out = OUT_DIR / "trustpositif.html"
    r = fetch(BASE + "/")
    out.write_bytes(r.content)
    print(f"[+] Halaman beranda disimpan: {out} ({len(r.content)} bytes)")
    return out


def save_stats() -> Path:
    out = OUT_DIR / "stats_kategori.json"
    r = fetch(BASE + "/Rest_server/gettotalrecordsperkategori")
    out.write_bytes(r.content)
    print(f"[+] Statistik kategori disimpan: {out}")
    print(r.text[:500])
    return out


def download_blacklist() -> Path:
    raw = OUT_DIR / "blacklist_trustpositif.txt"
    gz = OUT_DIR / "blacklist_trustpositif.txt.gz"
    url = BASE + "/assets/db/domains"
    print(f"[*] Download blacklist dari {url} ...")
    with requests.get(url, headers=HEADERS, stream=True, timeout=180) as r:
        r.raise_for_status()
        total = int(r.headers.get("content-length", 0))
        done = 0
        with open(raw, "wb") as f:
            for chunk in r.iter_content(chunk_size=1024 * 1024):
                if not chunk:
                    continue
                f.write(chunk)
                done += len(chunk)
                if total:
                    print(f"\r    {done}/{total} bytes ({done*100//total}%)", end="", flush=True)
        print()
    with open(raw, "rb") as fin, gzip.open(gz, "wb") as fout:
        while True:
            chunk = fin.read(1024 * 1024)
            if not chunk:
                break
            fout.write(chunk)
    raw.unlink()
    print(f"[+] Blacklist di-gzip: {gz} ({gz.stat().st_size} bytes)")
    return gz


def parse_search(html_text: str):
    """Ambil pasangan domain -> status dari halaman hasil /welcome."""
    text = re.sub(r"<script.*?</script>", " ", html_text, flags=re.S | re.I)
    text = re.sub(r"<style.*?</style>", " ", text, flags=re.S | re.I)
    text = re.sub(r"<[^>]+>", " ", text)
    text = html.unescape(text)
    text = re.sub(r"\s+", " ", text)

    start = text.find("Domain/URL Status")
    end = text.find("PENGAJUAN NORMALISASI", start)
    if start == -1:
        return []
    seg = text[start:end if end != -1 else len(text)]
    seg = seg.replace("Domain/URL Status", "", 1).strip()

    results = []
    for block in re.findall(r"([^ ]+(?:\.[^ ]+)+?)\s+(Ada|Tidak Ada|Not Found|Terblokir|Normal|Suspect)", seg):
        domain, status = block
        results.append((domain.strip(), status.strip()))
    return results


def check_domains(domains: list[str], out_csv: Path | None = None) -> list[dict]:
    # Max 5 URL per request sesuai petunjuk situs
    results: list[dict] = []
    for i in range(0, len(domains), 5):
        batch = domains[i:i+5]
        q = "\n".join(batch)
        r = fetch(BASE + "/welcome", params={"domains": q})
        pairs = parse_search(r.text)
        print(f"[*] Batch {i//5 + 1}: {batch} -> {pairs}")
        results.extend({"domain": d, "status": s} for d, s in pairs)

    if out_csv:
        with open(out_csv, "w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(f, fieldnames=["domain", "status"])
            writer.writeheader()
            writer.writerows(results)
        print(f"[+] Hasil CSV disimpan: {out_csv}")
    return results


def main():
    ap = argparse.ArgumentParser(description="Scraper TrustPositif Komdigi")
    ap.add_argument("--home", action="store_true", help="Simpan halaman beranda")
    ap.add_argument("--stats", action="store_true", help="Simpan statistik kategori")
    ap.add_argument("--download-blacklist", action="store_true", help="Download daftar blacklist resmi")
    ap.add_argument("--check", help="Cek domain/URL, pisahkan dengan koma. Maks 5 per request.")
    ap.add_argument("--check-file", help="Cek dari file teks, satu domain per baris")
    ap.add_argument("--out", default="hasil_trustpositif.csv", help="Output CSV untuk --check")
    args = ap.parse_args()

    if args.home:
        save_homepage()
    if args.stats:
        save_stats()
    if args.download_blacklist:
        download_blacklist()

    domains = []
    if args.check:
        domains = [d.strip() for d in args.check.split(",") if d.strip()]
    if args.check_file:
        domains += [line.strip() for line in open(args.check_file, encoding="utf-8") if line.strip()]

    if domains:
        # Hapus protokol/trailing slash agar format query benar
        cleaned = []
        for d in domains:
            d = re.sub(r"^https?://", "", d)
            d = d.rstrip("/")
            cleaned.append(d)
        check_domains(cleaned, Path(args.out))

    if not (args.home or args.stats or args.download_blacklist or domains):
        ap.print_help()


if __name__ == "__main__":
    main()
