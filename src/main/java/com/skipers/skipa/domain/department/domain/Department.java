package com.skipers.skipa.domain.department.domain;

import com.skipers.skipa.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "departments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_departments_name", columnNames = "name")
        }
)
public class Department extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Builder
    private Department(String name) {
        this.name = name;
    }

    public void update(String name) {
        this.name = name;
    }
}
