package com.ahogeking.studentanalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentSortOption {
    private String sortColumn;
    private String sortOrder;
}
