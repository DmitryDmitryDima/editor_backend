package com.mytry.editortry.Try.dto.dashboard;

import com.mytry.editortry.Try.dto.projects.FlatTreeMember;
import com.mytry.editortry.Try.utils.cache.CacheSystem;
import com.mytry.editortry.Try.utils.cache.ProjectCache;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnswer {



    private List<FlatTreeMember> files = new ArrayList<>();

    private Map<Long, ProjectCache> cache;

}
