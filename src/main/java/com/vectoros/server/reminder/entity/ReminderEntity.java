package com.vectoros.server.reminder.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vectoros.server.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Data
@Entity
@Table(name = "reminders", schema = "vectoros",
        indexes = {
                @Index(name = "idx_reminders_user", columnList = "user_id"),
                @Index(name = "idx_reminders_time", columnList = "reminder_time"),
                @Index(name = "idx_reminders_status", columnList = "status"),
                @Index(name = "idx_reminders_next_time", columnList = "next_reminder_time")
        })
public class ReminderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReminderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReminderRepeatability repeatability;

    @Column(name = "reminder_time", nullable = false)
    private Instant reminderTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReminderStatus status = ReminderStatus.PENDING;

    @Column(name = "next_reminder_time")
    private Instant nextReminderTime;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reminder_user"))
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onPrePersist() {
        if (nextReminderTime == null) {
            nextReminderTime = reminderTime;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (status == null) {
            status = ReminderStatus.PENDING;
        }
    }
}

