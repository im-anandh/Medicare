package com.medicare.config;

import com.medicare.security.CustomUserDetailsService;
import com.medicare.security.JwtAuthenticationFilter;
import com.medicare.security.RestAccessDeniedHandler;
import com.medicare.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * This class is the central rulebook for the whole application's
 * security. @EnableWebSecurity turns on Spring Security's web support
 * and lets us customize it via the SecurityFilterChain bean below.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                           CustomUserDetailsService userDetailsService,
                           RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                           RestAccessDeniedHandler restAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
    }

    /**
     * BCryptPasswordEncoder implements a one-way hashing algorithm
     * specifically designed for passwords - it's slow ON PURPOSE
     * (unlike something fast like plain SHA-256), which makes
     * brute-force password guessing attacks impractically slow, even
     * if a database ever leaked. We NEVER store or compare raw
     * passwords anywhere in this codebase - only encoder.encode(...)
     * output (a hash), and later encoder.matches(raw, hash) to verify
     * a login attempt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager is the Spring Security component that
     * actually performs the "check this username/password" step
     * during login. We wire it here to use our
     * CustomUserDetailsService (to find the user) and our
     * passwordEncoder (to verify the password hash matches).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * This is where we define exactly which endpoints require
     * authentication, which require a specific ROLE, and which are
     * fully public.
     *
     * .csrf(csrf -> csrf.disable())
     *   CSRF protection defends against a specific attack relevant to
     *   browser-based, COOKIE-authenticated apps. Our API uses JWT
     *   tokens sent explicitly in headers instead of cookies, so this
     *   protection isn't relevant here and would only get in the way
     *   of tools like Postman.
     *
     * .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
     *   Tells Spring Security "never create or use an HTTP session."
     *   This is the server-side half of what makes JWT auth
     *   stateless - we never store any "logged in" state in server
     *   memory; every request must prove who it is via its own token.
     *
     * .requestMatchers(...).permitAll() vs .hasRole(...) vs
     * .authenticated()
     *   Defines, path by path, exactly who's allowed in. Rules are
     *   checked TOP TO BOTTOM, and the first matching rule wins - so
     *   order matters. We list the most specific/permissive rules
     *   first, and end with a catch-all requiring plain
     *   authentication for anything not explicitly called out.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Anyone can log in or (for patients only - see
                        // AuthServiceImpl) self-register without being logged in.
                        .requestMatchers("/api/auth/**").permitAll()
                        // The health check stays public too.
                        .requestMatchers("/api/health").permitAll()

                        // The web app itself (the built React files in
                        // src/main/resources/static) must be loadable BEFORE
                        // anyone has logged in - otherwise the login page could
                        // never appear. This is only the empty application
                        // shell: every byte of real data still comes from
                        // /api/**, which stays fully protected below.
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.svg", "/assets/**").permitAll()

                        // Only ADMIN can manage departments and doctors' core
                        // records (create/update/deactivate) - matches the
                        // ADMIN permissions list from the project spec.
                        .requestMatchers(HttpMethod.POST, "/api/departments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/departments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/departments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/doctors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/doctors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/doctors/**").hasRole("ADMIN")
                        // PATCH /api/doctors/{id}/availability. Without this rule
                        // it fell through to the "any logged-in user" catch-all,
                        // so even a PATIENT could change a doctor's availability.
                        // ADMIN, or a DOCTOR for their own record (the service
                        // checks "their own").
                        .requestMatchers(HttpMethod.PATCH, "/api/doctors/**").hasAnyRole("ADMIN", "DOCTOR")

                        // RECEPTIONIST and ADMIN can manage patients and
                        // appointments (register patients, book/cancel/
                        // reschedule appointments) - per spec.
                        .requestMatchers(HttpMethod.POST, "/api/patients/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.PUT, "/api/patients/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.DELETE, "/api/patients/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                        // Viewing appointments: ADMIN/RECEPTIONIST see
                        // everything (they manage the schedule); DOCTOR and
                        // PATIENT can view too, but the SERVICE layer
                        // (AppointmentServiceImpl) narrows what they
                        // actually see down to just their own appointments.
                        .requestMatchers(HttpMethod.GET, "/api/appointments/**")
                            .hasAnyRole("ADMIN", "RECEPTIONIST", "DOCTOR", "PATIENT")
                        // Booking/rescheduling/cancelling stays staff-only -
                        // patients don't self-serve appointments in this
                        // version of the system.
                        .requestMatchers(HttpMethod.POST, "/api/appointments/**").hasAnyRole("ADMIN", "RECEPTIONIST", "DOCTOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/appointments/**").hasAnyRole("ADMIN", "RECEPTIONIST", "DOCTOR")

                        // Only DOCTOR can create or edit clinical content -
                        // per spec, this is a doctor-only responsibility.
                        .requestMatchers(HttpMethod.POST, "/api/medical-records/**").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/medical-records/**").hasRole("DOCTOR")
                        // Viewing clinical content is deliberately NOT part
                        // of the "any authenticated user" catch-all below -
                        // medical notes are more sensitive than basic
                        // contact/appointment info, so only the doctor who
                        // treated a patient, or the patient themselves, may
                        // read them at all. ADMIN and RECEPTIONIST are
                        // excluded here on purpose, matching your
                        // requirement that even admins shouldn't see deep
                        // medical detail.
                        .requestMatchers(HttpMethod.GET, "/api/medical-records/**").hasAnyRole("DOCTOR", "PATIENT")

                        // Prescriptions are clinical content too, so they
                        // follow the exact same model as medical records:
                        // only DOCTOR writes, only DOCTOR + PATIENT read
                        // (the service narrows both down to "their own"),
                        // and ADMIN/RECEPTIONIST are shut out entirely.
                        // Rule ORDER matters in Spring Security - the first
                        // matching rule wins - so these must stay ABOVE the
                        // anyRequest() catch-all below.
                        .requestMatchers(HttpMethod.POST, "/api/prescriptions/**").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/prescriptions/**").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/prescriptions/**").hasAnyRole("DOCTOR", "PATIENT")

                        // Billing is a front-desk/finance concern: ADMIN and
                        // RECEPTIONIST manage it, a PATIENT may read their own
                        // bills (the service narrows that down), and DOCTOR
                        // gets nothing. There is no DELETE rule because there
                        // is no delete endpoint - a wrong bill is CANCELLED.
                        .requestMatchers(HttpMethod.POST, "/api/bills/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.PUT, "/api/bills/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.PATCH, "/api/bills/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.GET, "/api/bills/**").hasAnyRole("ADMIN", "RECEPTIONIST", "PATIENT")

                        // Login accounts. The ADMIN manages all of them;
                        // a RECEPTIONIST may only create PATIENT logins (the
                        // service enforces that part). This is now the only
                        // way to create ADMIN / DOCTOR / RECEPTIONIST logins.
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/**").hasRole("ADMIN")

                        // The SMS delivery log is operational data for staff
                        // (ADMIN, RECEPTIONIST). Triggering a reminder run by
                        // hand is ADMIN-only. Doctors and patients get 403.
                        .requestMatchers(HttpMethod.POST, "/api/notifications/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/notifications/**").hasAnyRole("ADMIN", "RECEPTIONIST")

                        // Everything else just needs SOME valid, logged-in
                        // user - e.g. viewing (GET) patients/doctors/
                        // departments is allowed for any authenticated role.
                        .anyRequest().authenticated()
                )
                // Insert our custom JWT filter BEFORE Spring Security's
                // built-in username/password filter, so our token check
                // runs first on every request.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
