package com.example.dify.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DegreeDTO {
    private String name;
    private String birthday;
    private String university;
    private String degreeNo;
}
