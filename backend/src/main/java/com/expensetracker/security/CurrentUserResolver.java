package com.expensetracker.security;

import com.expensetracker.entity.User;
import com.expensetracker.exception.ApiExceptions;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UserRepository userRepository;

    public UserPrincipal principal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public Long userId() {
        return principal().userId();
    }

    public User entity() {
        return userRepository.findById(userId())
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("User not found"));
    }
}
