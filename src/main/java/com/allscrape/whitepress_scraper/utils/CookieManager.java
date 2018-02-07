package com.allscrape.whitepress_scraper.utils;

import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class CookieManager {
    private static final String SET_COOKIE = "Set-Cookie";
    private static final String COOKIE_VALUE_DELIMITER = ";";
    private static final String NAME_VALUE_SEPARATOR = "=";
    private static final String DATE_FORMAT = "EEE, dd-MMM-yyyy hh:mm:ss z";
    private static final String COOKIE_SEPARATOR = "; ";
    private static final String COOKIE = "Cookie";
    private static final String EXPIRESLC = "expires";
    private static final String EXPIRESUC = "Expires";
    private static final String PATH = "path";

    private DateFormat dateFormat;

    private Map<String, Map<String, Map<String, String>>> store;


    public CookieManager() {
        store = new HashMap<>();
        dateFormat = new SimpleDateFormat(DATE_FORMAT);
    }

    /**
     * Retrieves and stores cookies returned by the server through {@link URLConnection}
     *
     * @param connection is a {@link URLConnection} must have been opened by the method connect()
     */
    public void storeCookies(URLConnection connection) {

        String domain = connection.getURL().getHost();
        Map<String, Map<String, String>> domainStore = store.computeIfAbsent(domain, cookiesMap -> new HashMap<>());
        List<String> cookiesList = connection.getHeaderFields().get(SET_COOKIE);
        if (cookiesList != null && !cookiesList.isEmpty()) {
            cookiesList.stream() //get "Set-Cookie" headers (list of String)
                    .map(str -> str.split(COOKIE_VALUE_DELIMITER)) // every String split by ";"
                    .filter(arr -> arr.length > 0)
                    .forEach(cookieStringArr -> {
                        //first element should be main and define key of cookies map
                        String[] cookieValueArr = cookieStringArr[0].split(NAME_VALUE_SEPARATOR);
                        //name of cookie
                        String name = cookieValueArr[0];
                        //map of cookies props
                        Map<String, String> cookieMap = new HashMap<>();
                        Arrays.stream(cookieStringArr).forEach(str -> {
                            String[] cookieArr = str.split(NAME_VALUE_SEPARATOR, 2);
                            if (cookieArr.length == 2) {
                                cookieMap.put(cookieArr[0], cookieArr[1]);
                                domainStore.put(name, cookieMap);
                            }
                        });
                    });
        }
    }


    public void setCookies(URLConnection connection) {
        URL url = connection.getURL();
        String domain = url.getHost();
        String path = url.getPath();

        Map<String, Map<String, String>> domainStore = store.get(domain);
        StringBuilder cookieSb = new StringBuilder();
        if (domainStore == null) {
            return;
        }
        domainStore.forEach((cookieName, cookieValues) -> {
            if (comparePaths(cookieValues.get(PATH), path)
                    && expiresCheck(cookieValues.get(EXPIRESUC))
                    && expiresCheck(cookieValues.get(EXPIRESLC))) {
                cookieSb.append(cookieName).append("=").append(cookieValues.get(cookieName)).append(COOKIE_SEPARATOR);
            }
        });
        if (cookieSb.length() > 0) {
            connection.setRequestProperty(COOKIE, cookieSb.subSequence(0, cookieSb.length() - 2).toString());
        }
    }

    private boolean expiresCheck(String cookieExpires) {
        if (cookieExpires == null) return true;
        Date now = new Date();
        try {
            return now.compareTo(dateFormat.parse(cookieExpires)) <= 0;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean comparePaths(String cookiePath, String targetPath) {
        return cookiePath == null
                || cookiePath.equals("/")
                || targetPath.regionMatches(0, cookiePath, 0, cookiePath.length());

    }

    @Override
    public String toString() {
        return store.toString();
    }

    public Map<String, Map<String, Map<String, String>>> getStore() {
        return store;
    }

    public void setStore(Map<String, Map<String, Map<String, String>>> store) {
        this.store = store;
    }
}
