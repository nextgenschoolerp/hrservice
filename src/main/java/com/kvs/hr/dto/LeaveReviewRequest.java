package com.kvs.hr.dto;

import com.kvs.hr.enums.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveReviewRequest {

    @NotNull(message = "Review status decision is required (APPROVED/REJECTED)")
    private LeaveStatus status;

    private String reviewComments;
}
