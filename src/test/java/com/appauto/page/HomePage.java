package com.appauto.page;

import com.appauto.base.TestConfig;
import org.openqa.selenium.By;

public class HomePage {
    // 桌面上的 BYR 图标。桌面是原生应用，可以用元素定位。
    public static final By byrIcon = By.xpath("//*[@content-desc='" + TestConfig.get("byr.icon.desc") + "']");

    // 底部「发现」tab 的中心坐标（BYR 是 Flutter 应用，冷启动不暴露元素树，用坐标点击）。
    public static final int DISCOVER_X = TestConfig.getInt("discover.x");
    public static final int DISCOVER_Y = TestConfig.getInt("discover.y");
}
