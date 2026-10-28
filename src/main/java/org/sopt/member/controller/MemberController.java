package org.sopt.member.controller;

import org.sopt.global.response.ApiResponse;
import org.sopt.global.response.SuccessCode;
import org.sopt.member.dto.request.CreateMemberRequest;
import org.sopt.member.dto.response.MemberResponse;
import org.sopt.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(path = "/api/v1/members")
public class MemberController implements MemberApi {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> createMember(
            @RequestBody CreateMemberRequest request
    ) {
        MemberResponse response = memberService.registerMember(request.name());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{memberId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.of(SuccessCode.MEMBER_CREATED, response));
    }

    @Override
    @DeleteMapping(path = "/{memberId}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(
            @PathVariable(name = "memberId") Long memberId
    ) {
        memberService.withdrawMember(memberId);
        return ApiResponse.toResponseEntity(SuccessCode.MEMBER_DELETED, null);
    }
}
