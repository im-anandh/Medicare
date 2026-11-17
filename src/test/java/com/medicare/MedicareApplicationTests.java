package com.medicare;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * A minimal smoke test. @SpringBootTest boots the full Spring application
 * context. If contextLoads() passes, it means all our beans (controllers,
 * services, config classes, etc.) wired together correctly with no errors.
 *
 * Note: this test will try to connect to the MySQL database defined in
 * application.properties, since ddl-auto=update needs a live connection.
 * Make sure MySQL is running locally (or update your env vars) before
 * running `mvn test`.
 */
@SpringBootTest
class MedicareApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: if the application context fails to start,
        // this test fails automatically.
    }

}
