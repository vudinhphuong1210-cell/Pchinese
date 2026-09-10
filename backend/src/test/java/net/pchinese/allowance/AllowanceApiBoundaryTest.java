package net.pchinese.allowance;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllowanceApiBoundaryTest {

    @Test
    void noPublicControllerExposesAllowanceMutationEndpoints() throws Exception {
        Path controllerDir = Paths.get("src/main/java/net/pchinese");
        assertTrue(Files.exists(controllerDir), "Source directory must exist");

        List<String> controllerFiles;
        try (Stream<Path> stream = Files.walk(controllerDir)) {
            controllerFiles = stream
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(path -> path.endsWith("Controller.java"))
                    .collect(Collectors.toList());
        }

        for (String filePath : controllerFiles) {
            String content = Files.readString(Paths.get(filePath));
            assertFalse(content.contains("/allowance/reserve"), "Public controller must not expose allowance reserve endpoint: " + filePath);
            assertFalse(content.contains("/allowance/refund"), "Public controller must not expose allowance refund endpoint: " + filePath);
            assertFalse(content.contains("/allowance/succeed"), "Public controller must not expose allowance succeed endpoint: " + filePath);
            assertFalse(content.contains("AiAllowanceService"), "Public controller must not directly inject AiAllowanceService: " + filePath);
        }
    }
}
