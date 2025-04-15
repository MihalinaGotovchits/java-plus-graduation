package ru.practicum.ewm.location.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.core.validation.NullOrNotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewLocationDto {
    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double lat;
    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double lon;
    @NullOrNotBlank
    @Size(min = 3, max = 255)
    private String name;
    @NullOrNotBlank
    @Size(min = 3, max = 1000)
    private String address;
}