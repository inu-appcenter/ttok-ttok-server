package com.inuappcenter.team_2_project_server.domain.ai.service.chatbot;

import com.inuappcenter.team_2_project_server.domain.ai.client.FactChatClient;
import com.inuappcenter.team_2_project_server.domain.ai.dto.request.EmailRequestDto;
import com.inuappcenter.team_2_project_server.domain.ai.dto.response.EmailResponseDto;
import com.inuappcenter.team_2_project_server.domain.laboratory.entity.Laboratory;
import com.inuappcenter.team_2_project_server.domain.laboratory.repository.LaboratoryRepository;
import com.inuappcenter.team_2_project_server.global.error.ex.ErrorCode;
import com.inuappcenter.team_2_project_server.global.error.ex.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 교수님께 메일 쓰기
 * 1. laboratoryId로 연구실과 담당 교수 정보를 DB에서 꺼낸다. (연구실이 정해져 있으므로 검색(RAG)은 필요 없다)
 * 2. LLM은 본문만 쓴다. 학생 입력과 연구실 정보를 프롬프트로 넘긴다.
 * 3. 인사말, 자기소개, 연락 목적 문장, 끝인사, 서명은 서버가 고정 형식으로 붙인다.
 * (교수 이름, 학과, 이름처럼 틀리면 안 되는 값을 LLM이 옮겨 적지 않게 하기 위함)
 * <p>
 * 외부 API(LLM) 호출 동안 DB 연결을 잡고 있지 않도록 메서드 전체를 트랜잭션으로 묶지 않는다.
 */
@Slf4j
@Service
public class EmailEditorService {

    public static final String SYSTEM_PROMPT = """
            당신은 인천대학교 학생이 교수님께 보낼 이메일의 본문을 써 주는 도우미입니다.
            인사말, 자기소개, 연락 목적 문장, 끝인사, 서명은 따로 붙이므로, 당신은 그 사이에 들어갈 본문만 씁니다.
            
            규칙:
            1. 본문만 씁니다. "안녕하세요", "저는 ~입니다", "감사합니다", "~드림" 같은 인사말, 자기소개, 끝인사, 서명은 쓰지 않습니다.
            2. 정확히 2문단으로 쓰고, 두 문단 사이에는 빈 줄을 하나 넣습니다. 첫 문장은 "저는"으로 시작하지 않습니다.
               - 1문단 (3~4문장): 관심 있는 연구, 관련해서 해본 일, 연구실의 연구가 어떻게 이어지는지 설명합니다.
               - 2문단 (2문장): [메일 목적]에 맞게 정중히 요청합니다.
                 · 학부연구생 지원: 면담 요청과 참여 방법, 준비할 점 문의
                   예) 바쁘신 중에 시간을 내어 주셔서 가능하시다면 면담을 통해 연구실의 연구 현황과 학부연구생 참여 방법에 대해 말씀을 들을 수 있으면 감사하겠습니다. 또한 학부연구생으로 참여하기 위해 준비하면 좋을 사항이나 지원 절차가 있다면 함께 여쭙고 싶습니다.
                 · 대학원 진학 상담: 진학 상담 요청과 준비할 점 문의
                 · 교수님께 질문: 궁금한 점을 구체적으로 여쭙고 답변 부탁
            3. 연구실에 대한 내용은 [연구실 정보]에 적힌 것만 근거로 씁니다. 적혀 있지 않은 연구나 성과를 지어내지 않습니다.
            4. 학생의 경험과 학습은 [관련해서 해본 일]에 적힌 것만 씁니다. 과장하거나 새로 만들지 않습니다.
               [관련해서 해본 일]이 (없음)이면 경험, 공부, 활동에 대한 문장을 아예 쓰지 않습니다. "경험은 없지만"처럼 경험이 없다는 말도 쓰지 않습니다.
            5. 정중한 존댓말로, 전체 공백 포함 300~450자 정도로 짧게 씁니다.
            6. 제목, 따옴표, 마크다운 없이 본문 텍스트만 출력합니다.""";

