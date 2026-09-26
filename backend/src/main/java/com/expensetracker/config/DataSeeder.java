package com.expensetracker.config;

import com.expensetracker.entity.Category;
import com.expensetracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

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

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            List<Category> defaults = DEFAULT_CATEGORIES.entrySet().stream()
                    .map(e -> Category.builder().name(e.getKey()).icon(e.getValue()).isDefault(true).build())
                    .toList();
            categoryRepository.saveAll(defaults);
        }
    }
}
