package com.mytry.editortry.Try.utils.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectLastUpdateInfo {

    private Instant updatedAt;

    // нас интересуют именно изменения в файле - если изменения затрагивают проект в целом, кеш стирается автоматически
    private Long fileId;

}
