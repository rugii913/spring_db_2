package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

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
        String username = "singleTx";

        // When
        memberService.joinV1(username);

        // Then: 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService     @Transactional: ON
     * memberRepository  @Transactional: ON
     * logRepository     @Transactional: ON
     */
    @Test
    void outerTxOn_success() {
        // Given
        String username = "outerTxOn_success";

        // When
        memberService.joinV1(username);

        // Then: 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService     @Transactional: ON
     * memberRepository  @Transactional: ON
     * logRepository     @Transactional: ON Exception
     */
    @Test
    void outerTxOn_fail() {
        // Given
        String username = "로그예외_outerTxOn_fail";

        // When: logRepository의 예외 발생 -> memberService까지 예외 올라옴
        assertThatThrownBy(() -> memberService.joinV1(username)).isExactlyInstanceOf(RuntimeException.class);

        // Then: 모든 데이터가 롤백된다. - 데이터 정합성에 문제 발생하지 않는다. - outerTxOff_fail()와 비교
        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * memberService     @Transactional: ON
     * memberRepository  @Transactional: ON
     * logRepository     @Transactional: ON Exception
     */
    @Test
    void recoverException_fail() { // 내부 트랜잭션에서 발생한 예외를 잡아서 복구한 경우, 외부 트랜잭션은 별 문제 없이 커밋할 수 있을까? ==> 실무에서 많이들 실수하는 부분
        // Given
        String username = "로그예외_recoverException_fail";

        // When: logRepository의 예외 발생 -> memberService.joinV2(~)에서는 예외 잡아서 복구 => RuntimeException.class이 아니라 UnexpectedRollbackException.class
        assertThatThrownBy(() -> memberService.joinV2(username)).isExactlyInstanceOf(UnexpectedRollbackException.class);
        // 트랜잭션 매니저에서 rollbackOnly 설정을 확인한 후 던진 UnexpectedRollBackException이 프록시 MemberService를 거쳐 클라이언트까지 전달됨

        // Then: 모든 데이터가 롤백된다.
        // assertTrue(memberRepository.find(username).isPresent()); // 필요: true, 실제 false - 내부 트랜잭션에서 rollbackOnly 마크하기 때문에 여전히 롤백됨
        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
    }
}
