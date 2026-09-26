package com.expensetracker.service;

import com.expensetracker.dto.CategoryDtos.CategoryResponse;
import com.expensetracker.entity.Category;
import com.expensetracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private static final Map<String, String> DEFAULT_CATEGORIES = Map.ofEntries(
            Map.entry("Food", "utensils"),
            Map.entry("Transportation", "car"),
            Map.entry("Shopping", "shopping-bag"),
            Map.entry("Bills", "receipt"),
            Map.entry("Entertainment", "film"),
            Map.entry("Healthcare", "heart-pulse"),
            Map.entry("Education", "graduation-cap"),
            Map.entry("Other", "circle-ellipsis")
    );

    /**
     * Always make sure the default categories exist before returning them.
     * This is intentionally done at read time as well as application startup,
     * because a restarted/deployed instance may have an empty or partially
     * initialized database.
     */
    @Transactional
    public List<CategoryResponse> getAll() {
        ensureDefaultCategories();
        return categoryRepository.findAll().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getIcon()))
                .toList();
    }

    private void ensureDefaultCategories() {
        DEFAULT_CATEGORIES.forEach((name, icon) -> {
            if (categoryRepository.findByNameIgnoreCase(name).isEmpty()) {
                categoryRepository.save(Category.builder()
                        .name(name)
                        .icon(icon)
                        .isDefault(true)
                        .build());
            }
        });
    }
}
