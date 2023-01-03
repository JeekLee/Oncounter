package com.bluehair.hanghaefinalproject.music.entity;

import com.bluehair.hanghaefinalproject.collaboRequest.entity.CollaboRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@Entity
@NoArgsConstructor
public class Music {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String musicFile;

    @Column(nullable = false)
    private String musicPart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COLLABOREQUEST_ID")
    private CollaboRequest collaboRequest;
}
