package com.mytry.editortry.Try.dto.execution;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntryPointSetRequest {
    private Long projectId;
    private Long fileId;
}
