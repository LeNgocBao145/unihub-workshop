package org.unihubworkshop.dataimportservice.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfile {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "student_code", nullable = false, unique = true)
    private String studentCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(nullable = false)
    private String department;

    @Column
    private String major;

    @Column(name = "class_name")
    private String className;
}
