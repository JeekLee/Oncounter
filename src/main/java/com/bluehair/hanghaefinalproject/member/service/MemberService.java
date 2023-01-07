package com.bluehair.hanghaefinalproject.member.service;

import com.bluehair.hanghaefinalproject.common.service.Validator;
import com.bluehair.hanghaefinalproject.member.dto.responseDto.ResponseMemberInfoDto;
import com.bluehair.hanghaefinalproject.member.dto.serviceDto.*;
import com.bluehair.hanghaefinalproject.member.entity.Follow;
import com.bluehair.hanghaefinalproject.member.entity.FollowCompositeKey;
import com.bluehair.hanghaefinalproject.member.entity.Member;
import com.bluehair.hanghaefinalproject.member.exception.InvalidLoginRequestException;
import com.bluehair.hanghaefinalproject.member.exception.InvalidSignUpRequestException;
import com.bluehair.hanghaefinalproject.member.repository.FollowRepository;
import com.bluehair.hanghaefinalproject.member.repository.MemberRepository;
import com.bluehair.hanghaefinalproject.security.CustomUserDetails;
import com.bluehair.hanghaefinalproject.security.exception.CustomJwtException;
import com.bluehair.hanghaefinalproject.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.bluehair.hanghaefinalproject.common.response.error.ErrorCode.*;
import static com.bluehair.hanghaefinalproject.member.mapper.MemberMapStruct.MEMBER_MAPPER;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    private final JwtUtil jwtUtil;
    private final Validator validator;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final FollowRepository followRepository;

    @Transactional
    public void signUp(SignUpDto signUpDto) {
        memberRepository.findByEmail(signUpDto.getEmail())
                .ifPresent(m->{
                    throw new InvalidSignUpRequestException(DUPLICATED_EMAIL);
                });
        memberRepository.findByNickname(signUpDto.getNickname())
                .ifPresent(m-> {
                    throw new InvalidSignUpRequestException(DUPLICATED_NICKNAME);
                });
        if(!validator.isValidEmail(signUpDto.getEmail())){
            throw new InvalidSignUpRequestException(INVALID_EMAIL);
        }
        if(!validator.isValidPassword(signUpDto.getPassword())){
            throw new InvalidSignUpRequestException(INVALID_PASSWORD);
        }

        signUpDto.encryptPassword(passwordEncoder.encode(signUpDto.getPassword()));

        if (signUpDto.getProfileImg() == null) {
            signUpDto.setRandomProfileImg();
        }

        Member member = MEMBER_MAPPER.SignUpDtoToMember(signUpDto);
        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public void validateEmail(ValidateEmailDto validateEmailDto) {
        if(!validator.isValidEmail(validateEmailDto.getEmail())){
            throw new InvalidSignUpRequestException(INVALID_EMAIL);
        }
        memberRepository.findByEmail(validateEmailDto.getEmail())
                .ifPresent(m->{
                    throw new InvalidSignUpRequestException(DUPLICATED_EMAIL);
                });
    }
    @Transactional(readOnly = true)
    public void validateNickname(ValidateNicknameDto validateNicknameDto){
        memberRepository.findByNickname(validateNicknameDto.getNickname())
                .ifPresent(m->{
                    throw new InvalidSignUpRequestException(DUPLICATED_NICKNAME);
                });
    }


    private void setNewTokens(HttpServletResponse response, Member member) {
        response.addHeader(JwtUtil.AUTHORIZATION_ACCESS, jwtUtil.createAccessToken(member.getEmail(), member.getRole()));
        String refreshToken = jwtUtil.createRefreshToken();
        response.addHeader(JwtUtil.AUTHORIZATION_REFRESH, refreshToken);
        member.updateToken(refreshToken);
    }

    @Transactional
    public void login(LoginDto loginDto, HttpServletResponse response) {
        Member member = memberRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(()->new InvalidLoginRequestException(MEMBER_NOT_FOUND));

        if(!passwordEncoder.matches(loginDto.getPassword(), member.getPassword())){
            throw new InvalidLoginRequestException(PASSWORD_INCORRECT);
        }

        setNewTokens(response, member);
    }

    @Transactional
    public void tokenReissuance(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = jwtUtil.resolveToken(request, "AccessToken");
        Claims claims = getClaimsFromAccessToken(request, accessToken);

        String refreshToken = jwtUtil.resolveToken(request, "RefreshToken");
        jwtUtil.validateToken(refreshToken, false);

        Member member = memberRepository.findByEmail(claims.getSubject())
                .orElseThrow(()->new InvalidSignUpRequestException(MEMBER_NOT_FOUND));

        if(!member.getRefreshToken().substring(7).equals(refreshToken)){
            throw new CustomJwtException(INVALID_REFRESHTOKEN);
        }

        setNewTokens(response, member);
    }

    private Claims getClaimsFromAccessToken(HttpServletRequest request, String accessToken) {
        try {
            jwtUtil.validateToken(accessToken, true);
        }
        catch (CustomJwtException e){
            if(e.getErrorCode().getCustomHttpStatusCode() == 4015) {
                return jwtUtil.getUserInfoFromHttpServletRequest(request, true);
            }
        }
        jwtUtil.validateToken(accessToken, true);
        return null;
    }

    @Transactional
    public ResponseMemberInfoDto memberInfo(CustomUserDetails userDetails){
        return ResponseMemberInfoDto.builder()
                .email(userDetails.getMember().getEmail())
                .nickname(userDetails.getMember().getNickname())
                .profileImg(userDetails.getMember().getProfileImg())
                .build();
    }

    @Transactional
    public void doFollow(CustomUserDetails userDetails, FollowDto followDto) {
        Member myFollowingMember = memberRepository.findByNickname(followDto.getMyFollowingMemberNickname())
                .orElseThrow(()-> new InvalidLoginRequestException(MEMBER_NOT_FOUND));

        FollowCompositeKey followCompositeKey
                = new FollowCompositeKey(userDetails.getMember().getId(), myFollowingMember.getId());

        if (followRepository.existsById(followCompositeKey)){
            throw new InvalidLoginRequestException(ALREADY_FOLLWED);
        }

        Follow follow = new Follow(followCompositeKey, userDetails.getMember(), myFollowingMember);
        followRepository.save(follow);

        memberRepository.updateFollowingCount(userDetails.getMember().getFollowingCount()+1, userDetails.getMember().getId());
        memberRepository.updateFollowerCount(myFollowingMember.getFollowerCount()+1, myFollowingMember.getId());
    }

    @Transactional
    public void doUnfollow(CustomUserDetails userDetails, FollowDto followDto) {
        Member myFollowingMember = memberRepository.findByNickname(followDto.getMyFollowingMemberNickname())
                .orElseThrow(()-> new InvalidLoginRequestException(MEMBER_NOT_FOUND));

        FollowCompositeKey followCompositeKey
                = new FollowCompositeKey(userDetails.getMember().getId(), myFollowingMember.getId());

        if (!followRepository.existsById(followCompositeKey)){
            throw new InvalidLoginRequestException(ALREADY_UNFOLLWED);
        }

        followRepository.deleteById(followCompositeKey);

        memberRepository.updateFollowingCount(userDetails.getMember().getFollowingCount()-1, userDetails.getMember().getId());
        memberRepository.updateFollowerCount(myFollowingMember.getFollowerCount()-1, myFollowingMember.getId());
    }
}
