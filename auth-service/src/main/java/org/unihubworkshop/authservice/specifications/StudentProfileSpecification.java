package org.unihubworkshop.authservice.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import org.unihubworkshop.authservice.entities.StudentProfile;

import java.util.ArrayList;
import java.util.List;

public class StudentProfileSpecification {

    public static Specification<StudentProfile> filterBy(
            String studentCode,
            String department,
            String major,
            String className) {
        
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(studentCode)) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("studentCode")),
                        "%" + studentCode.toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(department)) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("department")),
                        department.toLowerCase()
                ));
            }

            if (StringUtils.hasText(major)) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("major")),
                        major.toLowerCase()
                ));
            }

            if (StringUtils.hasText(className)) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("className")),
                        "%" + className.toLowerCase() + "%"
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}