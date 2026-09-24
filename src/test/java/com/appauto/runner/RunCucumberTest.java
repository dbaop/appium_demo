package com.appauto.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Cucumber 运行器：扫描 features 目录的 .feature 文件，执行 StepDefinitions 里的步骤。
 */
@CucumberOptions(
        features = "classpath:features",
        glue = "com.appauto.steps",
        plugin = {
                "pretty",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
        },
        monochrome = true
)
public class RunCucumberTest extends AbstractTestNGCucumberTests {
}
