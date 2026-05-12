package org.unihubworkshop.dataimportservice.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentClass {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Size(max = 100, message = "Class name must be less than 100 characters")
    @Column(name = "class_name", nullable = false, unique = true)
    private String className;

}
