package org.unihubworkshop.workshopservice.dto;

import java.util.List;

public class ReferenceDataResponse {
    private List<ReferenceItemDto> departments;
    private List<ReferenceItemDto> majors;
    private List<ReferenceItemDto> classes;

    public ReferenceDataResponse(List<ReferenceItemDto> departments, List<ReferenceItemDto> majors, List<ReferenceItemDto> classes) {
        this.departments = departments;
        this.majors = majors;
        this.classes = classes;
    }

    public List<ReferenceItemDto> getDepartments() { return departments; }
    public void setDepartments(List<ReferenceItemDto> departments) { this.departments = departments; }

    public List<ReferenceItemDto> getMajors() { return majors; }
    public void setMajors(List<ReferenceItemDto> majors) { this.majors = majors; }

    public List<ReferenceItemDto> getClasses() { return classes; }
    public void setClasses(List<ReferenceItemDto> classes) { this.classes = classes; }
}