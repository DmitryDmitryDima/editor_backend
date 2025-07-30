package com.mytry.editortry.Try.dto.files;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditorFileSaveAnswer {
    private Instant updatedAt;
}
