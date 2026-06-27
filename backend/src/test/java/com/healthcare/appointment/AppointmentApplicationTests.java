package com.healthcare.appointment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5433/healthcare",
    "spring.datasource.username=healthuser",
    "spring.datasource.password=healthpass",
    "spring.rabbitmq.port=5673"
})
class AppointmentApplicationTests {
    @Test
    void contextLoads() {
    }
}
