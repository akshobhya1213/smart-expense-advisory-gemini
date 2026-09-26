package com.expensetracker.dto;

public class CategoryDtos {
    public record CategoryResponse(Long id, String name, String icon) {}
}
