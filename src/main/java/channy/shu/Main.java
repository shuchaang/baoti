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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {


    public static void main(String[] args) throws InterruptedException {
        Param param = readParam();
        if(StringUtil.isNullOrEmpty(param.getPwd())
                || StringUtil.isNullOrEmpty(param.getUsername())
                || param.getTimes() == null || param.getTimes().size() != 2){
            System.out.println("no valid info");
            System.exit(0);
        }

        WebDriver driver = new ChromeDriver();
        login(driver,param);
        //已登陆
        driver.get("https://bawtt.ydmap.cn/booking/schedule/104005?salesItemId=103168");
        Thread.sleep(2000);
        WebElement list = driver.findElement(By.xpath("//*[@id=\"app\"]/div/div/section/div/div[2]/div[2]/div/ul"));
        if (list == null) {
            throw new RuntimeException("list is null");
        }
        List<WebElement> li = list.findElements(By.tagName("li"));
        boolean succ = false;
        LocalDateTime start = LocalDate.now().atTime(8, 0,0);
        LocalDateTime end = LocalDate.now().atTime(8, 15);
        while(!succ){
            LocalDateTime now = LocalDateTime.now();
            if(now.isBefore(start)){
                long sec = Duration.between(now, start).toMillis();
                System.out.println(sec/1000+"秒后启动");
                Thread.sleep(sec+500);
            }
            if(now.isAfter(start)&&now.isBefore(end)){
                succ = fetchPlate(driver, li.get(li.size()-1),param);
            }else{
                for (WebElement webElement : li) {
                    succ = fetchPlate(driver,webElement, param);
                    Thread.sleep(3000);
                }
            }
        }
    }

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
                }else if (str.contains("times")){
                    String[] split = str.split("=");
                    String[] split1 = split[1].split("/");
                    List<Integer> t = new ArrayList<>();
                    for (String s : split1) {
                        t.add(Integer.valueOf(s));
                    }
                    p.setTimes(t);
                }
            }
            return p;
        }catch (Exception e){
            System.out.println("no valid info");
            System.exit(0);
        }
        return null;
    }

    private static boolean fetchPlate(WebDriver driver, WebElement day, Param param) throws InterruptedException {
        day.click();
        Thread.sleep(1000);
        boolean success = false;
        Map<String,String> headerMap = Maps.newHashMap();
        List<WebElement> headers = driver.findElement(By.xpath("//*[@id=\"app\"]/div/div/section/div/div[3]/div/div[2]/table/thead/tr[1]")).findElements(By.tagName("th"));
        for (WebElement header : headers) {
            headerMap.put(header.getAttribute("data-platform-id"),header.getText());
        }

        //获取到table
        WebElement table = driver.findElement(By.xpath("//*[@id=\"app\"]/div/div/section/div/div[3]/div/div[3]/table/tbody"));
        if (table == null) {
            throw new RuntimeException("table is null");
        }

        List<WebElement> trs = table.findElements(By.tagName("tr"));
        List<WebElement> td8s = trs.get(param.getTimes().get(0)-1).findElements(By.tagName("td"));
        List<WebElement> td9s = trs.get(param.getTimes().get(1)-1).findElements(By.tagName("td"));
        Map<String,WebElement> td8Map = Maps.newHashMap();
        Map<String,WebElement> td9Map = Maps.newHashMap();
        for (WebElement td8 : td8s) {
            String aClass = td8.getAttribute("class");
            if (aClass.contains("col-completed") || aClass.contains("col-inprocess")) {
                continue;
            }
            String plat = td8.getAttribute("data-platform-id");
            td8Map.put(plat,td8);
        }
        for (WebElement td9 : td9s) {
            String aClass = td9.getAttribute("class");
            if (aClass.contains("col-completed") || aClass.contains("col-inprocess")) {
                continue;
            }
            String plat = td9.getAttribute("data-platform-id");
            td9Map.put(plat,td9);
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