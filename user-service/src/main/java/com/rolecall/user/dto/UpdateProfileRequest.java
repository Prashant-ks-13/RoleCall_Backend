package com.rolecall.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 150) String fullName,
        @Size(max = 150) String headline,
        @Size(max = 2000) String bio,
        @Size(max = 30) String phone,
        @Size(max = 150) String location,
        @Size(max = 150) String companyName,
        @Size(max = 300) String companyWebsite
) {
}
