package dk.rf.fleetmanager.model;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TaskRequest(
        @NotNull
        LocalDateTime startTime,
        @NotNull
        LocalDateTime endTime
) {
}
