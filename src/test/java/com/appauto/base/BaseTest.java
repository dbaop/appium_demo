package com.appauto.base;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;

/**
 * 驱动 + 通用操作的基类（供 StepDefinitions 继承）。
 * 生命周期由 Cucumber 的 @Before / @After 钩子调用 initDriver() / quitDriver() 管理。
 */
public class BaseTest {
    public AndroidDriver driver;
    public WebDriverWait wait;

    public void initDriver() throws Exception {
        // 前提：先手动启动 Appium（cmd 里执行 appium），保持 4723 端口监听。
        // 不设置 appPackage/appActivity，因为流程是「点击桌面 BYR 图标」进入 app。
        UiAutomator2Options options = new UiAutomator2Options();
        options.setDeviceName(TestConfig.get("device.udid"));
        options.setUdid(TestConfig.get("device.udid"));
        options.setPlatformVersion(TestConfig.get("platform.version"));
        options.setAutomationName("UiAutomator2");
        options.setDisableSuppressAccessibilityService(true);

        driver = new AndroidDriver(new URL(TestConfig.get("appium.url")), options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    public void quitDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    // 回到桌面（Home 键 keycode=3）
    public void pressHome() {
        driver.executeScript("mobile: pressKey", Map.of("keycode", 3));
    }

    // 坐标点击：Flutter 等不暴露元素树的应用用这个
    public void tapCoord(int x, int y) {
        driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
    }

    // 从屏幕左边缘往右快速滑（iOS 式返回/退出手势）。
    // Flutter 应用不响应 Appium 的 UiAutomation 手势，只认系统级 input 注入，所以用 adb 直接执行。
    public void swipeFromLeftEdge() {
        try {
            new ProcessBuilder("adb", "-s", TestConfig.get("device.udid"),
                    "shell", "input", "swipe", "5", "1332", "1100", "1332", "100")
                    .redirectErrorStream(true)
                    .start()
                    .waitFor();
        } catch (Exception e) {
            throw new RuntimeException("左滑退出失败: " + e.getMessage(), e);
        }
    }

    // 轮询等待 app 启动（前台包名变为 pkg），超时抛异常，等价于断言
    public void waitAppLaunched(String pkg) {
        waitPackageState(pkg, true);
    }

    // 轮询等待 app 退出（前台包名不再是 pkg），超时抛异常，等价于断言
    public void waitAppClosed(String pkg) {
        waitPackageState(pkg, false);
    }

    private void waitPackageState(String pkg, boolean expectEqual) {
        long deadline = System.currentTimeMillis() + 15000;
        String current = null;
        while (System.currentTimeMillis() < deadline) {
            current = driver.getCurrentPackage();
            if (expectEqual == pkg.equals(current)) {
                return;
            }
            sleep(500);
        }
        throw new AssertionError("等待超时: 前台包名应" + (expectEqual ? "为 " : "不是 ") + pkg + "，实际=" + current);
    }

    // 简单等待（Flutter 无元素树可等时，用固定时长兜底）
    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 截图保存到 target/ 下
    public void saveScreenshot(String filename) {
        try {
            File shot = driver.getScreenshotAs(OutputType.FILE);
            Files.copy(shot.toPath(), Path.of("target", filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("截图失败: " + e.getMessage(), e);
        }
    }

    // 显式等待并点击（用于桌面等原生界面）
    public void waitAndClick(By by) {
        wait.until(ExpectedConditions.elementToBeClickable(by)).click();
    }
}
