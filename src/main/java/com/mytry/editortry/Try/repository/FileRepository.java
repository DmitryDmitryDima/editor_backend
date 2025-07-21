package com.mytry.editortry.Try.repository;

import com.mytry.editortry.Try.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<File,Long> {





}
