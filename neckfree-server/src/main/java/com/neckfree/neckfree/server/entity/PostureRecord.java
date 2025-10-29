package com.neckfree.neckfree.server.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "posture_record")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PostureRecord {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;          // 사용자 ID
    private double neckAngle;       // 예: 목 각도
    private String postureState;    // 예: "정상" / "거북목"
    private LocalDateTime recordedAt;

    @PrePersist
    void onCreate() { recordedAt = LocalDateTime.now(); }
}

