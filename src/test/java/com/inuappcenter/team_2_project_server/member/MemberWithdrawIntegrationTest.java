package com.inuappcenter.team_2_project_server.member;

import com.inuappcenter.team_2_project_server.domain.bookmark.entity.Bookmark;
import com.inuappcenter.team_2_project_server.domain.bookmark.repository.BookmarkRepository;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.entity.CoffeeChat;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.enums.ContactType;
import com.inuappcenter.team_2_project_server.domain.coffeeChat.repository.CoffeeChatRepository;
import com.inuappcenter.team_2_project_server.domain.department.enums.College;
import com.inuappcenter.team_2_project_server.domain.department.enums.Department;
import com.inuappcenter.team_2_project_server.domain.labReview.entity.LabReview;
import com.inuappcenter.team_2_project_server.domain.labReview.repository.LabReviewRepository;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.domain.member.entity.Member;
import com.inuappcenter.team_2_project_server.domain.member.entity.Professor;
import com.inuappcenter.team_2_project_server.domain.member.entity.Researcher;
import com.inuappcenter.team_2_project_server.domain.member.repository.MemberRepository;
import com.inuappcenter.team_2_project_server.domain.member.repository.ProfessorRepository;
import com.inuappcenter.team_2_project_server.domain.member.repository.ResearcherRepository;
import com.inuappcenter.team_2_project_server.domain.member.service.MemberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 회원 탈퇴가 실제 PostgreSQL의 FK 제약에 걸리지 않고 끝나는지 검증한다.
 * (탈퇴 전에는 회원을 참조하는 북마크/연구자/교수 연동 때문에 FK 위반으로 500이 났다)
 */
@SpringBootTest
@ActiveProfiles("test")
class MemberWithdrawIntegrationTest {

    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ProfessorRepository professorRepository;
    @Autowired
    private LaboratoryRepository laboratoryRepository;
    @Autowired
    private ResearcherRepository researcherRepository;
    @Autowired
    private CoffeeChatRepository coffeeChatRepository;
    @Autowired
    private LabReviewRepository labReviewRepository;
    @Autowired
    private BookmarkRepository bookmarkRepository;

    private Professor professor;
    private Laboratory laboratory;

    @BeforeEach
    void setUp() {
        professor = professorRepository.save(Professor.create("홍교수", "교수", Department.CHEMISTRY, null, "prof@inu.ac.kr"));
        laboratory = laboratoryRepository.save(Laboratory.create(
                College.COLLEGE_OF_NATURAL_SCIENCES, Department.CHEMISTRY, "탈퇴테스트연구실", null, 0, 0,
                null, professor, null, null
        ));
    }

    @AfterEach
    void tearDown() {
        labReviewRepository.deleteAll();
        coffeeChatRepository.deleteAll();
        researcherRepository.deleteAll();
        bookmarkRepository.deleteAll();
        laboratoryRepository.deleteAll();
        professorRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void researcher_withdrawal_deletes_own_data_and_keeps_review_anonymously() {
        Member member = memberRepository.save(Member.create("209900001", "연구생", null, null));
        Researcher researcher = researcherRepository.save(Researcher.create(member, laboratory, "연구생"));
        CoffeeChat coffeeChat = coffeeChatRepository.save(CoffeeChat.create(laboratory, researcher, ContactType.EMAIL, "r@inu.ac.kr"));
        LabReview review = labReviewRepository.save(LabReview.create(laboratory, researcher, "있음", "주 1회", Set.of("논문 리딩")));
        Bookmark bookmark = bookmarkRepository.save(Bookmark.create(member, laboratory));

        memberService.deleteMember(member.getId());

        assertThat(memberRepository.findById(member.getId())).isEmpty();
        assertThat(bookmarkRepository.findById(bookmark.getId())).isEmpty();
        assertThat(coffeeChatRepository.findById(coffeeChat.getId())).isEmpty();
        assertThat(researcherRepository.findById(researcher.getId())).isEmpty();
        // 리뷰는 남고 작성자 연결만 끊긴다
        assertThat(labReviewRepository.findById(review.getId())).hasValueSatisfying(
                saved -> assertThat(saved.getResearcher()).isNull()
        );
        assertThat(labReviewRepository.findAllByLaboratoryId(laboratory.getId())).hasSize(1);
    }

    @Test
    void professor_withdrawal_keeps_professor_record_and_unlinks_account() {
        Member member = memberRepository.save(Member.create("20990002", "교수계정", null, null));
        professor.linkMember(member);
        professorRepository.save(professor);

        memberService.deleteMember(member.getId());

        assertThat(memberRepository.findById(member.getId())).isEmpty();
        assertThat(professorRepository.findById(professor.getId())).hasValueSatisfying(
                saved -> assertThat(saved.getMember()).isNull()
        );
        assertThat(laboratoryRepository.findById(laboratory.getId())).isPresent();
    }

    @Test
    void member_without_related_data_is_deleted() {
        Member member = memberRepository.save(Member.create("209900003", "탐색자", null, null));

        memberService.deleteMember(member.getId());

        assertThat(memberRepository.findById(member.getId())).isEmpty();
    }
}
