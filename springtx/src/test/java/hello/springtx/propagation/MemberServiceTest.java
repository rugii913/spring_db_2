package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
class MemberServiceTest {

    @Autowired
    MemberService memberService;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    LogRepository logRepository;

    /**
     * memberService     @Transactional: OFF
     * memberRepository  @Transactional: ON
     * logRepository     @Transactional: ON
     * - JPA를 통한 모든 데이터 변경에는 트랜잭션 필요
     *   그런데 강의 예제 코드 활용 1의 service에서 트랜잭션이 반드시 필요한 부분이 없기에, 트랜잭션을 repository에서 시작하는 것으로 배치함
     */
    @Test
    void outerTxOff_success() {
        // Given
        String username = "outerTxOff_success";

        // When
        memberService.joinV1(username);

        // Then: 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService     @Transactional: OFF
     * memberRepository  @Transactional: ON
     * logRepository     @Transactional: ON Exception
     */
    @Test
    void outerTxOff_fail() {
        // Given
        String username = "로그예외_outerTxOff_fail";

        // When: logRepository의 트랜잭션에서 예외 발생 및 롤백
        assertThatThrownBy(() -> memberService.joinV1(username)).isExactlyInstanceOf(RuntimeException.class);

        // Then: 완전히 롤백되지 않고, member 데이터가 남아서 저장된다. - 데이터 정합성에 문제가 생김
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * memberService     @Transactional: ON
     * memberRepository  @Transactional: OFF
     * logRepository     @Transactional: OFF
     */
    @Test
    void singleTx() {
        // Given
        String username = "outerTxOff_success";

        // When
        memberService.joinV1(username);

        // Then: 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }
}