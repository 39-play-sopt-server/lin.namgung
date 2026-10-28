package org.sopt.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.sopt.global.entity.BaseEntity;
import org.sopt.global.exception.ErrorCode;
import org.sopt.member.exception.InvalidMemberException;

@Entity
public class Member extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

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

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isDeleted() {
        return deleted;
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
