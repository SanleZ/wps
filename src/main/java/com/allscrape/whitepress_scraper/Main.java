package com.allscrape.whitepress_scraper;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        WpScraper scraper = new WpScraper();
        scraper.scrape(false, 42);
    }
}
