package ru.practicum.ewm.categories.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.categories.CategoryDto;
import ru.practicum.ewm.dto.categories.NewCategoryDto;
import ru.practicum.ewm.categories.service.CategoriesService;

@Validated
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@RestController
public class AdminCategoriesController {
    private final CategoriesService categoriesService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CategoryDto addCategory(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        return categoriesService.addCategory(newCategoryDto);
    }

    @PatchMapping("/{id}")
    public CategoryDto updateCategory(@PathVariable("id") Long id, @Valid @RequestBody NewCategoryDto newCategoryDto) {
        return categoriesService.updateCategory(id, newCategoryDto);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable("id") Long id) {
        categoriesService.deleteCategory(id);
    }
}