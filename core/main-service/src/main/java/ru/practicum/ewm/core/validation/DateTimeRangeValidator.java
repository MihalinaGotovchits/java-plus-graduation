package ru.practicum.ewm.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

public class DateTimeRangeValidator implements ConstraintValidator<DateTimeRange, Object> {
    private String beforeFieldName;
    private String afterFieldName;

    @Override
    public void initialize(DateTimeRange constraintAnnotation) {
        beforeFieldName = constraintAnnotation.before();
        afterFieldName = constraintAnnotation.after();
    }

    @Override
    public boolean isValid(final Object value, ConstraintValidatorContext context) {
        try {
            final Field beforeDateField = value.getClass().getDeclaredField(beforeFieldName);
            beforeDateField.setAccessible(true);

            final Field afterDateField = value.getClass().getDeclaredField(afterFieldName);
            afterDateField.setAccessible(true);

            final LocalDateTime beforeDate = (LocalDateTime) beforeDateField.get(value);
            final LocalDateTime afterDate = (LocalDateTime) afterDateField.get(value);
            return beforeDate == null || afterDate == null || beforeDate.isBefore(afterDate);

        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return false;
        }
    }
}
