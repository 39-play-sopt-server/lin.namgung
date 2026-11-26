package org.sopt.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.sopt.global.entity.BaseEntity;
import org.sopt.global.exception.ErrorCode;
import org.sopt.member.exception.InvalidMemberException;

@Entity
@Table(
        name = "member",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_member_social", columnNames = {"social_provider", "social_id"})
        }
)
public class Member extends BaseEntity {
    public static final String WITHDRAWN_NAME = "탈퇴한 회원";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider")
    private SocialProvider socialProvider;

    @Column(name = "social_id")
    private String socialId;

    private boolean deleted;

    protected Member() {
    }

    private Member(String name) {
        this.name = name;
        this.deleted = false;
    }

    public static Member create(String name) {
        validateName(name);
        return new Member(name);
    }

    public static Member createWithEmail(String name, String email, String encodedPassword) {
        Member member = create(name);
        member.email = email;
        member.password = encodedPassword;
        return member;
    }

    public static Member createWithSocial(String name, SocialProvider provider, String socialId) {
        Member member = create(name);
        member.socialProvider = provider;
        member.socialId = socialId;
        return member;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public SocialProvider getSocialProvider() {
        return socialProvider;
    }

    public String getSocialId() {
        return socialId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public String getDisplayName() {
        return deleted ? WITHDRAWN_NAME : name;
    }

    public void delete() {
        this.deleted = true;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidMemberException(ErrorCode.INVALID_MEMBER_NAME);
        }
    }
}
