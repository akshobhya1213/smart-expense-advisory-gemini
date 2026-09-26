package com.expensetracker.config;

import com.expensetracker.entity.Category;
import com.expensetracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
        // Seed any missing default category, not only when the table is completely empty.
        // This repairs an existing deployment where the category table was partially seeded.
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
