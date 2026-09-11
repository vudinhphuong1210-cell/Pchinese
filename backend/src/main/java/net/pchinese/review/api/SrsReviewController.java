package net.pchinese.review.api;

import jakarta.validation.Valid;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.review.api.SrsReviewDtos.DueQueueResponse;
import net.pchinese.review.api.SrsReviewDtos.SubmitReviewRequest;
import net.pchinese.review.api.SrsReviewDtos.SubmitReviewResponse;
import net.pchinese.review.application.SrsReviewService;
import net.pchinese.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/srs")
public class SrsReviewController {

    private final SrsReviewService reviewService;

    public SrsReviewController(SrsReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/due")
    public ApiEnvelope<DueQueueResponse> getDueQueue(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        DueQueueResponse response = reviewService.getDueQueue(principal.userId(), limit);
        return ApiEnvelope.success(response);
    }

    @PostMapping("/review")
    public ApiEnvelope<SubmitReviewResponse> submitReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubmitReviewRequest request
    ) {
        SubmitReviewResponse response = reviewService.submitReview(principal.userId(), request);
        return ApiEnvelope.success(response);
    }
}
