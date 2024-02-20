package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // querydsl로 dto 조회하기 위해선 기본 생성자 필요
public class MemberDto {
    private String username;
    private int age;

    @QueryProjection // 이후에 compileQuerydsl 실행. dto도 Q로 생성해줌.
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
