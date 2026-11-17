package com.medicare.service.impl;

import com.medicare.dto.AuthResponseDto;
import com.medicare.dto.LoginRequestDto;
import com.medicare.dto.RegisterRequestDto;
import com.medicare.entity.Patient;
import com.medicare.entity.Role;
import com.medicare.entity.User;
import com.medicare.exception.InvalidRequestException;
import com.medicare.repository.PatientRepository;
import com.medicare.repository.UserRepository;
import com.medicare.security.JwtUtil;
import com.medicare.service.AuthService;
import com.medicare.service.ProfileLinker;
import com.medicare.util.PhoneNumberUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    // One deliberately vague message for every identity-check failure
    // (no such patient, wrong phone, wrong date of birth), so the form
    // can't be used to probe which patient IDs exist or what is on file.
    private static final String IDENTITY_CHECK_FAILED =
            "We could not verify those details against our records. "
                    + "Please check them or ask the front desk for help.";

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final ProfileLinker profileLinker;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final String defaultCountryCode;

    public AuthServiceImpl(UserRepository userRepository,
                            PatientRepository patientRepository,
                            ProfileLinker profileLinker,
                            PasswordEncoder passwordEncoder,
                            AuthenticationManager authenticationManager,
                            JwtUtil jwtUtil,
                            @Value("${sms.default-country-code:+91}") String defaultCountryCode) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.profileLinker = profileLinker;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.defaultCountryCode = defaultCountryCode;
    }

    /**
     * PUBLIC patient self-registration (this endpoint needs no login).
     *
     * Two protections, because anyone on the internet can call it:
     *  1. Only the PATIENT role can be created here. Staff accounts
     *     (ADMIN, DOCTOR, RECEPTIONIST) are created by an administrator
     *     through POST /api/users.
     *  2. The caller must PROVE they are that patient by supplying the
     *     phone number and date of birth already on file. Without this,
     *     a stranger could register with any unclaimed patient ID and
     *     instantly gain access to that person's medical records,
     *     prescriptions and bills.
     */
    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto dto) {
        if (dto.getRole() != Role.PATIENT) {
            throw new InvalidRequestException(
                    "Public registration is only available for PATIENT accounts. "
                            + "Staff accounts are created by an administrator.");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new InvalidRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new InvalidRequestException("Email is already registered");
        }

        verifyPatientIdentity(dto);

        User user = new User();
        user.setUsername(dto.getUsername());
        // We never store dto.getPassword() - only its one-way BCrypt hash.
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setRole(Role.PATIENT);
        User saved = userRepository.save(user);

        // If linking fails (e.g. the profile already has a login), the
        // exception rolls this whole transaction back, so no orphaned
        // login is left behind.
        profileLinker.link(Role.PATIENT, dto.getProfileId(), saved);

        String token = jwtUtil.generateToken(saved.getUsername());
        return new AuthResponseDto(token, saved.getUsername(), saved.getRole());
    }

    private void verifyPatientIdentity(RegisterRequestDto dto) {
        if (dto.getProfileId() == null || dto.getPhone() == null || dto.getPhone().isBlank()
                || dto.getDateOfBirth() == null) {
            throw new InvalidRequestException(
                    "Patient ID, the phone number on file and your date of birth are all required");
        }
        Patient patient = patientRepository.findById(dto.getProfileId()).orElse(null);
        if (patient == null) {
            throw new InvalidRequestException(IDENTITY_CHECK_FAILED);
        }
        String onFile = PhoneNumberUtil.normalize(patient.getPhone(), defaultCountryCode);
        String supplied = PhoneNumberUtil.normalize(dto.getPhone(), defaultCountryCode);
        boolean phoneMatches = onFile != null && onFile.equals(supplied);
        boolean dobMatches = dto.getDateOfBirth().equals(patient.getDateOfBirth());
        if (!phoneMatches || !dobMatches) {
            throw new InvalidRequestException(IDENTITY_CHECK_FAILED);
        }
    }

    @Override
    public AuthResponseDto login(LoginRequestDto dto) {
        try {
            // This one call loads the user (CustomUserDetailsService) and
            // compares the submitted password with the stored hash. We
            // deliberately do not reveal WHICH of the two was wrong.
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidRequestException("Invalid username or password");
        }

        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new InvalidRequestException("Invalid username or password"));

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponseDto(token, user.getUsername(), user.getRole());
    }
}
