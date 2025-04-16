package ru.practicum.ewm.dto.location;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.validation.NullOrNotBlank;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class UpdateLocationAdminRequestDto {
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double lat;
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
