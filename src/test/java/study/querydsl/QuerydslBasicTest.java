package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.TypedQuery;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1",10,teamA);
        Member member2 = new Member("member2",20,teamA);

        Member member3 = new Member("member3",30,teamB);
        Member member4 = new Member("member4",40,teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }

    @Test
    public void startJPQL(){
        // member1을 찾아라.
        Member findMember = em.createQuery("select m from Member m" +
                        " where m.username =:username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    @Test
    public void startQuerydsl(){ // queryDsl
        // compilieQuerydsl 해주어야함.
//        QMember m = new QMember("m");
//        Member findMember = queryFactory
//                .select(m)
//                .from(m)
//                .where(m.username.eq("member1"))
//                .fetchOne();
        // alt 엔터로 스태틱 임폴트 가능
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){ // 검색 조건 쿼리
        // member는 Qmember를 의미함.
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10))).fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

//         다양한 조건
//        member.username.eq("member1") / username = 'member1'
//        member.username.nq("member1") / username != 'member1'
//        member.username.eq("member1").not() / username != 'member1'
//        member.username.isNotNull() / 이름 is not null

//        member.age.in(10,20) // age in (10,20)
//        member.age.notIn(10,20) // age not in (10, 20)
//        member.age.between(10,30) // between 10,30

//        member.age.goe(3) // age >= 30
//        member.age.gt(30) // age > 30
//        member.age.loe(30) // age <=30
//        member.age.lt(30) // age < 30

