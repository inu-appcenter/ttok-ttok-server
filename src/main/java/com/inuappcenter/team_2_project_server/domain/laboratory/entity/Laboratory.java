package com.inuappcenter.team_2_project_server.domain.laboratory.entity;

import com.inuappcenter.team_2_project_server.domain.department.College;
import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "laboratory",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_laboratory_lab_name_professor_department",
                        columnNames = {"lab_name", "professor_id", "department"}
                )
        }
)
public class Laboratory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "laboratory_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private College college;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Department department;

    @Column(name = "lab_name", nullable = false)
    private String labName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    private Professor professor;

    @Column(name = "lab_url")
    private String labUrl;

    // 엑셀 원문을 그대로 저장
    @Column(name = "research_field_raw")
    private String researchFieldRaw;
    
    private String location;

    @Column(name = "graduate_student_count")
    private Integer graduateStudentCount;

    @Column(name = "undergraduate_student_count")
    private Integer undergraduateStudentCount;

    private String introduction;

    private Laboratory(
            College college,
            Department department,
            String labName,
            String location,
            Integer graduateStudentCount,
            Integer undergraduateStudentCount,
            String introduction,
            Professor professor,
            String labUrl,
            String researchFieldRaw
    ) {
        this.college = college;
        this.department = department;
        this.labName = labName;
        this.location = location;
        this.graduateStudentCount = graduateStudentCount;
        this.undergraduateStudentCount = undergraduateStudentCount;
        this.introduction = introduction;
        this.professor = professor;
        this.labUrl = labUrl;
        this.researchFieldRaw = researchFieldRaw;
    }

    public static Laboratory create(
            College college,
            Department department,
            String labName,
            String location,
            Integer graduateStudentCount,
            Integer undergraduateStudentCount,
            String introduction,
            Professor professor,
            String labUrl,
            String researchFieldRaw
    ) {
        return new Laboratory(college, department, labName, location, graduateStudentCount, undergraduateStudentCount, introduction, professor, labUrl, researchFieldRaw);
    }

    public void updateLab(
            String labName,
            String location,
            Integer graduateStudentCount,
            Integer undergraduateStudentCount,
            String introduction,
            String labUrl,
            String researchFieldRaw
    ) {
        if (labName != null) {
            this.labName = labName;
        }
        if (location != null) {
            this.location = location;
        }
        if (graduateStudentCount != null) {
            this.graduateStudentCount = graduateStudentCount;
        }
        if (undergraduateStudentCount != null) {
            this.undergraduateStudentCount = undergraduateStudentCount;
        }
        if (introduction != null) {
            this.introduction = introduction;
        }
        if (labUrl != null) {
            this.labUrl = labUrl;
        }
        if (researchFieldRaw != null) {
            this.researchFieldRaw = researchFieldRaw;
        }
    }
}
