package com.appauto.report;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestStep;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 简单静态 HTML 报告插件。
 * 只有运行 mvn test -Dreport=true 时才生成报告到
 * src/test/resources/reports/mobile_case_yyyyMMdd_HHmmss.html；
 * 不带该参数时本插件什么都不做（照常走 Allure）。
 */
public class SimpleReportPlugin implements ConcurrentEventListener {

    private final List<CaseResult> cases = new ArrayList<>();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseFinished.class, this::onCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::onRunFinished);
    }

    private void onCaseFinished(TestCaseFinished event) {
        TestCase tc = event.getTestCase();
        CaseResult r = new CaseResult();
        r.feature = featureName(tc.getUri().toString());
        r.name = tc.getName();
        r.status = event.getResult().getStatus().name();
        r.durationMs = event.getResult().getDuration().toMillis();
        r.steps = new ArrayList<>();
        for (TestStep ts : tc.getTestSteps()) {
            if (ts instanceof PickleStepTestStep) {
                PickleStepTestStep psts = (PickleStepTestStep) ts;
                r.steps.add(psts.getStep().getKeyword() + " " + psts.getStep().getText());
            }
        }
        cases.add(r);
    }

    private void onRunFinished(TestRunFinished event) {
        if (!"true".equalsIgnoreCase(System.getProperty("report"))) {
            return; // 没带 -Dreport=true，不生成静态报告
        }
        try {
            Path dir = Paths.get("src", "test", "resources", "reports");
            Files.createDirectories(dir);
            String filename = "mobile_case_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    + ".html";
            Path out = dir.resolve(filename);
            Files.write(out, buildHtml().getBytes(StandardCharsets.UTF_8));
            System.out.println("[SimpleReport] 静态报告已生成: " + out.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("生成静态报告失败", e);
        }
    }

    private String buildHtml() {
        long passed = cases.stream().filter(c -> "PASSED".equals(c.status)).count();
        long failed = cases.stream().filter(c -> "FAILED".equals(c.status)).count();
        long skipped = cases.size() - passed - failed;
        double rate = cases.isEmpty() ? 0 : passed * 100.0 / cases.size();

        StringBuilder caseBlocks = new StringBuilder();
        for (CaseResult c : cases) {
            String text;
            String color;
            switch (c.status) {
                case "PASSED": text = "通过"; color = "#2e7d32"; break;
                case "FAILED": text = "失败"; color = "#c62828"; break;
                case "SKIPPED": text = "跳过"; color = "#757575"; break;
                default: text = c.status; color = "#ef6c00";
            }
            StringBuilder stepsHtml = new StringBuilder();
            for (String s : c.steps) {
                stepsHtml.append("<li>").append(esc(s)).append("</li>");
            }
            caseBlocks.append("<div class='case'>")
                .append("<div class='case-header'>")
                .append("<span class='case-name'>[").append(esc(c.feature)).append("] ").append(esc(c.name)).append("</span>")
                .append("<span class='status' style='color:").append(color).append("'>").append(text).append("</span>")
                .append("<span class='duration'>").append(c.durationMs).append(" ms</span>")
                .append("</div>")
                .append("<ol class='steps'>").append(stepsHtml).append("</ol>")
                .append("</div>");
        }

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return "<!DOCTYPE html><html lang='zh-CN'><head><meta charset='UTF-8'>"
            + "<title>Appium 测试报告</title>"
            + "<style>"
            + "body{font-family:'Microsoft YaHei',sans-serif;margin:40px;color:#333}"
            + "h1{border-bottom:2px solid #1976d2;padding-bottom:10px}"
            + ".summary{background:#f9f9f9;padding:15px;border-radius:6px;font-size:15px;margin:15px 0}"
            + ".case{border:1px solid #e0e0e0;border-radius:6px;margin:12px 0;padding:12px}"
            + ".case-header{display:flex;gap:20px;align-items:center;font-size:16px}"
            + ".case-name{font-weight:bold}"
            + ".status{font-weight:bold}"
            + ".duration{color:#757575;font-size:13px}"
            + ".steps{margin:10px 0 0 20px;padding-left:20px}"
            + ".steps li{margin:4px 0;color:#555}"
            + "</style></head><body>"
            + "<h1>Appium UI 自动化测试报告</h1>"
            + "<p>生成时间：<b>" + time + "</b></p>"
            + "<div class='summary'>共 <b>" + cases.size() + "</b> 个用例，"
            + "通过 <b style='color:#2e7d32'>" + passed + "</b>，"
            + "失败 <b style='color:#c62828'>" + failed + "</b>，"
            + "跳过 <b>" + skipped + "</b>，"
            + "成功率 <b style='color:#1976d2'>" + String.format("%.1f", rate) + "%</b></div>"
            + caseBlocks
            + "</body></html>";
    }

    private static String featureName(String uri) {
        String name = uri.substring(uri.lastIndexOf('/') + 1);
        return name.endsWith(".feature") ? name.substring(0, name.length() - 8) : name;
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static class CaseResult {
        String feature;
        String name;
        String status;
        long durationMs;
        List<String> steps;
    }
}
