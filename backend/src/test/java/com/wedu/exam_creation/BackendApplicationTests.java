package com.wedu.exam_creation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "JWT_ACCESS_SECRET=VmKHS3Ne9qibwd/wEb2JrzzAF4M2Vk4xUpJOU/tHacg=",
        "JWT_REFRESH_SECRET=Pt8ctKox/C1foTSk40+E8CY5vKyOlf0FDzpICNt6dRw=",
        "AWS_ACCESS_KEY_ID=mock-access-key-id",
        "AWS_SECRET_ACCESS_KEY=mock-secret-access-key"
})
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