    private static final String EMPTY = "(없음)";

    private final LaboratoryRepository laboratoryRepository;
    private final FactChatClient factChatClient;
    private final String chatModel;

    public EmailEditorService(
            LaboratoryRepository laboratoryRepository,
            FactChatClient factChatClient,
            @Value("${ai.email-editor.chat-model}") String chatModel
    ) {
        this.laboratoryRepository = laboratoryRepository;
        this.factChatClient = factChatClient;
        this.chatModel = chatModel;
    }

    // 사용자 메시지: 메일 목적 + 연구실 정보 + 학생 입력. 이름은 본문에 쓰지 않으므로 넘기지 않는다
    static String buildUserMessage(EmailRequestDto request, Laboratory laboratory) {
        return "[메일 목적]\n" + request.purpose().getDescription() + "\n\n"
                + "[연구실 정보]\n"
                + "연구실: " + laboratory.getLabName() + "\n"
                + "담당 교수: " + orEmpty(professorName(laboratory)) + "\n"
                + "연구 분야: " + orEmpty(laboratory.getResearchFieldRaw()) + "\n"
                + "연구실 소개: " + orEmpty(laboratory.getIntroduction()) + "\n\n"
                + "[학생]\n"
                + request.department().getDepartmentName() + " " + request.grade().getIntroText() + "\n\n"
                + "[관심 있는 연구]\n" + request.interest().strip() + "\n\n"
                + "[관련해서 해본 일]\n" + orEmpty(request.content());
    }

    // "[학부연구생 지원 문의] 국어국문학과 홍길동"
    static String buildSubject(EmailRequestDto request) {
        return "[" + request.purpose().getSubjectLabel() + "] "
                + request.department().getDepartmentName() + " " + request.name().strip();
    }

    static String wrapBody(EmailRequestDto request, Laboratory laboratory, String body) {
        String professorName = professorName(laboratory);
        String greeting = professorName == null ? "안녕하세요, 교수님." : "안녕하세요, " + professorName + " 교수님.";
        String name = request.name().strip();

        return greeting + "\n"
                + request.department().getDepartmentName() + " " + request.grade().getIntroText() + " " + name + "입니다. "
                + request.purpose().getIntroSentence() + "\n\n"
                + body + "\n\n"
                + "감사합니다.\n"
                + name + " 드림";
    }

    private static String professorName(Laboratory laboratory) {
        return laboratory.getProfessor() == null ? null : laboratory.getProfessor().getName();
    }

    private static String orEmpty(String value) {
        return value == null || value.isBlank() ? EMPTY : value.strip();
    }

    /**
     * 메일 초안 생성. 연구실이 없으면 404, LLM 호출이 실패하거나 빈 답이면 503
     */
    public EmailResponseDto writeDraft(EmailRequestDto request) {
        // 1. 연구실 + 담당 교수 (교수까지 함께 조회해 트랜잭션 밖에서 교수 이름을 읽는다)
        Laboratory laboratory = laboratoryRepository.findWithProfessorByIdIn(List.of(request.laboratoryId())).stream()
                .findFirst()
                .orElseThrow(() -> new MyException(ErrorCode.LABORATORY_NOT_FOUND));

        // 2. 본문: LLM
        String body = factChatClient.chat(chatModel, SYSTEM_PROMPT, buildUserMessage(request, laboratory))
                .map(String::strip)
                .filter(answer -> !answer.isEmpty())
                .orElseThrow(() -> {
                    log.warn("메일 초안 본문 생성 실패 (laboratoryId={})", request.laboratoryId());
                    return new MyException(ErrorCode.EMAIL_DRAFT_UNAVAILABLE);
                });

        // 3. 고정 형식으로 감싼다
        return new EmailResponseDto(buildSubject(request), wrapBody(request, laboratory, body));
    }
}
