package com.fintech.audit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditVerificationResponse {

    @JsonProperty("valid")
    private boolean valid;

    @JsonProperty("total_records_checked")
    private long totalRecordsChecked;

    @JsonProperty("last_hash")
    private String lastHash;

    @JsonProperty("status")
    private String status; // VERIFIED_TAMPER_FREE ou CORRUPTION_DETECTED

    @JsonProperty("corrupted_record_id")
    private Long corruptedRecordId;

    @JsonProperty("details")
    private String details;

    @JsonProperty("verified_at")
    @Builder.Default
    private Instant verifiedAt = Instant.now();
}
