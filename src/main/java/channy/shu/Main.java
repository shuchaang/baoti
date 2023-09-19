package channy.shu;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.safari.SafariDriver;

import java.util.Collections;
import java.util.List;

public class Main {

    public static final String BAOTI_YUMAOQIU_URL = "https://bawtt.ydmap.cn/booking/schedule/104005?salesItemId=103168";

    public static void main(String[] args) throws InterruptedException {
        WebDriver driver = new ChromeDriver();
        driver.get("https://bawtt.ydmap.cn/page.shtml?id=101456");
        Thread.sleep(2000);
        driver.findElement(By.id("foot_icon_3")).click();
        Thread.sleep(1000);
        WebElement phone = driver.findElement(By.xpath("//input[@class='el-input__inner' and @type='number']"));
        WebElement pwd = driver.findElement(By.xpath("//input[@class='el-input__inner' and @type='password']"));
        if (phone != null && pwd != null) {
            //need login
            phone.sendKeys("18126355089");
            pwd.sendKeys("sc123456");
            Thread.sleep(2000);
            List<WebElement> buttons = driver.findElements(By.tagName("button"));
            for (WebElement button : buttons) {
                if (button.getAttribute("class").contains("primary")) {
                    button.click();
                    //等验证码
                    Thread.sleep(1500);
                    try{
                        while(driver.findElement(By.className("yidun_modal"))!=null){
                            Thread.sleep(1000);
                        }
                    }catch (Exception e){}
                }
            }
        }
        //已登陆
        driver.get("https://bawtt.ydmap.cn/booking/schedule/104005?salesItemId=103168");
        Thread.sleep(2000);
        WebElement list = driver.findElement(By.xpath("//*[@id=\"app\"]/div/div/section/div/div[2]/div[2]/div/ul"));
        if (list == null) {
            throw new RuntimeException("list is null");
        }
        List<WebElement> li = list.findElements(By.tagName("li"));
        li.get(li.size() - 1).click();
        Thread.sleep(1000);
        //获取到table
        WebElement table = driver.findElement(By.xpath("//*[@id=\"app\"]/div/div/section/div/div[3]/div/div[3]/table/tbody"));
        if (table == null) {
            throw new RuntimeException("table is null");
        }
        //只要最后两行
        List<WebElement> trs = table.findElements(By.tagName("tr"));
        WebElement tr8 = trs.get(0);
        WebElement tr9 = trs.get(trs.size() - 1);
        List<WebElement> td8s = tr8.findElements(By.tagName("td"));
        List<WebElement> td9s = tr8.findElements(By.tagName("td"));
        //找中间的场地
        int mid = td8s.size() / 2;
        for (int i = mid; i < td8s.size(); i++) {
            WebElement td8 = td8s.get(i);
            String aClass = td8.getAttribute("class");
            if (!aClass.contains("col-completed")&&
                    !aClass.contains("col-inprocess")) {
                td8.click();
                break;
//                    WebElement td9=td9s.get(i);
//                    if(!td9.getAttribute("class").contains("col-completed")){
//                        td8.click();
//                        td9.click();
//                        break;
//                    }
            }
        }
        List<WebElement> buttons = driver.findElements(By.tagName("button"));
        for (WebElement button : buttons) {
            if(button.getAttribute("class").contains("primary-button")){
                button.click();
                break;
            }
        }
        Thread.sleep(1000);
        WebElement message = driver.findElement(By.className("el-message-box__message"));
        if(message==null){
            throw new RuntimeException("message box is null");
        }
        String js = "document.getElementsByClassName(\"el-message-box__message\")[0].scrollTop=10000";
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript(js);
        buttons = driver.findElement(By.className("el-message-box")).findElements(By.tagName("button"));
        try {
            for (WebElement button : buttons) {
                if (button.getAttribute("class").contains("primary")) {
                    while(true){
                        Thread.sleep(100);
                        button.click();
                    }
                }
            }
        }catch (Exception e){}
        buttons = driver.findElements(By.tagName("button"));
        for (WebElement button : buttons) {
            if(button.getAttribute("class").contains("primary")){
                button.click();
                break;
            }
        }
    }
}