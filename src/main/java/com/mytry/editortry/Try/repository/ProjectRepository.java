package com.mytry.editortry.Try.repository;

import com.mytry.editortry.Try.model.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {


    Optional<Project> findByOwnerUsernameAndName(String username, String name);

}
