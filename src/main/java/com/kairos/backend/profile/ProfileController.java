package com.kairos.backend.profile;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kairos.backend.auth.CurrentUser;
import com.kairos.backend.common.ApiException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "Athlete profile")
public class ProfileController {

    private final AthleteProfileRepository profiles;

    public ProfileController(AthleteProfileRepository profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    @Operation(summary = "The athlete's profile; 404 until one has been saved")
    @Transactional(readOnly = true)
    public ProfileDto get(@CurrentUser UUID userId) {
        return profiles.findById(userId).map(ProfileDto::of).orElseThrow(() -> ApiException.notFound("A profile"));
    }

    @PutMapping
    @Operation(summary = "Create or replace the athlete's profile")
    @Transactional
    public ProfileDto put(@CurrentUser UUID userId, @Valid @RequestBody ProfileDto body) {
        AthleteProfile profile = profiles.findById(userId)
            .orElseGet(() -> new AthleteProfile(userId, LocalDate.now(ZoneOffset.UTC)));
        profile.update(body);
        return ProfileDto.of(profiles.save(profile));
    }
}
