package com.vectoros.server.reminder.controllers;

import com.vectoros.server.reminder.entity.ReminderEntity;
import com.vectoros.server.reminder.service.ReminderService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@AllArgsConstructor
@RequestMapping("/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping
    public ReminderEntity createReminder(@RequestBody ReminderEntity reminder) {
        return reminderService.createReminder(reminder);
    }

    @GetMapping
    public ResponseEntity<List<ReminderEntity>> getAllUserReminders(@RequestParam Long telegramId) {
        return ResponseEntity.ok(reminderService.getAllUserReminders(telegramId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReminderEntity> getReminderById(@PathVariable Long id) {
        Optional<ReminderEntity> reminder = reminderService.getReminderById(id);
        return reminder.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

