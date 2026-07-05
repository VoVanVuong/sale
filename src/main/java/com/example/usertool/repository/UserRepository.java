package com.example.usertool.repository;

import com.example.usertool.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    Optional<User> findByIdAndDeletedFalse(Long id);

    List<User> findAllByDeletedFalse();
}
