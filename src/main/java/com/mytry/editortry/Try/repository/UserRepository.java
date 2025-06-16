package com.mytry.editortry.Try.repository;

import com.mytry.editortry.Try.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {


    // Метод с жадной загрузкой проектов
    @EntityGraph(attributePaths = {"projects"})
    Optional<User> findWithProjectsByUsername(String username);
}
