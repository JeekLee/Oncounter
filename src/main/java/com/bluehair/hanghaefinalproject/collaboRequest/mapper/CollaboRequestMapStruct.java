package com.bluehair.hanghaefinalproject.collaboRequest.mapper;

import com.bluehair.hanghaefinalproject.collaboRequest.dto.CollaboRequestDetailsDto;
import com.bluehair.hanghaefinalproject.collaboRequest.dto.CollaboRequestDto;
import com.bluehair.hanghaefinalproject.collaboRequest.dto.CollaboRequestListForPostDto;
import com.bluehair.hanghaefinalproject.collaboRequest.entity.CollaboRequest;
import com.bluehair.hanghaefinalproject.post.entity.Post;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CollaboRequestMapStruct {
    CollaboRequestMapStruct COLLABOREQUEST_MAPPER = Mappers.getMapper(CollaboRequestMapStruct.class);

    CollaboRequestDto CollaboRequestDetailsDtotoCollaboRequestDto(CollaboRequestDetailsDto collaboRequestDetailsDto, String nickname);
    CollaboRequestDto CollaboRequestDetailsDtotoUpdate(CollaboRequestDetailsDto collaboRequestDetailsDto);

    default CollaboRequest CollaboRequestDtotoCollaboRequest(CollaboRequestDto collaboRequestDto, Post post){
        return CollaboRequest.builder()
                .contents(collaboRequestDto.getContents())
                .activated(collaboRequestDto.getActivated())
                .approval(collaboRequestDto.getApproval())
                .nickname(collaboRequestDto.getNickname())
                .post(post)
                .build();
    }

    default CollaboRequestListForPostDto CollaboRequestListtoCollaboRequestListDto(CollaboRequest collaboRequest,
                                                                                   Long follwerCount,
                                                                                   Boolean isFollowed,
                                                                                   String profileImg,
                                                                                   List<String> musicPartsList){
        return CollaboRequestListForPostDto.builder()
                .collaboId(collaboRequest.getId())
                .nickname(collaboRequest.getNickname())
                .profileImg(profileImg)
                .followerCount(follwerCount)
                .isFollowed(isFollowed)
                .createdAt(collaboRequest.getCreatedAt())
                .modifiedAt(collaboRequest.getModifiedAt())
                .musicPartsList(musicPartsList)
                .build();
    }



}

