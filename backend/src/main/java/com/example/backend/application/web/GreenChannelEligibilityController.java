package com.example.backend.application.web;

import com.example.backend.application.dto.GreenChannelEligibility;
import com.example.backend.application.service.GreenChannelEligibilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Temporary development identity header is replaced by member one's trusted session context later. */
@RestController
@RequestMapping("/api/applications/green-channel")
public class GreenChannelEligibilityController {
    private final GreenChannelEligibilityService eligibility;

    public GreenChannelEligibilityController(GreenChannelEligibilityService eligibility) {
        this.eligibility = eligibility;
    }

    @GetMapping("/eligibility")
    public GreenChannelEligibility check(@RequestHeader("X-Student-Id") Long studentId) {
        return eligibility.check(studentId);
    }
}
