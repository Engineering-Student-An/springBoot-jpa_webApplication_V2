package jpabook2.jpashop2.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jpabook2.jpashop2.domain.Address;
import jpabook2.jpashop2.domain.Member;
import jpabook2.jpashop2.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController     //@Controller @ResponseBody 두 개의 annotation 합친 것
                    // @ResponseBody : 데이터 자체를 바로 json이나 xml로 보내기 위한 annotation
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    // ========== 회원 조회 api ==========
    @GetMapping("/api/v1/members")
    public List<Member> membersV1(){    // 엔티티를 직접 노출 => 엔티티 내부의 정보들이 모두 노출됨
                                        // 클라이언트마다 요구 정보가 다르기 때문에 엔티티 안에 (@JsonIgnore 같은 것)을 녹이기 시작하면 해결 불가
                                        //           + 화면을 뿌리기 위한 로직(프레젠테이션 계층을 위한 로직)이 엔티티에 추가됨
        // 엔티티의 필드명이 (name -> username) 변경되면 api 스팩이 변경됨
        // 결론 : 엔티티를 직접 반환하면 안됨!
        return memberService.findMembers();
    }

    @GetMapping("/api/v2/members")
    public Result membersV2() { // 결과를 List<Member>에서 List<MemberDto>로 바꿈
        // 엔티티를 dto로 변환하는 수고는 추가됨
        // => 장점 : 엔티티가 변경되도 api 스팩이 변경되지 않음 + Result로 감싸서 반환하므로 유연성 생김!
        List<Member> findMembers = memberService.findMembers();
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName(), m.getAddress()))
                .collect(Collectors.toList());

        return new Result(collect.size(), collect);     // Result 껍데기로 감쌈 => 리스트를 반환하면 json 배열 타입으로 나가므로 유연성이 떨어짐
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class MemberDto {    // 노출하고 싶은 정보만 스팩에 노출 => api 스팩이 DTO와 1:1 매칭됨 => 클라이언트 요청 별 반환하고 싶은 정보를 컨트롤 가능
        private String name;
        private Address address;    // address 필드 추가함
    }

    // ========== ========== ==========

    // ========== 회원 등록 api ==========
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) { // => api 만들때는 항상 엔티티를 파라미터로 받지 말기!
        //      + 엔티티를 외부에 노출해서도 안됨!
        //@Valid => jakarta validation 검증 (Member 엔티티에서 name => @NotEmpty) => postman에서 name없이 데이터 전송하면 에러발생!
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @PostMapping("/api/v2/members")
    // 별도의 DTO 사용
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /*
    V1 vs V2
    V1 : DTO 클래스를 안만들어도 되는 유일한 장점 / 엔티티 필드를 수정했을 때 같이 수정됨 (잘못하면 같이 망함)
    V2 : 엔티티와 api 스팩을 명확하게 분리 가능
         + 엔티티를 변경해도 api 스팩이 안바뀜 (멤버 엔티티에서 name -> username으로 수정하면 컴파일 오류 발생!
                                     => getName을 getUsername으로 수정만 하면 됨 => api에 전혀 영향 x)
         DTO를 받으면 api 스팩을 확인 가능함 (DTO 클래스 확인 => name만 받게 되있음을 알 수 있음)
                + DTO에서 validation 조건 추가 => 멤버 엔티티의 name 필드에 @NotEmpty 넣으면 어떤 api에서는 null 가능이라면 오류
                ====> DTO사용으로 api 스팩에 정확하게 핏하게 맞출 수 있음 (=> 유지보수에 큰 장점)

    ==============================================================================================================
     결론 : 엔티티를 외부에 노출하거나 엔티티를 파라미터로 그대로 받지 않고 api 스팩에 맞는 별도의 DTO를 만드는 것이 api 만들때의 정석임!
     */

    @Data
    static class CreateMemberRequest {
        @NotEmpty
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }

    // ========== ========== ==========

    // ========== 회원 수정 api ==========
    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id,
                                               @RequestBody @Valid UpdateMemberRequest request) {
        memberService.update(id, request.getName());
        Member findMember = memberService.findOne(id);  // 커맨드와 쿼리를 분리
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    @Data
    static class UpdateMemberRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }

    // ========== ========== ==========
}
