package com.allscrape.whitepress_scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.internal.Coordinates;
import org.openqa.selenium.internal.Locatable;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WpScraper {
    private static final String LINK_INIT_GET = "https://www.whitepress.pl/konto";
    private static final String LINK_LOGIN_POST = "https://www.whitepress.pl/www/includes/form.php?type=form&id=konto_logowanie_form";
    private static final String DATA_LOGIN_NAME = "snuiverink@gmail.com";
    private static final String DATA_LOGIN_PWD = "Rikjandirk123$";
    private static final String NEXT_PAGE_GET = "https://www.whitepress.pl/konto/reklamodawca-artykuly-sponsorowane/zamow-publikacje/serwis-24387?page=";
    private static final String LINK_PORTALS_GET = "https://www.whitepress.pl/konto/reklamodawca-artykuly-sponsorowane/zamow-publikacje";
    private static final String LINK_PORTALS_GET_ALL = "https://www.whitepress.pl/konto/reklamodawca-artykuly-sponsorowane/zamow-publikacje/serwis-24387";
    private static String pathToDriver = "D:\\Distrib\\dev\\_utility\\webdrivers\\chromedriver\\chromedriver.exe";
    private WebDriverWait wait;
    private static final Random RND = new Random();

    public void scrape(boolean headless, int startPage) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("d:\\output.csv", true))) {
            if (startPage <= 1) {
                bufferedWriter.write("\"WEBSITE\",\"# OF DAYS\", \"PRICE\",\"WRITE_ARTICLE\",\"DESIGNATION\"\n");
                startPage = 1;
            }
            System.setProperty("webdriver.chrome.driver", pathToDriver);
            WebDriver driver;
            ChromeOptions options = new ChromeOptions();
            options.addArguments("start-maximized");
            if (headless) {
                options.addArguments("--headless");
                driver = new ChromeDriver(options);
            } else {
                driver = new ChromeDriver(options);
            }
            driver.get(LINK_INIT_GET);
            wait = new WebDriverWait(driver, 30);
            WebElement submitButton;
            WebElement nameField;
            WebElement passwordField;
            try {
                nameField = driver.findElement(By.cssSelector("input#login"));
                passwordField = driver.findElement(By.cssSelector("input#password"));
                submitButton = driver.findElement(By.cssSelector("form[name=konto_logowanie_form] button[type=submit]"));
            } catch (TimeoutException | NoSuchElementException e) {
                return;
            }
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input#login")));
            nameField.sendKeys(DATA_LOGIN_NAME);
            passwordField.sendKeys(DATA_LOGIN_PWD);
            submitButton.submit();

            By sectionBy = By.cssSelector("div#section_link3");
            wait.until(ExpectedConditions.presenceOfElementLocated(sectionBy));
            driver.findElement(sectionBy).click();
            try {
                Thread.sleep(RND.nextInt(2000) + 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            By linkBy = By.cssSelector("a.zamow_pub2");
            wait.until(ExpectedConditions.presenceOfElementLocated(linkBy));
            driver.findElement(linkBy).click();
            try {
                Thread.sleep(RND.nextInt(2000) + 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            By portalsBy = By.cssSelector("tr.td_middle a.button");
            wait.until(ExpectedConditions.presenceOfElementLocated(portalsBy));
            driver.findElement(portalsBy).click();

            if (startPage > 1) {
                driver.get(NEXT_PAGE_GET + startPage);
            }

            By firstButtonBy = By.cssSelector("table.konto tr td:last-of-type a");
            wait.until(ExpectedConditions.presenceOfElementLocated(firstButtonBy));
            JavascriptExecutor jse = (JavascriptExecutor) driver;
            List<WebElement> elements = driver.findElements(By.cssSelector("table.konto tbody tr"));
            int page = startPage;
            while (true) {
                for (WebElement webElement : elements) {
                    String url = "";
                    WebElement nameElm = webElement.findElement(By.cssSelector("td:first-of-type div.projekt_cut a"));
                    if (nameElm != null) {
                        url = nameElm.getText();
                    }
                    webElement = webElement.findElement(By.cssSelector("td:last-of-type a"));
                    try {
                        Thread.sleep(RND.nextInt(500) + 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    jse.executeScript("window.scrollBy(0,250)", "");

                    wait.until(ExpectedConditions.elementToBeClickable(webElement));
                    webElement.click();
                    By formBy = By.cssSelector("div#reklamodawca_oferta_spons_top_cont");
                    wait.until(ExpectedConditions.presenceOfElementLocated(formBy));
                    String pageSource = driver.getPageSource();
                    Document document = Jsoup.parse(pageSource);

                    getInfo(document, bufferedWriter, url);

                    By closeBy = By.cssSelector("div#cboxContent div#cboxClose");
                    wait.until(ExpectedConditions.presenceOfElementLocated(closeBy));
                    try {
                        Thread.sleep(RND.nextInt(500) + 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    driver.findElement(closeBy).click();
                }
                page++;
                By nextBy = By.cssSelector("span.pagination a.next");
                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(nextBy));
                } catch (Exception e) {
                    System.out.println("ERROR on Page " + 1);
                    break;
                }
                System.out.println("PAGE " + page);
                wait.until(ExpectedConditions.elementToBeClickable(nextBy));
                WebElement nextPageElm = driver.findElement(nextBy);
                nextPageElm.click();
                elements = driver.findElements(By.cssSelector("table.konto tbody tr"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getInfo(Document document, BufferedWriter bufferedWriter, String url) throws IOException {
        List<String> infoList = new ArrayList<>();
        Element formElm = document.select("form[name=reklamodawca_oferta_spons_wydawcy_form]").first();
        if (formElm != null) {
            Element firstRecord = formElm.select("table.konto tr.td_top").first();
            if (firstRecord != null) {
                //url
                Element link = null;//firstRecord.select("td:eq(0) a").first();
                if (link != null) {
                    infoList.add(link.attr("href"));
                } else {
                    if (!url.startsWith("http")) {
                        infoList.add("http://" + url.toLowerCase());
                    } else {
                        infoList.add(url.toLowerCase());
                    }
                }
                //days
                Element daysElm = firstRecord.select("td:eq(2)").first();
                if (daysElm != null) {
                    infoList.add(daysElm.ownText().trim());
                } else {
                    infoList.add("ERROR");
                }

                //price
                Element priceELm = firstRecord.select("td:eq(3) span").first();
                if (priceELm != null) {
                    infoList.add(priceELm.text().replaceAll("[^0-9.,]", ""));
                } else {
                    infoList.add("ERROR");
                }

                //status
                Element statusElm = firstRecord.select("td:eq(4) span.status").first();
                if (statusElm != null) {
                    if (statusElm.attr("class").contains("status2")) {
                        infoList.add("X");
                    } else {
                        infoList.add("V");
                    }
                } else {
                    infoList.add("ERROR");
                }
                //designation
                Element designationElm = firstRecord.select("td:eq(5) span").first();
                if (designationElm != null) {
                    infoList.add(designationElm.text().trim());
                } else {
                    infoList.add("ERROR");
                }
            }
        }
        if (infoList.size() == 5) {
            String info = getStringInfo(infoList);
            bufferedWriter.write(info + "\n");
            System.out.println(info);
            bufferedWriter.flush();
        } else {
            System.out.println("ERROR");
            bufferedWriter.write("ERROR\n");
        }

    }

    private String getStringInfo(List<String> infoList) {
        StringBuilder infoSb = new StringBuilder();
        for (String i : infoList) {
            infoSb.append(",").append("\"").append(i).append("\"");
        }
        return infoSb.toString().replaceFirst(",", "");
    }
}
