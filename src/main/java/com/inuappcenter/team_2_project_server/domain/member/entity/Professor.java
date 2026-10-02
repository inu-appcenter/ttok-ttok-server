package com.inuappcenter.team_2_project_server.domain.member.entity;

import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
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
            Department department,
            String phoneNumber,
            String email
    ) {
        this.name = name;
        this.positionRaw = positionRaw;
        this.department = department;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    public static Professor create(
            String name,
            String positionRaw,
            Department department,
            String phoneNumber,
            String email) {
        return new Professor(name, positionRaw, department, phoneNumber, email);
    }

    // college는 department에 종속된 값이라 별도로 저장하지 않고 그때그때 계산한다
    public College getCollege() {
        return department == null ? null : department.getCollegeName();
    }

    // 엑셀 재임포트로 원본 데이터를 갱신. 이미 계정과 연동된 교수는 본인이 직접 고쳤을 수 있으니 덮어쓰지 않는다 (호출부에서 그 여부를 판단)
    public void updateFromExcel(
            String positionRaw,
            String phoneNumber
    ) {
        this.positionRaw = positionRaw;
        this.phoneNumber = phoneNumber;
    }

    // 본인(교수)이 직접 자기 정보를 고치는 경우. 학과/이름은 uk_professor_department_name_email 유니크 제약
    // 및 엑셀 재매칭 키와 얽혀있어 여기서는 다루지 않는다 (변경하려면 별도 절차 필요).
    // 이메일은 연동된 교수를 이름+학과로 우선 매칭하도록 해뒀기 때문에 수정해도 재임포트 매칭이 깨지지 않는다
    public void updateProfile(
            String positionRaw,
            String phoneNumber,
            String email
    ) {
        if (positionRaw != null) {
            this.positionRaw = positionRaw;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
        if (email != null) {
            this.email = email;
        }
    }

    // 온보딩에서 본인 이름으로 검색해 찾아낸 교수 레코드에 로그인 계정을 연결
    public void linkMember(Member member) {
        this.member = member;
    }
}
