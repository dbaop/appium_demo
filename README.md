# Appium UI 自动化测试框架（BYR App Demo · Cucumber BDD）

基于 **Cucumber (BDD) + Appium + Java + TestNG + Allure** 的安卓真机 UI 自动化框架。被测应用是 BYR（北邮人论坛，`com.byr.bbs_flutter`，**Flutter 应用**）。

采用 **行为驱动开发（BDD）**：测试场景用 Gherkin（自然语言）写在 `.feature` 文件里，Step Definitions 把每一步映射到底层 Appium 操作。

**核心场景**：点击桌面 BYR 图标 → 进入首页 → 点击「发现」→ 进入发现页 → 从左边缘右滑退出到桌面。

## 技术栈 / 版本

| 组件 | 版本 | 说明 |
|---|---|---|
| Appium | 3.7.0 | 服务端 |
| uiautomator2 驱动 | 8.7.0 | 安卓自动化驱动 |
| java-client | 8.5.1 | Appium Java 客户端 |
| Selenium | **4.13.0（锁定）** | 不能升，见坑 #3 |
| Cucumber | 7.15.0 | BDD 框架（cucumber-java + cucumber-testng） |
| TestNG | 7.8.0 | Cucumber 的底层运行器 |
| Allure | 2.24.0 | 报告（适配器 `allure-cucumber7-jvm`） |

## 项目结构

```
src/test/
├── java/com/appauto/
│   ├── runner/RunCucumberTest.java   # Cucumber 运行器（@CucumberOptions）
│   ├── steps/StepDefinitions.java    # 步骤定义：Gherkin → 代码 + 生命周期钩子
│   ├── base/BaseTest.java            # 驱动 + 通用操作（点击/滑动/轮询等待/截图）
│   ├── base/TestConfig.java          # 读取 config.properties
│   └── page/HomePage.java            # 元素/坐标定位
└── resources/
    ├── features/byr.feature          # Gherkin 场景（业务可读）
    └── config.properties             # 配置（设备 UDID、坐标、包名等）
```

## BDD 三层对应关系

| 层 | 文件 | 示例 |
|---|---|---|
| 场景（业务语言） | `features/byr.feature` | `当我 点击桌面上的 BYR 图标` |
| 步骤定义（胶水代码） | `steps/StepDefinitions.java` | `@When("点击桌面上的 BYR 图标")` |
| 底层操作（复用） | `base/BaseTest.java` | `waitAndClick(byrIcon)` |

---

# 一、踩过的坑（从搭建到现在）

## 坑 1：`ANDROID_HOME` 环境变量未设置

**报错**：
```
Neither ANDROID_HOME nor ANDROID_SDK_ROOT environment variable was exported.
```

**原因**：Appium 的 uiautomator2 驱动要用 adb 连接手机、用 apksigner 给要装到手机上的 Appium 服务端 APK 签名，找不到 Android SDK。

**解决**：
1. 装完整 Android SDK（`platform-tools` + `build-tools` + `cmdline-tools`），缺 `build-tools` 会继续报 `apksigner` 找不到
2. 设环境变量（系统级）：`ANDROID_HOME`、`ANDROID_SDK_ROOT` 都指向 SDK 根目录，`JAVA_HOME` 指向 JDK
3. **重启 Appium 服务进程**（环境变量是进程启动时读入的，已运行的进程不更新）

> 设完变量后，如果 Appium 还报同样的错，先确认「正在跑的那个 Appium」是不是在设变量之前启动的——杀掉重开即可。

## 坑 2：`INSTALL_FAILED_ABORTED: User rejected permissions`

**报错**：装 Appium 服务端 APK 时报 `Failure [INSTALL_FAILED_ABORTED: User rejected permissions]`。

**原因**：华为（EMUI/HarmonyOS）手机默认禁止「通过 USB 安装应用」，且 adb 装包时手机会弹确认框。

**解决**：
1. 开发者选项里打开 **「USB 安装」**（有的叫「通过 USB 安装应用」）
2. 装包时**盯着手机屏幕**，弹「是否安装」时点「允许/继续」

## 坑 3：Selenium 版本冲突（编译错 + 运行时错）

`java-client 8.5.1` 的 pom 里 Selenium 依赖写的是区间 `[4.9.1, 5.0)`，会解析到本机最新的 Selenium（如 4.49.0），导致：

- **编译错**：`找不到 org.openqa.selenium.ContextAware / html5.LocationContext`（新版 Selenium 移除了这些接口）
- **运行时错**：`NoSuchMethodError: ClientConfig.<init>(...)`（`ClientConfig` 构造器签名变了）

**解决**：在 `pom.xml` 用 `dependencyManagement` 把 Selenium 锁到 **4.13.0**：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-api</artifactId>
            <version>4.13.0</version>
        </dependency>
        <!-- selenium-remote-driver、selenium-support 同样锁 4.13.0 -->
    </dependencies>
