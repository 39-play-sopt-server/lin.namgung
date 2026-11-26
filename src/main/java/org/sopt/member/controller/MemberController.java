package org.sopt.member.controller;

import org.sopt.global.response.ApiResponse;
import org.sopt.global.response.SuccessCode;
import org.sopt.member.dto.response.MemberResponse;
import org.sopt.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/members")
public class MemberController implements MemberApi {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    @GetMapping(path = "/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMe(@AuthenticationPrincipal Long memberId) {
        return ApiResponse.toResponseEntity(SuccessCode.MEMBER_READ, memberService.getMember(memberId));
    }

    @Override
    @DeleteMapping(path = "/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe(@AuthenticationPrincipal Long memberId) {
        memberService.withdrawMember(memberId);
        return ApiResponse.toResponseEntity(SuccessCode.MEMBER_DELETED, null);
    }
}
