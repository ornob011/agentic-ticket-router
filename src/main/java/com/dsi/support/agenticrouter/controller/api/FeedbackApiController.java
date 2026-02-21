package com.dsi.support.agenticrouter.controller.api;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import com.dsi.support.agenticrouter.service.learning.FeedbackCaptureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackApiController {

    private final FeedbackCaptureService feedbackCaptureService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT', 'SUPERVISOR', 'ADMIN') and @ticketAuthorizationService.canAccessTicket(#request.ticketId)")
    public ResponseEntity<ApiDtos.FeedbackResponse> submitFeedback(
        @Valid @RequestBody ApiDtos.FeedbackRequest request
    ) throws BindException {
        return ResponseEntity.ok(
            feedbackCaptureService.submitFeedback(request)
        );
    }

    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT', 'SUPERVISOR', 'ADMIN') and @ticketAuthorizationService.canAccessTicket(#ticketId)")
    public ResponseEntity<List<ApiDtos.FeedbackResponse>> getFeedbackForTicket(
        @PathVariable Long ticketId
    ) {
        return ResponseEntity.ok(
            feedbackCaptureService.getFeedbackForTicket(ticketId)
        );
    }

    @GetMapping("/ticket/{ticketId}/summary")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT', 'SUPERVISOR', 'ADMIN') and @ticketAuthorizationService.canAccessTicket(#ticketId)")
    public ResponseEntity<ApiDtos.FeedbackSummary> getFeedbackSummary(
        @PathVariable Long ticketId
    ) {
        return ResponseEntity.ok(
            feedbackCaptureService.getFeedbackSummaryForTicket(ticketId)
        );
    }
}
