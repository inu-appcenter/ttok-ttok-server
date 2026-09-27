package com.inuappcenter.team_2_project_server.domain.member.entity;

import com.inuappcenter.team_2_project_server.domain.department.College;
import com.inuappcenter.team_2_project_server.domain.department.Department;
import com.inuappcenter.team_2_project_server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "professor",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_professor_department_name_email",
                        columnNames = {"department", "name", "email"}
                )
        }
)
public class Professor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "position_raw")
    String positionRaw;

    @Enumerated(EnumType.STRING)
    College college;

    @Enumerated(EnumType.STRING)
    Department department;

    @Column(nullable = false)
    String name;

    @Column(name = "phone_number")
    String phoneNumber;

    String email;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", unique = true)
    Member member;

    private Professor(
            String name,
            String positionRaw,
            College college,
            Department department,
            String phoneNumber,
            String email
    ) {
        this.name = name;
        this.positionRaw = positionRaw;
        this.college = college;
        this.department = department;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    // 엑셀 재임포트로 원본 데이터를 갱신. 이미 계정과 연동된 교수는 본인이 직접 고쳤을 수 있으니 덮어쓰지 않는다 (호출부에서 그 여부를 판단)
    public void updateFromExcel(
            String positionRaw,
            College college,
            String phoneNumber
    ) {
        this.positionRaw = positionRaw;
        this.college = college;
        this.phoneNumber = phoneNumber;
    }

    public static Professor create(
            String name,
            String positionRaw,
            College college,
            Department department,
            String phoneNumber,
            String email) {
        return new Professor(name, positionRaw, college, department, phoneNumber, email);
    }
}
