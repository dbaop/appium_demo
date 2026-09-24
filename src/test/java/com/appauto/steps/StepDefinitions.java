package com.appauto.steps;

import com.appauto.base.BaseTest;
import com.appauto.base.TestConfig;
import com.appauto.page.HomePage;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

public class StepDefinitions extends BaseTest {

    private final String byrPackage = TestConfig.get("byr.package");

    @Before
    public void setUp() throws Exception {
        initDriver();
    }

    @After
    public void tearDown() {
        quitDriver();
    }

    @Given("我回到手机桌面")
    public void goToHome() {
        pressHome();
        sleep(500);
    }

    @When("点击桌面上的 BYR 图标")
    public void clickByrIcon() {
        waitAndClick(HomePage.byrIcon);
    }

    @Then("应用已经进入首页")
    public void assertEnteredHome() {
        waitAppLaunched(byrPackage);   // 轮询等 app 启动，超时即失败
        saveScreenshot("1_home_page.png");
    }

    @When("点击底部发现标签")
    public void clickDiscoverTab() {
        tapCoord(HomePage.DISCOVER_X, HomePage.DISCOVER_Y);
        sleep(1500);   // 发现页切换，Flutter 无元素可等
    }

    @Then("应用进入发现页")
    public void assertInDiscoverPage() {
        Assert.assertEquals(driver.getCurrentPackage(), byrPackage, "点击发现后离开了 app");
        saveScreenshot("2_discover_page.png");
    }

    @When("从屏幕左边缘向右滑动")
    public void swipeLeftEdgeStep() {
        swipeFromLeftEdge();
    }

    @Then("应用已退出到桌面")
    public void assertExitedToLauncher() {
        waitAppClosed(byrPackage);   // 轮询等 app 退出，超时即失败
        saveScreenshot("3_back_to_launcher.png");
    }
}
