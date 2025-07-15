package com.mytry.editortry.Try.repository.saga;

import com.mytry.editortry.Try.model.saga.FileIdempotentProcess;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileIdempotentProcessRepository extends JpaRepository<FileIdempotentProcess, Long> {
}
