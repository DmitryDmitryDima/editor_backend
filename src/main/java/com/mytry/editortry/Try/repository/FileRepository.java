package com.mytry.editortry.Try.repository;

import com.mytry.editortry.Try.model.File;
import com.mytry.editortry.Try.model.state.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File,Long> {



    List<File> findByStatus(FileStatus status);

}
