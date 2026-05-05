package org.unihubworkshop.workshopservice.models;


import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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