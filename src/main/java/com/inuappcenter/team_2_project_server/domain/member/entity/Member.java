package com.inuappcenter.team_2_project_server.domain.member.entity;

import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.member.enums.UserType;
import com.inuappcenter.team_2_project_server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "member",
        uniqueConstraints = {@UniqueConstraint(name = "uk_user_student_number", columnNames = "student_number")}
)
public class Member extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "student_number", nullable = false, unique = true)
    private String studentNumber;

    @Column(name = "nickname")
    private String nickName;

    @Enumerated(EnumType.STRING)
    private Department department;

    private String email;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "is_new", nullable = false)
    @ColumnDefault("false")
    private boolean isNew = true;

    // 이 시각 이전에 발급된(iat) 토큰은 무효로 본다. 로그아웃 시 now() 로 갱신. null 이면 무효화 이력 없음
    @Column(name = "token_invalid_before")
    private LocalDateTime tokenInvalidBefore;

    private String role;

    @Column(name = "user_type")
    @Enumerated(EnumType.STRING)
    private UserType userType;

    private Member(
            String studentNumber,
            String nickName,
            Department department,
            String email,
            LocalDateTime lastLoginAt,
            String role
    ) {
        this.studentNumber = studentNumber;
        this.nickName = nickName;
        this.department = department;
        this.email = email;
        this.lastLoginAt = lastLoginAt;
        this.role = role;
    }

    public static Member create(
            String studentNumber,
            String nickName,
            Department department,
            String email
    ) {
        return new Member(studentNumber, nickName, department, email, LocalDateTime.now(), "ROLE_USER");
    }

    // 외부에서 role을 받아서 member를 만드는 정적 팩토리 메서드
    public static Member createWithRole(
            String studentNumber,
            String nickName,
            Department department,
            String email,
            String role
    ) {
        return new Member(studentNumber, nickName, department, email, LocalDateTime.now(), role);
    }

    // college는 department에 종속된 값이라 별도로 저장하지 않고 그때그때 계산한다
    public College getCollege() {
        return department == null ? null : department.getCollegeName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.id.toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void updateMemberProfile(
            String nickName,
            Department department,
            String email
    ) {
        if (nickName != null) {
            this.nickName = nickName;
        }
        if (department != null) {
            this.department = department;
        }
        if (email != null) {
            this.email = email;
        }
    }

    public void updateIsNew() {
        this.isNew = false;
    }

    public void assignUserType(UserType userType) {
        this.userType = userType;
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // 로그아웃: 지금까지 발급된 모든 access/refresh 토큰을 무효화한다
    public void logout() {
        this.tokenInvalidBefore = LocalDateTime.now();
    }
}
