package ru.practicum.ewm.dto.categories;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class NewCategoryDto {
    @Length(min = 1, max = 50, message = "Название категории должно быть от 1 до 50 символов")
    @NotBlank(message = "Название категории не может быть пустым")
    private String name;
}