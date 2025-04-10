package ru.practicum.dto.location;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LocationDto {

    @Min(-90)
    @Max(90)
    @NotNull
    private Double lat;

    @Min(-180)
    @Max(180)
    @NotNull
    private Double lon;
}
