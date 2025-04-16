package ru.practicum.ewm.categories.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.categories.CategoryDto;
import ru.practicum.ewm.dto.categories.NewCategoryDto;
import ru.practicum.ewm.categories.mapper.CategoryMapper;
import ru.practicum.ewm.categories.model.Category;
import ru.practicum.ewm.categories.repository.CategoriesRepository;
import ru.practicum.ewm.error.exception.NotFoundException;
import ru.practicum.ewm.util.PagingUtil;

import java.util.List;

@Transactional(readOnly = true)
@Service
@Slf4j
@RequiredArgsConstructor
public class CategoriesServiceImpl implements CategoriesService {
    private final CategoriesRepository categoriesRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    @Override
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        Category category = categoriesRepository.save(categoryMapper.toEntity(newCategoryDto));
        log.info("Category is created: {}", category);
        return categoryMapper.toDto(category);
    }

    @Transactional
    @Override
    public CategoryDto updateCategory(Long id, NewCategoryDto updateCategoryDto) {
        log.info("start updateCategory");
        Category category = categoriesRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category with id " + id + " not found"));
        category = categoriesRepository.save(categoryMapper.update(category, updateCategoryDto));
        log.info("Category is updated: {}", category);
        return categoryMapper.toDto(categoriesRepository.save(category));
    }

    @Transactional
    @Override
    public void deleteCategory(Long id) {
        categoriesRepository.deleteById(id);
        log.info("Category deleted with id: {}", id);
    }

    @Override
    public CategoryDto findBy(Long id) {
        log.info("Starting to retrieve category by id: {}", id);

        return categoriesRepository.findById(id)
                .map(category -> {
                    log.info("Category is found: {}", category);
                    return categoryMapper.toDto(category);
                })
                .orElseThrow(() -> new NotFoundException("Category with id " + id + " not found"));
    }

    @Override
    public List<CategoryDto> findBy(int from, int size) {
        log.info("start getCategory by from {} to {}", from, size);
        return categoriesRepository.findAll(PagingUtil.pageOf(from, size)).stream()
                .map(categoryMapper::toDto).toList();
    }
}