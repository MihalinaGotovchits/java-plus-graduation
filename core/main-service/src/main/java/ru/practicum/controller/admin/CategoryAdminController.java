package ru.practicum.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.service.CategoryService;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CategoryAdminController {
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    public CategoryDto createCategory(@RequestBody @Validated NewCategoryDto categoryDto) {
        log.info("/admin/categories/POST/createCategory - {}", categoryDto);
        return categoryService.addNewCategory(categoryDto);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long categoryId) {
        log.info("/admin/categories/DELETE/deleteCategory");
        categoryService.deleteCategoryById(categoryId);
    }

    @PatchMapping("/{categoryId}")
    public CategoryDto updateCategory(@PathVariable(value = "categoryId") @Min(1) Long categoryId,
                                      @RequestBody @Valid CategoryDto categoryDto) {
        log.info("/admin/categories/PATCH/updateCategory - {}", categoryDto);
        return categoryService.updateCategory(categoryId, categoryDto);
    }
}
