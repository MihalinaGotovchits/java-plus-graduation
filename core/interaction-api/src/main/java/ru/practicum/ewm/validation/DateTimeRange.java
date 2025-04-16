package ru.practicum.ewm.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Constraint(validatedBy = DateTimeRangeValidator.class)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DateTimeRange {
    String message() default "Start date should be less than end date";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String before();

    String after();
}