package com.mytry.editortry.Try.dto.dashboard;

import com.mytry.editortry.Try.dto.projects.FlatTreeMember;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnswer {



    private List<FlatTreeMember> files = new ArrayList<>();

}