//        membre.username.like("member%") // like 검색
//        member.username.contains("member") // like%$member% 검색
//        member.username.startsWith("member") // like 'member%' 검색
    }
    @Test // 검색 결과조회
    public void resultFetch(){
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        Member fetchFirst = queryFactory.selectFrom(member)
                .fetchFirst();

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults(); // 현재 지원X, 단순한 쿼리에서만 사용해주어야함.
        results.getTotal();
        List<Member> content = results.getResults();
    }

    @Test // 정렬
    public void sort(){
        // 회원 정렬 순서
        // 1. 회원 나이 내림차순(desc)
        // 2. 회원 이름 오름차순(asc)
        // 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)

        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
        Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
        Assertions.assertThat(memberNull.getUsername()).isNull();
    }

    @Test // 페이징
    public void paging1(){
        List<Member> result = queryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 0부터 시작
                .limit(2)
                .fetch();
        Assertions.assertThat(result.size()).isEqualTo(2);
        Assertions.assertThat(result.size()).isEqualTo(2);
        Assertions.assertThat(result.size()).isEqualTo(2);
    }
    @Test // 페이징
    public void paging2(){
        QueryResults<Member> queryResults = queryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 0부터 시작
                .limit(2)
                .fetchResults();
        Assertions.assertThat(queryResults.getTotal()).isEqualTo(4);
        Assertions.assertThat(queryResults.getLimit()).isEqualTo(2);
        Assertions.assertThat(queryResults.getOffset()).isEqualTo(1);
        Assertions.assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test // 집합
    public void aggregation(){
        List<Tuple> result = queryFactory // 쿼리dsl에 있는 Tuple
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                ).from(member)
                .fetch();
        // 실무에서는 Tuple을 많이 사용하지는 않음. dto로 조회를 많이 함.
        Tuple tuple = result.get(0);
        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
        Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        Assertions.assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
        Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }
    @Test
    public void group() throws Exception{
        // 팀의 이름과 팀의 평균 연령을 구해라
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamA");
        Assertions.assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        Assertions.assertThat(teamB.get(team.name)).isEqualTo("teamB");
        Assertions.assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test // 조인
    public void join(){
        // 팀A에 소속된 모든 회원
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        result.forEach(System.out::println);
        Assertions.assertThat(result)
                .extracting("username") // member에 있는 username 속성만 추출
                .containsExactly("member1","member2");

    }

    @Test
    public void theta_join(){
        // 세타 조인
        // 회원의 이름이 팀 이름과 같은 회원 조회
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        Assertions.assertThat(result)
                .extracting("username")
                .containsExactly("teamA","teamB");
    }

    @Test // 조인 - on 절
    public void join_on_filtering(){
        // 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
        // JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
//                .join(member.team, team).on(team.name.eq("teamA")) // member의 team 이름이 teamA인 것들만 조회
                .leftJoin(member.team, team).on(team.name.eq("teamA")) // member의 team의 이름이 teamA가 아니면 null
                .fetch();

//                .join(member.team, team).on(team.name.eq("teamA")) ==
//                .join(member.team, team).where(team.name.eq("teamA")와 결과가 같음

        for(Tuple tuple:result){
            System.out.println("tuple = " + tuple);
        }
    }
    @Test
    public void join_on_no_relation(){
        // 연관관계가 없는 엔티티 외부 조인
        // 회원의 이름이 팀 이름과 같은 대상 외부 조인
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = "+ tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test // 페치조인
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // team이 불러와졌는지 확인해주는 메서드

        Assertions.assertThat(loaded).as("페치조인 미적용").isFalse();

        em.flush();
        em.clear();
        // 페치조인
        Member findMember2 = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded2 = emf.getPersistenceUnitUtil().isLoaded(findMember2.getTeam()); // team이 불러와졌는지 확인해주는 메서드
        Assertions.assertThat(loaded2).as("페치조인 적용").isTrue();
    }

    @Test // 서브쿼리
    public void subQuery(){
        QMember memberSub = new QMember("memberSub");
        // 나이가 가장 많은 회원 조회
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                )).fetch();
        Assertions.assertThat(result).extracting("age").containsExactly(40);
        // 나이가 평균 이상인 회원
        List<Member> result2 = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )).fetch();
        Assertions.assertThat(result2).extracting("age").containsExactly(30,40);
    }
    @Test
    public void selectSubQuery(){
        QMember memberSUb = new QMember("memberSub");
        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions // static import 가능
                                .select(memberSUb.age.avg())
                                .from(memberSUb))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test // case문
    public void basicCase(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test // 상수, 문자 더하기
    public void constant(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    @Test
    public void concat(){
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

//    ========================================================================================== //





    // Querydsl 중급 문법 //

    // 프로젝션과 결과 반환 -기본
    // Tuple은 외부로 노출 되는 것은 바람직하지 않음(querydsl에서 제공해주는 tuple이기 때문)
    @Test
    public void eimpleProjection(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    // 프로젝션과 결과 반환 - DTO 조회 //
    @Test // 순수 JPA, new 명령어를 사용해야함.
    public void findDtoByJPQL(){
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) " +
                "from Member m", MemberDto.class).getResultList();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    // setter를 이용한 방법
    @Test
    public void findDtoBySetter(){
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    // field로 생성하는 방법. getter, setter가 필요없음.
    @Test
    public void findDtoByField(){
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    // constructor로 생성하는 방법. dto의 속성 타입과 불러올 데이터의 타입을 맞춰주어야함.
    @Test
    public void findDtoByConstructor(){
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    @Test
    public void findUserDto(){
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
//                        member.username, // 이렇게 사용하는 경우 UserDto에 name 필드엔 아무것도 들어가지 않게 됨.
                        member.username.as("name"), // name 필드 값과 일치시켜주어야함.
                        member.age))
                .from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
        // 프로퍼티나, 필드 접근 생성 방식에서 이름이 다를 때 해결 방안2
        // 만약 서브 쿼리를 주고 싶은 경우엔 ExpressionsUtils를 사용해줌.
        List<UserDto> result2 = queryFactory
                .select(Projections.fields(UserDto.class,
                                member.username.as("name"),
                                ExpressionUtils.as(JPAExpressions
                                        .select(memberSub.age.max())
                                        .from(memberSub),"age"))
                )
                .from(member)
                .fetch();
        for (UserDto userDto : result2) {
            System.out.println("userDto2 = " + userDto);
        }
    }

    // 프로젝션과 결과 반환 - @QueryProjection //
    // 장점 : 컴파일 전에 오류를 찾을 수 있음.
    // 단점 : dto가 쿼리dsl를 의존해야하는 단점이 있음.
    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 동적 쿼리 - BooleanBuilder //
    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember1(usernameParam,ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);

    }
    private List<Member> searchMember1(String usernameCond, Integer ageCond){
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }
        if(ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }
    // 동적 쿼리 - Where 다중 파라미터 사용
    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember2(usernameParam,ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
//                .where(usernameEq(usernameCond),ageEq(ageCond))
                .where(allEq(usernameCond,ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        if(usernameCond == null) {
            return null;
        }
        return member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq(Integer ageCond) {
        if(ageCond == null){
            return null;
        }
        return member.age.eq(ageCond);
    }
    private Predicate allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    // 수정, 삭제 배치 쿼리
    @Test
    @Commit
    public void bulkupdate(){

        // member1 = 10 -> DB member1
        // member2 = 20 -> DB member2
        // member3 = 30 -> DB member3
        // member4 = 40 -> DB member4


        queryFactory
                .update(member)
                .set(member.username,"비회원")
                .where(member.age.lt(28))
                .execute();
        // 영속성 컨텍스트를 무시하고 DB에 쿼리를 날림.
        // 만약 영속성 컨텍스트에 이름이 member1인 member1과 이름이 member2인 member2가 있다면?

        // member1 = 10 -> DB 비회원 / 1차 캐시 member1
        // member2 = 20 -> DB 비회원 / 1차 캐시 member2
        // member3 = 30 -> DB member3
        // member4 = 40 -> DB member4
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }

        em.flush();
        em.clear();
    }
    @Test
    public void bulkAdd(){
        queryFactory
                .update(member)
                .set(member.age,member.age.add(1)) // member.age에 1살씩 추가. (곱하기는 member.age.multiply(2))
                .execute();
    }
    @Test
    public void bulkDelete(){
        queryFactory
                .delete(member)
                .where(member.age.gt(18)) // 18살 모두 지워.
                .execute();
    }

    // SQL function 호출하기
    @Test
    public void sqlFunction(){
        // H2 DB에 replace 함수가 등록돼있어야함.
        // member.username의 member를 M으로 바꾸는 함수
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})",
                                member.username, "member", "M"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    @Test
    public void sqlFunction2(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower',{0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }

    }
}
