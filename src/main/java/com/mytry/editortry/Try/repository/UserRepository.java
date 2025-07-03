package com.mytry.editortry.Try.repository;

import com.mytry.editortry.Try.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {


    // Метод с жадной загрузкой проектов
    @EntityGraph(attributePaths = {"projects"})
    Optional<User> findWithProjectsByUsername(String username);




}
