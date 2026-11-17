package com.medicare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the MediCare Hospital Management System.
 *
 * @SpringBootApplication is a convenience annotation that combines three things:
 *   1. @Configuration      -> this class can define Spring beans
 *   2. @EnableAutoConfiguration -> Spring Boot guesses and configures beans
 *                                  based on the dependencies on the classpath
 *                                  (e.g. it sees spring-boot-starter-web and
 *                                  automatically configures an embedded Tomcat server)
 *   3. @ComponentScan      -> Spring scans this package and all sub-packages
 *                                  (controller, service, repository, etc.) for
 *                                  classes annotated with @Component, @Service,
 *                                  @Repository, @Controller/@RestController, etc.,
 *                                  and registers them as "beans" it manages.
 */
@SpringBootApplication
public class MedicareApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicareApplication.class, args);
    }

}