</dependencyManagement>
```

> 试过锁 4.15.0 也不行（运行时仍报 `ClientConfig` 构造器错），最终降到 4.13.0 才通。

## 坑 4：Flutter 应用不暴露元素树（content-desc 时有时无）

**现象**：Appium Inspector / `driver.findElement` 拿不到 BYR 的元素；用 `adb shell uiautomator dump` 会 OOM 被 kill（`Killed`, exit 137）。

**原因**：Flutter 应用把界面画在自绘引擎里，默认**不生成无障碍语义树**，只有检测到无障碍服务时才生成。

**解决**：
1. 加 `options.setDisableSuppressAccessibilityService(true)`（让 uiautomator2 不屏蔽无障碍服务，**部分**场景能触发语义树，但冷启动时不稳定）
2. **最终方案：坐标点击**。从页面源码里拿到的 bounds 换算坐标，用 `mobile: clickGesture` 点。见 `HomePage.java`。

## 坑 5：Flutter 应用不响应 Appium 手势（左滑退出）

**现象**：「从屏幕左边缘往右滑退出」这个手势，用 Appium 的 `dragGesture`（各种 speed）、`flingGesture`、W3C Actions 全部无效。

**原因**：Appium 手势通过 UiAutomation 注入，Flutter 不认；只有**系统级 `input` 注入**（`adb shell input swipe`）能被 Flutter 识别。

**解决**：直接在 Java 里用 `Runtime.exec` 调 adb：

```java
new ProcessBuilder("adb", "-s", udid,
        "shell", "input", "swipe", "5", "1332", "1100", "1332", "100")
        .redirectErrorStream(true).start().waitFor();
```

- 从 `x=5`（最左边缘）滑到 `x=1100`，`100ms` 快速滑动
- `100ms` 这个速度刚好，太快（如 dragGesture 的 speed=20000）会被当成点击，太慢触发不了

## 坑 6：其他小坑

- `mobile: swipeGesture` 不传元素时必须带 `left/top/width/height`，否则报 `The swipe area coordinates must be provided`
- `mobile: shell` 能执行 adb 命令，但需要 Appium 以 `--allow-insecure=adb_shell` 启动（安全敏感参数），所以没用它，改用 `Runtime.exec` 直连 adb
- 中文在控制台/日志显示成乱码（`����`）一般是**控制台编码问题**，class 文件里的字符串是正常 UTF-8，不影响运行
- Cucumber 的 `.feature` 文件第一行要写 `# language: zh-CN`，否则中文关键字（功能/场景/当/那么）解析不了

---

# 二、常用命令速查

## 启动 / 运行

```bash
# 启动 Appium 服务（保持 4723 端口监听）
appium

# 跑测试（Cucumber 场景，结果自动采集到 target/allure-results）
mvn test

# 生成 Allure HTML 报告（到 target/allure-report）
mvn allure:report

# 查看报告（起本地服务并打开浏览器，推荐）
mvn allure:serve
```

## adb 命令

```bash
adb devices                                   # 查看连接设备
adb -s <UDID> shell dumpsys window             # 查看当前前台 activity
adb -s <UDID> shell am start -n <pkg>/<act>    # 启动应用
adb -s <UDID> shell pm list packages           # 查看已装包
adb -s <UDID> shell input tap x y              # 模拟点击
adb -s <UDID> shell input swipe 5 1332 1100 1332 100   # 左边缘右滑（返回手势）
adb -s <UDID> exec-out screencap -p > screen.png       # 截图
```

## 环境变量（Windows）

```cmd
set ANDROID_HOME=D:\android-sdk
set ANDROID_SDK_ROOT=D:\android-sdk
set JAVA_HOME=D:\Program Files\Java\jdk-26.0.2.1
```

---

# 三、框架搭建完整流程

## 1. 环境准备

1. 装 **JDK**（设 `JAVA_HOME`）
2. 装 **Android SDK**（用 command line tools 的 `sdkmanager` 装 `platform-tools` + `build-tools`；或装 Android Studio），设 `ANDROID_HOME`
3. 装 **Node.js**，然后 `npm install -g appium`
4. 装 Appium 驱动：`appium driver install uiautomator2`
5. 装 **Maven**

## 2. 手机准备

1. 开「开发者选项」+「USB 调试」
2. 华为额外开「USB 安装」（见坑 #2）
3. 连电脑，`adb devices` 确认设备在列

## 3. 创建 Maven 项目（pom.xml 要点）

- 依赖：`java-client 8.5.1`、`cucumber-java 7.15.0`、`cucumber-testng 7.15.0`、`allure-cucumber7-jvm 2.24.0`
- **锁定 Selenium 4.13.0**（`dependencyManagement`，见坑 #3）
- 加 `allure-maven` 插件（`mvn allure:report` 用）
- surefire 里加 `<systemPropertyVariables><allure.results.directory>target/allure-results</allure.results.directory></systemPropertyVariables>`

## 4. 写代码（Cucumber 分层）

1. **写 `.feature` 场景**（`src/test/resources/features/`）：用 Gherkin 描述业务步骤
2. **写 Step Definitions**（`steps/`）：用 `@Given/@When/@Then` 把每句映射到代码，`@Before/@After` 管理 driver 生命周期
3. **写 Runner**（`runner/`）：`@CucumberOptions` 指定 features 目录、glue 包名、报告插件
4. **写底层操作**（`base/BaseTest`）：驱动初始化 + 通用操作（坐标点击、滑动、轮询等待、截图）
5. **写定位**（`page/HomePage`）+ **配置**（`TestConfig` + `config.properties`）

**关键点**：Flutter 应用用坐标点击 + `adb input swipe` 做手势，原生界面（桌面）用元素定位。

## 5. 运行验证

```bash
appium          # 终端 1
mvn test        # 终端 2
mvn allure:report
```

跑完看 `target/` 下的截图（`1_home_page.png`、`2_discover_page.png`、`3_back_to_launcher.png`）和 `target/allure-report/index.html` 报告（报告会按 Gherkin 的「功能/场景」组织）。
