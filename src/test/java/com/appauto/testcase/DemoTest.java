package com.appauto.testcase;

import com.appauto.base.BaseTest;
import com.appauto.base.TestConfig;
import com.appauto.page.HomePage;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

@Feature("BYR导航")
public class DemoTest extends BaseTest {

    @Test(description = "点击BYR图标进首页-点击发现-左滑退出")
    @Story("完整流程")
    public void testBytFlow() {
        String byrPackage = TestConfig.get("byr.package");

        // 0. 先回到桌面
        pressHome();
        sleep(500);

        // 1. 点击桌面 BYR 图标，进入首页
        waitAndClick(HomePage.byrIcon);
        waitAppLaunched(byrPackage);   // 轮询等 app 启动（超时即失败）
        saveScreenshot("1_home_page.png");

        // 2. 点击「发现」进入发现页
        tapCoord(HomePage.DISCOVER_X, HomePage.DISCOVER_Y);
        sleep(1500);   // 发现页切换，Flutter 无元素可等，保留短等待
        Assert.assertEquals(driver.getCurrentPackage(), byrPackage, "点击发现后离开了 app");
        saveScreenshot("2_discover_page.png");

        // 3. 从屏幕左边缘往右滑，退出到手机默认页面（桌面）
        swipeFromLeftEdge();
        waitAppClosed(byrPackage);   // 轮询等 app 退出（超时即失败）
        saveScreenshot("3_back_to_launcher.png");
    }
}
