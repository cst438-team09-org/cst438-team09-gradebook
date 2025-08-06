package com.cst438.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EnterEnrollmentGradesSystemTest {

    private static final Properties localConfig = new Properties();
    static {
        try (InputStream input = EnterEnrollmentGradesSystemTest.class.getClassLoader().getResourceAsStream("test.properties")) {
            if (input == null) {
                throw new RuntimeException("test.properties not found. Copy test.properties.example and configure it.");
            }
            localConfig.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Could not load test.properties", e);
        }
    }

    // front-end UI under test
    private static final String URL = localConfig.getProperty("test.url");
    // back-end test endpoints
    private static final String BACKEND_URL = "http://localhost:" + localConfig.getProperty("server.port");

    private final RestTemplate restTemplate = new RestTemplate();

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    public void beforeEach() {
        // seed the test data
        restTemplate.postForEntity(BACKEND_URL + "/test/seed", null, Void.class);

        // start ChromeDriver and navigate to the UI
        System.setProperty("webdriver.chrome.driver", localConfig.getProperty("chrome.driver.path"));
        ChromeOptions ops = new ChromeOptions();
        ops.addArguments("--remote-allow-origins=*");
        ops.addArguments("--headless=new");
        ops.setBinary(localConfig.getProperty("chrome.binary.path"));

        driver = new ChromeDriver(ops);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(URL);
    }

    @AfterEach
    public void afterEach() {
        // reset the test data
        restTemplate.postForEntity(BACKEND_URL + "/test/reset", null, Void.class);

        // tear down browser
        if (driver != null) driver.quit();
    }

    @Test
    public void testEnterEnrollmentGrades() throws InterruptedException {
        // login as instructor
        wait.until(ExpectedConditions.elementToBeClickable(By.id("email"))).sendKeys("ted@csumb.edu");
        driver.findElement(By.id("password")).sendKeys("ted2025");
        driver.findElement(By.id("loginButton")).click();

        // select term 2025 / Fall
        wait.until(ExpectedConditions.elementToBeClickable(By.id("year"))).clear();
        driver.findElement(By.id("year")).sendKeys("2025");
        driver.findElement(By.id("semester")).clear();
        driver.findElement(By.id("semester")).sendKeys("Fall");
        driver.findElement(By.id("selectTermButton")).click();

        // find the "course id" column index dynamically
        WebElement courseHeader = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//thead/tr/th[" + "translate(normalize-space(.)," + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='course id']")));
        int courseCol = courseHeader.findElements(By.xpath("./preceding-sibling::th")).size() + 1;

        // click "Enrollments" for cst599
        WebElement enrollLink = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//tbody/tr/td[" + courseCol + "][text()='cst599']/.." + "  //a[@id='enrollmentsLink']")));
        assertNotNull(enrollLink);
        enrollLink.click();

        // locate email and grade columns
        WebElement emailHdr = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//thead/tr/th[" + "translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='email']")));
        int emailCol = emailHdr.findElements(By.xpath("./preceding-sibling::th")).size() + 1;

        WebElement gradeHdr = driver.findElement(By.xpath("//thead/tr/th[" + "translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='grade']"));
        int gradeCol = gradeHdr.findElements(By.xpath("./preceding-sibling::th")).size() + 1;

        // enter grades for students
        Map<String, String> gradesToEnter = Map.of("sama@csumb.edu", "A", "samb@csumb.edu", "B+", "samc@csumb.edu", "C");
        for (var entry : gradesToEnter.entrySet()) {
            String email = entry.getKey();
            String grade = entry.getValue();
            WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//tbody/tr[td[" + emailCol + "][text()='" + email + "']]" + "/td[" + gradeCol + "]//input")));
            input.clear();
            input.sendKeys(grade);
        }

        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Save Grades']"))).click();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("homeLink"))).click();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("year"))).clear();
        driver.findElement(By.id("year")).sendKeys("2025");
        driver.findElement(By.id("semester")).clear();
        driver.findElement(By.id("semester")).sendKeys("Fall");
        driver.findElement(By.id("selectTermButton")).click();

        enrollLink = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//tbody/tr/td[" + courseCol + "][text()='cst599']/.." + "  //a[@id='enrollmentsLink']")));
        enrollLink.click();

        for (var entry : gradesToEnter.entrySet()) {
            WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//tbody/tr[td[" + emailCol + "][text()='" + entry.getKey() + "']]" + "/td[" + gradeCol + "]//input")));
            assertEquals(entry.getValue(), input.getAttribute("value"));
        }

        // logout & login as samb to verify transcript
        wait.until(ExpectedConditions.elementToBeClickable(By.id("logoutLink"))).click();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("email"))).sendKeys("samb@csumb.edu");
        driver.findElement(By.id("password")).sendKeys("sam2025");
        driver.findElement(By.id("loginButton")).click();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("transcriptLink"))).click();

        WebElement row = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//tbody/tr[td[3][text()='cst599']]")));
        String transcriptGrade = row.findElement(By.xpath("./td[7]")).getText();
        assertEquals("B+", transcriptGrade);
    }
}