package com.animekita.network;

import com.animekita.model.Anime;
import com.animekita.model.Episode;
import com.animekita.model.HomeResponse;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scraper {

    private static final String SRC = "https://s13.nontonanimeid.boats";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT = 15000;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    // ========== HTTP ==========

    static String httpGet(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(TIMEOUT);
        c.setReadTimeout(TIMEOUT);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Accept", "text/html,application/xhtml+xml");
        c.setRequestProperty("Accept-Language", "id-ID,id;q=0.9,en;q=0.8");
        c.setRequestProperty("Referer", SRC + "/");
        int code = c.getResponseCode();
        if (code != 200) { c.disconnect(); throw new Exception("HTTP " + code); }
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuffer sb = new StringBuffer(); String l;
        while ((l = r.readLine()) != null) sb.append(l).append("\n");
        r.close(); c.disconnect(); return sb.toString();
    }

    static String httpPost(String u, String data) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(TIMEOUT); c.setReadTimeout(TIMEOUT);
        c.setDoOutput(true);
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        c.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        c.setRequestProperty("Origin", SRC);
        c.setRequestProperty("Referer", SRC + "/");
        OutputStream os = c.getOutputStream();
        os.write(data.getBytes("UTF-8")); os.flush(); os.close();
        int code = c.getResponseCode();
        if (code != 200) { c.disconnect(); throw new Exception("HTTP " + code); }
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuffer sb = new StringBuffer(); String l;
        while ((l = r.readLine()) != null) sb.append(l).append("\n");
        r.close(); c.disconnect(); return sb.toString();
    }

    // ========== HELPERS ==========

    static String clean(String s) {
        if (s == null) return "";
        return android.text.Html.fromHtml(s, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim();
    }
    static String strip(String s) { return s == null ? "" : s.replaceAll("<[^>]+>", "").trim(); }
    static String re1(String html, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        return m.find() ? (m.group(1) != null ? m.group(1) : "") : "";
    }
    static String slug(String url) {
        if (url == null) return "";
        return url.replace(SRC + "/anime/", "").replace(SRC + "/", "").replaceAll("/$", "");
    }

    // ========== PARSE: CARDS (search) ==========

    static List<Anime> parseCards(String html) {
        List<Anime> list = new ArrayList<>();
        Matcher m = Pattern.compile("<a\\b[^>]*class=\"[^\"]*\\bas-anime-card\\b[^\"]*\"[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        while (m.find()) {
            String tag = m.group(0), inner = m.group(1);
            String href = re1(tag, "href=\"([^\"]+)\"");
            String title = clean(re1(inner, "<h3[^>]*class=\"as-anime-title\"[^>]*>([^<]+)<"));
            String poster = re1(inner, "<img[^>]+src=\"([^\"]+)\"");
            String rating = re1(inner, "as-rating[^>]*>.*?([\\d.]+)");
            String type = clean(re1(inner, "as-type[^>]*>\\s*<span[^>]*>.*?</span>\\s*([^<]+)"));
            String synopsis = clean(strip(re1(inner, "<p[^>]*class=\"as-synopsis\"[^>]*>(.*?)</p>")));
            Anime a = new Anime();
            a.setId(slug(href)); a.setTitle(title); a.setThumb(poster);
            a.setRating(rating); a.setGenre(type); a.setSynopsis(synopsis);
            list.add(a);
        }
        return list;
    }

    // ========== PARSE: ANIMESERIES (home) ==========

    static List<Anime> parseAnimeSeries(String html) {
        List<Anime> list = new ArrayList<>();
        Matcher m = Pattern.compile("<article\\b[^>]*class=\"[^\"]*\\banimeseries\\b[^\"]*\"[^>]*>(.*?)</article>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        while (m.find()) {
            String inner = m.group(1);
            String href = re1(inner, "href=\"([^\"]+)\"");
            if (href.isEmpty()) continue;
            String title = re1(inner, "data-title-default=\"([^\"]*)\"");
            if (title.isEmpty()) title = clean(strip(re1(inner, "<h3[^>]*class=\"title[^\"]*\"[^>]*>(.*?)</h3>")));
            else title = clean(title);
            String poster = re1(inner, "<img[^>]+src=\"([^\"]+)\"");
            String eps = clean(re1(inner, "<span class=\"types episodes\"[^>]*>.*?</span>\\s*([^<]*)"));
            String score = re1(inner, "kotakscore[^>]*>.*?([\\d.]+)");
            Anime a = new Anime();
            a.setId(slug(href)); a.setTitle(title); a.setThumb(poster);
            a.setRating(score); a.setStatus(eps);
            list.add(a);
        }
        return list;
    }

    // ========== PARSE: DETAIL ==========

    static Anime parseDetail(String html) {
        Anime a = new Anime();
        String title = clean(re1(html, "<h1[^>]*class=\"entry-title[^\"]*\"[^>]*>.*?data-title-default=\"([^\"]*)\""));
        if (title.isEmpty()) title = clean(re1(html, "<h1[^>]*class=\"entry-title[^\"]*\"[^>]*>\\s*Nonton\\s*<span[^>]*>([^<]+)"));
        a.setTitle(title);
        a.setThumb(re1(html, "<div class=\"anime-card__sidebar\">\\s*<img[^>]+src=\"([^\"]+)\""));
        a.setRating(clean(re1(html, "anime-card__score\">\\s*<span[^>]*>[^<]*</span>\\s*<span class=\"value\">([^<]+)<")));
        a.setType(clean(re1(html, "<span class=\"type\">([^<]+)<")));
        Matcher infoM = Pattern.compile("<span class=\"info-item[^\"]*\"[^>]*>(.*?)</span>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        if (infoM.find()) a.setStatus(clean(strip(infoM.group(1))));
        String syn = re1(html, "<div class=\"synopsis-prose\">(.*?)</div>");
        if (!syn.isEmpty()) a.setSynopsis(clean(strip(syn.replaceAll("<br\\s*/?>", "\n"))));
        StringBuilder genre = new StringBuilder();
        Matcher gm = Pattern.compile("<a[^>]+href=\"[^\"]+\"[^>]*class=\"genre-tag\"[^>]*>([^<]+)<",
            Pattern.CASE_INSENSITIVE).matcher(html);
        while (gm.find()) { if (genre.length() > 0) genre.append(", "); genre.append(clean(gm.group(1))); }
        a.setGenre(genre.toString());
        List<Episode> episodes = parseEpisodeItems(html);
        appendMoreEpisodes(html, episodes);
        sortEpisodes(episodes);
        a.setEpisodes(episodes);

        return a;
    }

    static String extractEpNumber(String url) {
        Matcher m = Pattern.compile("episode-(\\d+)", Pattern.CASE_INSENSITIVE).matcher(url);
        return m.find() ? m.group(1) : "";
    }

    static List<Episode> parseEpisodeItems(String html) {
        List<Episode> episodes = new ArrayList<>();
        Matcher em = Pattern.compile("<a\\b[^>]*episode-item[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        while (em.find()) {
            String openTag = em.group(0).substring(0, em.group(0).indexOf('>') + 1);
            String inner = em.group(1);
            String href = re1(openTag, "href=\"([^\"]+)\"");
            if (href.isEmpty()) continue;

            String epTitle = clean(strip(re1(inner, "<span[^>]*class=\"[^\"]*ep-title[^\"]*\"[^>]*>(.*?)</span>")));
            String epDate = clean(strip(re1(inner, "<span[^>]*class=\"[^\"]*ep-date[^\"]*\"[^>]*>(.*?)</span>")));
            String epNum = extractEpNumber(href);
            if (epTitle.isEmpty()) epTitle = epDate;
            if (epTitle.isEmpty()) epTitle = epNum.isEmpty() ? "Episode" : ("Episode " + epNum);

            Episode ep = new Episode();
            ep.setId(slug(href));
            ep.setTitle(epTitle);
            ep.setEpisodeNumber(epNum);
            episodes.add(ep);
        }
        return episodes;
    }

    static JSONObject extractJsVar(String html, String varName) {
        Matcher bm = Pattern.compile("data:text/javascript;base64,([A-Za-z0-9+/=]+)").matcher(html);
        while (bm.find()) {
            try {
                byte[] dec = android.util.Base64.decode(bm.group(1), android.util.Base64.DEFAULT);
                String d = new String(dec, "UTF-8");
                if (!d.contains(varName)) continue;
                int v = d.indexOf("var " + varName + "=");
                int s = v >= 0 ? d.indexOf("{", v) : d.indexOf("{");
                int e = d.lastIndexOf("}");
                if (s < 0 || e <= s) continue;
                return new JSONObject(d.substring(s, e + 1));
            } catch (Exception ignored) {}
        }
        return null;
    }

    static boolean hasEpisode(List<Episode> list, Episode ep) {
        if (ep == null || ep.getId() == null) return false;
        for (Episode e : list) {
            if (ep.getId().equals(e.getId())) return true;
        }
        return false;
    }

    static void appendMoreEpisodes(String html, List<Episode> episodes) {
        try {
            JSONObject params = extractJsVar(html, "misha_loadmore_params2");
            if (params == null) return;

            int maxPage = params.optInt("max_page", 0);
            int totalPosts = params.optInt("total_posts", 0);
            if (maxPage <= 0 || totalPosts <= episodes.size()) return;

            String ajaxurl = params.optString("ajaxurl", "");
            if (ajaxurl.isEmpty()) ajaxurl = SRC + "/wp-admin/admin-ajax.php";
            String nonce = params.optString("nonce", "");
            String posts = params.optString("posts", "");
            if (posts.isEmpty()) return;
            String type = params.optString("type", "anime");
            String postsToDisplay = params.optString("posts_to_display", "20");
            String isLargeSeries = params.optString("is_large_series", "");

            for (int page = 1; page <= maxPage; page++) {
                if (episodes.size() >= totalPosts) break;

                String data = "action=loadmore2"
                    + "&nonce=" + URLEncoder.encode(nonce, "UTF-8")
                    + "&query=" + URLEncoder.encode(posts, "UTF-8")
                    + "&page=" + page
                    + "&type=" + URLEncoder.encode(type, "UTF-8")
                    + "&posts_to_display=" + URLEncoder.encode(postsToDisplay, "UTF-8")
                    + "&is_large_series=" + URLEncoder.encode(isLargeSeries, "UTF-8")
                    + "&total_posts=" + totalPosts;

                String resp = httpPost(ajaxurl, data);
                if (resp == null || resp.trim().isEmpty() || resp.trim().equals("0")) break;

                List<Episode> more = parseEpisodeItems(resp);
                int before = episodes.size();
                for (Episode ep : more) {
                    if (!hasEpisode(episodes, ep)) episodes.add(ep);
                }
                if (episodes.size() == before) break;
            }
        } catch (Exception ignored) {}
    }

    static void sortEpisodes(List<Episode> episodes) {
        Collections.sort(episodes, new Comparator<Episode>() {
            @Override
            public int compare(Episode a, Episode b) {
                return compareEpisodes(a, b);
            }
        });
    }

    static int compareEpisodes(Episode a, Episode b) {
        int na = toInt(a.getEpisodeNumber());
        int nb = toInt(b.getEpisodeNumber());
        if (na != 0 && nb != 0) return na - nb;
        if (na != 0) return -1;
        if (nb != 0) return 1;
        return 0;
    }

    static int toInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    // ========== WATCH / PLAYER ==========

    static class WatchInfo { String title, poster, epNum, nonce; List<Server> servers = new ArrayList<>(); }
    static class Server { String post, type, nume, name; }
    static class PlayerData { String kind, url, title, error; }

    static WatchInfo parseWatch(String html) {
        WatchInfo w = new WatchInfo();
        Matcher sm = Pattern.compile("<li\\b[^>]*data-post=\"([^\"]+)\"[^>]*data-type=\"([^\"]+)\"[^>]*data-nume=\"([^\"]+)\"[^>]*>\\s*<span>([^<]+)<",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        while (sm.find()) {
            Server s = new Server();
            s.post = sm.group(1); s.type = sm.group(2); s.nume = sm.group(3); s.name = clean(sm.group(4));
            w.servers.add(s);
        }
        Matcher bm = Pattern.compile("data:text/javascript;base64,([A-Za-z0-9+/=]+)").matcher(html);
        while (bm.find()) {
            try {
                byte[] dec = android.util.Base64.decode(bm.group(1), android.util.Base64.DEFAULT);
                String d = new String(dec, "UTF-8");
                if (d.contains("kotakajax")) { String n = re1(d, "\"nonce\":\"([^\"]+)\""); if (!n.isEmpty()) w.nonce = n; }
                if (d.contains("episodeToTrack")) {
                    String p = re1(d, "\"poster\":\"([^\"]+)\""); if (!p.isEmpty()) w.poster = p.replace("\\/", "/");
                    String en = re1(d, "\"episodeNumber\":\"([^\"]+)\""); if (!en.isEmpty()) w.epNum = en;
                }
            } catch (Exception ignored) {}
        }
        w.title = clean(re1(html, "data-title-default=\"([^\"]*)\""));
        if (w.title.isEmpty()) w.title = clean(strip(re1(html, "<h1[^>]*>(.*?)</h1>"))).replaceAll("\\s*Sub Indo\\s*$", "");
        return w;
    }

    static PlayerData getPlayer(String epUrl, Server srv, String nonce) {
        PlayerData pd = new PlayerData();
        try {
            String data = "action=player_ajax&post=" + URLEncoder.encode(srv.post, "UTF-8")
                + "&nume=" + URLEncoder.encode(srv.nume, "UTF-8")
                + "&serverName=" + URLEncoder.encode(srv.type, "UTF-8")
                + "&nonce=" + URLEncoder.encode(nonce, "UTF-8");
            String resp = httpPost(SRC + "/wp-admin/admin-ajax.php", data);
            String iframe = re1(resp, "<iframe[^>]+src=\"([^\"]+)\"");
            if (iframe.isEmpty()) iframe = re1(resp, "data-src=\"([^\"]+)\"");
            if (iframe.isEmpty()) { pd.kind = "error"; pd.error = "No iframe"; return pd; }
            if (iframe.contains("kotakanimeid")) {
                try {
                    String emb = httpGet(iframe);
                    String m3u8 = extractM3u8(emb);
                    if (m3u8 != null && !m3u8.isEmpty()) { pd.kind = "hls"; pd.url = m3u8; pd.title = srv.name; return pd; }
                } catch (Exception ignored) {}
            }
            pd.kind = "iframe"; pd.url = iframe; pd.title = srv.name;
        } catch (Exception e) { pd.kind = "error"; pd.error = e.getMessage(); }
        return pd;
    }

    static String extractM3u8(String html) {
        Matcher pm = Pattern.compile("eval\\(function.*?</script>", Pattern.DOTALL).matcher(html);
        if (pm.find()) {
            String dec = unpackJs(pm.group(0));
            if (dec != null) {
                int si = dec.indexOf("sources");
                String block = si >= 0 ? dec.substring(si) : dec;
                int fi = block.indexOf("\"file\"");
                if (fi >= 0) {
                    String f = re1(block.substring(fi + 7).trim(), "^\"([^\"]+)\"");
                    if (!f.isEmpty()) return f.replaceAll("\\\\\\\\+", "/");
                }
            }
        }
        return re1(html, "(https?://[^\"'\\s]+\\.m3u8[^\"'\\s]*)");
    }

    static String unpackJs(String s) {
        try {
            s = s.replace("\\'", "'").replace("\\\"", "\"");
            int o = s.indexOf("}('"); if (o < 0) return null;
            int cl = s.lastIndexOf("'.split('|'),0,{}))"); if (cl < 0 || cl <= o) return null;
            String in = s.substring(o + 3, cl);
            Matcher pm = Pattern.compile("^(.*)',(\\d+),(\\d+),'(.*)$", Pattern.DOTALL).matcher(in);
            if (!pm.find()) return null;
            String p = pm.group(1); int a = Integer.parseInt(pm.group(2));
            int c = Integer.parseInt(pm.group(3)); String[] k = pm.group(4).split("\\|");
            Map<String, String> d = new HashMap<>();
            for (int i = 0; i < c; i++) { String key = n2b(i, a); d.put(key, (i < k.length && !k[i].isEmpty()) ? k[i] : key); }
            StringBuffer sb = new StringBuffer();
            Matcher wm = Pattern.compile("\\b[a-zA-Z0-9_]+\\b").matcher(p);
            while (wm.find()) wm.appendReplacement(sb, d.getOrDefault(wm.group(), wm.group()));
            wm.appendTail(sb); return sb.toString();
        } catch (Exception e) { return null; }
    }

    static String n2b(int n, int b) {
        StringBuilder s = new StringBuilder();
        while (n > 0) { int r = n % b; s.insert(0, r > 35 ? (char)(r+29) : r < 10 ? (char)('0'+r) : (char)(r+87)); n /= b; }
        return s.toString();
    }

    // ==================== PUBLIC API ====================

    /** Home — episode terbaru */
    public static void fetchHome(int page, Callback<HomeResponse> cb) {
        new Thread(() -> doFetchHome(page, cb)).start();
    }

    private static void doFetchHome(int page, Callback<HomeResponse> cb) {
        try {
            String url = SRC + (page > 1 ? "/page/" + page + "/" : "/");
            String html = httpGet(url);
            HomeResponse resp = new HomeResponse();
            resp.getAllAnime().addAll(parseAnimeSeries(html));
            cb.onSuccess(resp);
        } catch (Exception e) { cb.onError(e.getMessage()); }
    }

    /** Detail anime + episodes */
    public static void fetchDetail(String slug, Callback<Anime> cb) {
        new Thread(() -> doFetchDetail(slug, cb)).start();
    }

    private static void doFetchDetail(String slug, Callback<Anime> cb) {
        try {
            String html = httpGet(SRC + "/anime/" + slug + "/");
            Anime a = parseDetail(html);
            if (a.getTitle().isEmpty()) { cb.onError("Anime tidak ditemukan"); return; }
            cb.onSuccess(a);
        } catch (Exception e) { cb.onError(e.getMessage()); }
    }

    /** Episode — stream URL */
    public static void fetchEpisode(String epSlug, Callback<Episode> cb) {
        new Thread(() -> doFetchEpisode(epSlug, cb)).start();
    }

    private static void doFetchEpisode(String epSlug, Callback<Episode> cb) {
        try {
            String url = SRC + "/" + epSlug + "/";
            WatchInfo w = parseWatch(httpGet(url));
            if (w.servers.isEmpty()) { cb.onError("Tidak ada server"); return; }
            Server first = w.servers.get(0);
            PlayerData p = getPlayer(url, first, w.nonce);
            Episode ep = new Episode();
            ep.setId(epSlug); ep.setTitle(w.title); ep.setEpisodeNumber(w.epNum); ep.setThumb(w.poster);
            if ("hls".equals(p.kind) || "iframe".equals(p.kind)) ep.setVideoUrl(p.url);
            cb.onSuccess(ep);
        } catch (Exception e) { cb.onError(e.getMessage()); }
    }

    /** Search */
    public static void searchAnime(String query, int page, Callback<List<Anime>> cb) {
        new Thread(() -> doSearchAnime(query, page, cb)).start();
    }

    private static void doSearchAnime(String query, int page, Callback<List<Anime>> cb) {
        try {
            String q = URLEncoder.encode(query, "UTF-8");
            String html = httpGet(SRC + "/?s=" + q + "&paged=" + page);
            cb.onSuccess(parseCards(html));
        } catch (Exception e) { cb.onError(e.getMessage()); }
    }
}
