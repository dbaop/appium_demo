package com.appauto.base;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 读取 classpath 下的 config.properties，把设备、坐标、包名等抽到配置文件。
 */
public class TestConfig {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = TestConfig.class.getResourceAsStream("/config.properties")) {
            if (in == null) {
                throw new IllegalStateException("classpath 下找不到 config.properties");
            }
            Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            PROPS.load(reader);
        } catch (Exception e) {
            throw new RuntimeException("加载 config.properties 失败: " + e.getMessage(), e);
        }
    }

    public static String get(String key) {
        return PROPS.getProperty(key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(PROPS.getProperty(key).trim());
    }
}
