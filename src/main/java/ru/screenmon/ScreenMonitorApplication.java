package ru.screenmon;

import nu.pattern.OpenCV;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class ScreenMonitorApplication {

    @PostConstruct
    public void loadOpenCV() {
        if (getJavaVersion() >= 12) {
            OpenCV.loadLocally();
        } else {
            OpenCV.loadShared();
        }
    }

    private static int getJavaVersion() {
        String v = System.getProperty("java.specification.version", "1.8");
        if (v.startsWith("1.")) {
            return Integer.parseInt(v.substring(2));
        }
        return Integer.parseInt(v);
    }

    public static void main(String[] args) {
        SpringApplication.run(ScreenMonitorApplication.class, args);
    }
}
