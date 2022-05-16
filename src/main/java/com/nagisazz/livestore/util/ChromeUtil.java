package com.nagisazz.livestore.util;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.core.io.ClassPathResource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
public class ChromeUtil {

    public static synchronized String getMeta(String path, String file, Integer fileSize) {
        try {
            //参数配置
            String osName = System.getProperties().getProperty("os.name");
            if (osName.equals("Linux")) {
                System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
            } else {
                System.setProperty("webdriver.chrome.driver",
                        URLDecoder.decode(
                                new ClassPathResource("driver/chromedriver.exe").getURL().getPath(),
                                StandardCharsets.UTF_8.name()));
            }
            WebDriver driver;
            ChromeOptions option = new ChromeOptions();
            option.addArguments("no-sandbox");//禁用沙盒
            option.setHeadless(true);
            //通过ChromeOptions的setExperimentalOption方法，传下面两个参数来禁止掉谷歌受自动化控制的信息栏
            option.setExperimentalOption("useAutomationExtension", false);
            option.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
            driver = new ChromeDriver(option);
            driver.get("http://localhost:9090/live/getMeta.html?path=" + path + "&file=" + file);
            //等待页面渲染，约等于1s20MB
            int renderSecond = fileSize / 1024 / 1024 / 20 + 2;
            Thread.sleep(renderSecond * 1000);
            String html = driver.getPageSource();
            driver.close();
            // Jsoup解析处理
            Document doc = Jsoup.parse(html);
            Elements body = doc.getElementsByTag("body");
            return Jsoup.parse(body.outerHtml()).getElementsByTag("div").text();
        } catch (Exception e) {
            log.error("chrome调用失败", e);
        }
        return "";
    }
}
