package channy.shu;

import com.google.common.collect.Maps;
import io.netty.util.internal.StringUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Main {


    public static void main(String[] args) throws InterruptedException {
        Param param = readParam();
        if(StringUtil.isNullOrEmpty(param.getPwd()) || StringUtil.isNullOrEmpty(param.getUsername())){
            System.out.println("no valid password or username");
            System.exit(0);
        }
        if(param.getMode()==null){
            param.setMode(MODE_TOMORROW);
        }
        WebDriver driver = new ChromeDriver();
        login(driver,param);
        //已登陆
       fetch(driver,param);
    }

    private static void fetch(WebDriver driver, Param param) throws InterruptedException {
        driver.get("https://bawtt.ydmap.cn/booking/schedule/104005?salesItemId=103168");
        Thread.sleep(2000);
        WebElement list = driver.findElement(By.xpath("//*[@id=\"app\"]/div/div/section/div/div[2]/div[2]/div/ul"));
        if (list == null) {
            throw new RuntimeException("list is null");
        }
        List<WebElement> li = list.findElements(By.tagName("li"));
        boolean succ = false;
        LocalDateTime fetchStart = LocalDate.now().atTime(8, 0);
        while(!succ){
            LocalDateTime now = LocalDateTime.now();
            while (now.isBefore(fetchStart)){
                now = LocalDateTime.now();
                long ms = Duration.between(now, fetchStart).toMillis();
                long wait = ms / 1000;
                System.out.println("距离八点还有"+wait+"秒,等待中");
                if(ms>10000){
                    Thread.sleep(10000);
                }else{
                    Thread.sleep(ms+300);
                }
            }
            try {
                if (Objects.equals(param.getMode(), MODE_TODAY)) {
                    li.get(0).click();
                    Thread.sleep(5000);
                    succ = fetchPlate(driver);
                } else {
                    li.get(1).click();
                    Thread.sleep(1000);
                    while (!succ) {
                        succ = fetchPlate(driver);
                        System.out.println("重试中");
                        if (!succ) {
                            Thread.sleep(3000);
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static final Integer MODE_TODAY=1;
    public static final Integer MODE_TOMORROW=2;
    private static Param readParam() {
        try {
            Param p =new Param();
            FileInputStream fis = new FileInputStream("info.txt");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fis));
            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                if(str.contains("username")) {
                    p.setUsername(str.split("=")[1]);
                }else if(str.contains("pwd")) {
                    p.setPwd(str.split("=")[1]);
                } else if (str.contains("mode")){
                    String[] split = str.split("=");
                    String code = split[1];
                    if(code.equalsIgnoreCase("today")){
                        p.setMode(MODE_TODAY);
                    }else{
                        p.setMode(MODE_TOMORROW);
                    }
                }
            }
            return p;
        }catch (Exception e){
            System.out.println("no valid info");
            System.exit(0);
        }
        return null;
    }
    public static final String AIM1="20:00-21:00";
    public static final String AIM2="21:00-22:00";

    private static boolean fetchPlate(WebDriver driver){
        boolean success = false;
        Map<String,String> headerMap = Maps.newHashMap();
        List<WebElement> headers = driver.findElement(By.xpath("//*[@id=\"app\"]/div/div/section/div/div[3]/div/div[2]/table")).findElements(By.tagName("th"));
        for (WebElement header : headers) {
            headerMap.put(header.getAttribute("data-platform-id"),header.getAccessibleName());
        }

        //获取到table
        WebElement table = driver.findElement(By.xpath("//*[@id=\"app\"]/div/div/section/div/div[3]/div/div[3]/table/tbody"));
        if (table == null) {
            throw new RuntimeException("table is null");
        }
        List<WebElement> tds = table.findElements(By.tagName("td"));
        Map<String,WebElement> td8Map = Maps.newHashMap();
        Map<String,WebElement> td9Map = Maps.newHashMap();
        for (WebElement td : tds) {
            String status = td.getAttribute("class");
            if (status.contains("col-completed") || status.contains("col-inprocess")) {
                continue;
            }
            String text = td.getText();
            if(text.contains(AIM1)){
                td8Map.put(td.getAttribute("data-platform-id"),td);
            }else if(text.contains(AIM2)){
                td9Map.put(td.getAttribute("data-platform-id"),td);
            }
        }
        if(td8Map.isEmpty() && td9Map.isEmpty()){
            return false;
        }else if(!td9Map.isEmpty()&&!td8Map.isEmpty()){
            for (Map.Entry<String, WebElement> elementEntry : td8Map.entrySet()) {
                String key = elementEntry.getKey();
                WebElement value = elementEntry.getValue();
                if(td9Map.containsKey(key)){
                    value.click();
                    WebElement webElement = td9Map.get(key);
                    webElement.click();
                    success = true;
                    String th = headerMap.get(key);
                    System.out.println("选中场地:"+th+",时间:"+value.getText()+"-"+webElement.getText());
                    break;
                }
            }
        }else if(td8Map.isEmpty()){
            for (Map.Entry<String, WebElement> random : td9Map.entrySet()) {
                random.getValue().click();
                System.out.println("选中场地:"+headerMap.get(random.getKey())+",时间:"+random.getValue().getText());
                success = true;
                break;
            }
        }else {
            for (Map.Entry<String, WebElement> random : td8Map.entrySet()) {
                random.getValue().click();
                System.out.println("选中场地:"+headerMap.get(random.getKey())+",时间:"+random.getValue().getText());
                success = true;
                break;
            }
        }
        if(!success){
            return false;
        }
        try {
            List<WebElement> buttons = driver.findElements(By.tagName("button"));
            for (WebElement button : buttons) {
                if (button.getAttribute("class").contains("primary-button")) {
                    button.click();
                    break;
                }
            }
            try {
                //这里可能有最近两个小时的提示
                WebElement element = driver.findElement(By.className("el-message-box"));
                if (element != null) {
                    buttons = element.findElement(By.className("el-message-box__btns")).findElements(By.tagName("button"));
                    for(WebElement button : buttons) {
                        if (button.getAttribute("class").contains("primary")) {
                            button.click();
                            break;
                        }
                    }
                }
            }catch (Exception e){

            }
            Thread.sleep(1000);
            WebElement message = driver.findElement(By.className("el-message-box__message"));
            if (message == null) {
                throw new RuntimeException("message box is null");
            }
            String js = "document.getElementsByClassName(\"el-message-box__message\")[0].scrollTop=10000";
            JavascriptExecutor jse = (JavascriptExecutor) driver;
            jse.executeScript(js);
            buttons = driver.findElement(By.className("el-message-box")).findElements(By.tagName("button"));
            for (WebElement button : buttons) {
                if (button.getAttribute("class").contains("primary")) {
                    while (true) {
                        try {
                            Thread.sleep(100);
                            button.click();
                        }catch (Exception e){
                            break;
                        }
                    }
                }
            }
            buttons = driver.findElements(By.tagName("button"));
            for (WebElement button : buttons) {
                if (button.getAttribute("class").contains("primary")) {
                    button.click();
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void login(WebDriver driver, Param param) throws InterruptedException {
        driver.get("https://bawtt.ydmap.cn/page.shtml?id=101456");
        Thread.sleep(2000);
        driver.findElement(By.id("foot_icon_3")).click();
        Thread.sleep(1000);
        WebElement phone = driver.findElement(By.xpath("//input[@class='el-input__inner' and @type='number']"));
        WebElement pwd = driver.findElement(By.xpath("//input[@class='el-input__inner' and @type='password']"));
        if (phone != null && pwd != null) {
            //need login
            phone.sendKeys(param.getUsername());
            pwd.sendKeys(param.getPwd());
            Thread.sleep(2000);
            List<WebElement> buttons = driver.findElements(By.tagName("button"));
            for (WebElement button : buttons) {
                if (button.getAttribute("class").contains("primary")) {
                    button.click();
                    //等验证码
                    Thread.sleep(1500);
                    try {
                        while (driver.findElement(By.className("yidun_modal")) != null) {
                            Thread.sleep(1000);
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
}