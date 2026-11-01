package com.shopsphere.dto.request.review;

import com.shopsphere.entity.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewModerationRequest {

    @NotNull(message = "Review status is required")
    private ReviewStatus status;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
