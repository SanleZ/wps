package com.allscrape.whitepress_scraper.utils;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;


public class HttpUtils {
    public static final String ORIGIN = "Origin";
    public static final String REFERER = "Referer";
    public static final String X_REQUESTED_WITH = "X-Requested-With";
    public static final String UPGRADE_INSECURE_REQUESTS = "Upgrade-Insecure-Requests";
    public static final String CONTENT_TYPE = "Content-Type";
    private static final String ERR_FORMAT = "Request error, URL:%s, HTTP status code:%d%n";


    public static HttpResp get(String urlString, CookieManager cm, Map<String, String> headers, int timeout) throws IOException {
        URL url = new URL(urlString);
        boolean isHttps = url.getProtocol().equals("https");
        String host = url.getHost();
        URLConnection connection = isHttps ? (HttpsURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection();

        if (cm != null) {
            cm.setCookies(connection);
        }

        //settings headers
        connection.setRequestProperty("Host", host);
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/json,application/xml;q=0.9,image/webp,*/*;q=0.8");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.8,ru;q=0.6");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout * 2);
        connection.connect();

        if (cm != null) {
            cm.storeCookies(connection);
        }

        int code = isHttps ? ((HttpsURLConnection) connection).getResponseCode() : ((HttpURLConnection) connection).getResponseCode();

        if (code == 200) {
            try (InputStream in = connection.getInputStream()) {
                InputStream stream = in;
                if ("gzip".equals(connection.getHeaderField("Content-Encoding"))) {
                    stream = new ZipInputStream(in);
                }
                return new HttpResp(code, readFully(stream));
            }
        }
        String errFormatString = String.format(ERR_FORMAT, urlString, code);
        throw new IOException(errFormatString);
    }

    public static HttpResp post(String urlString, byte[] data, CookieManager cm, Map<String, String> headers, int timeout) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout * 2);
        connection.setInstanceFollowRedirects(false);

        if (cm != null) {
            cm.setCookies(connection);
        }

        connection.setRequestProperty("Host", url.toString());
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Accept-Charset", "text/html,application/xhtml+xml,application/json,application/xml;q=0.9,image/webp,*/*;q=0.8");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        connection.setRequestMethod("POST");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        OutputStream out = connection.getOutputStream();
        out.write(data);
        out.close();

        int code = connection.getResponseCode();
        if (code == 200) {
            if (cm != null) {
                cm.storeCookies(connection);
            }
            try (InputStream in = connection.getInputStream()) {
                InputStream stream = in;
                if ("gzip".equals(connection.getHeaderField("Content-Encoding"))) {
                    stream = new ZipInputStream(in);
                }
                return new HttpResp(code, readFully(stream));
            }
        } else if (code == 302) {
            if (cm != null) {
                cm.storeCookies(connection);
            }

            Map<String, List<String>> fields = connection.getHeaderFields();
            String value;
            if (fields.containsKey("Location") && !fields.get("Location").isEmpty()
                    || (fields.containsKey("location") && !fields.get("location").isEmpty())) {
                if (fields.containsKey("Location")) {
                    value = fields.get("Location").get(0);
                } else {
                    value = fields.get("location").get(0);
                }
                if (!value.contains("http")) {
                    value = url.getProtocol() + "://" + url.getHost() + value;
                }
                return get(value, cm, headers, timeout);
            }
        }
        String errFormatString = String.format(ERR_FORMAT, urlString, code);
        throw new IOException(errFormatString);
    }


    private static String readFully(InputStream source) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[10 * 1024];
        int n;
        while ((n = source.read(buf)) > 0) {
            baos.write(buf, 0, n);
        }
        return baos.toString();
    }

}
