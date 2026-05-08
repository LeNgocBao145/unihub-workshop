package org.unihubworkshop.workshopservice.models;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class StudentProfile {
    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "student_code", nullable = false, unique = true)
    private String studentCode;

    @Column(nullable = false)
    private String department;

    @Column
    private String major;

    @Column(name = "class_name")
    private String className;
}