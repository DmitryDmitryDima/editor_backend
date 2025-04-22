package com.mytry.editortry.Try.dto.importsuggestion;

import lombok.Value;

import java.util.List;
import java.util.Set;

@Value
public class ImportAnswer {

    // список импортов - только уникальное
    Set<String> imports;
}
