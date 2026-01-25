package com.pekara.service;

import com.pekara.constant.RideStatus;
import com.pekara.model.Ride;
import com.pekara.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RideReminderScheduler {

    private final RideRepository rideRepository;
    private final MailService mailService;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void sendUpcomingRideReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime upper = now.plusMinutes(16);

        List<Ride> rides = rideRepository.findScheduledRidesStartingBefore(RideStatus.SCHEDULED, now, upper);
        for (Ride ride : rides) {
            if (ride.getScheduledAt() == null) {
                continue;
            }

            long minutesUntilStart = Duration.between(now, ride.getScheduledAt()).toMinutes();
            if (minutesUntilStart <= 0) {
                continue;
            }

            if (shouldSendReminder(ride, now, minutesUntilStart)) {
                try {
                    mailService.sendRideReminder(ride.getCreator().getEmail(), ride.getId(), ride.getScheduledAt());
                    ride.setLastReminderSentAt(now);
                    rideRepository.save(ride);
                } catch (Exception e) {
                    log.warn("Failed to send reminder for ride {}", ride.getId(), e);
                }
            }
        }
    }

    private boolean shouldSendReminder(Ride ride, LocalDateTime now, long minutesUntilStart) {
        // First reminder at T-15 (or the first scheduler tick after that)
        if (ride.getLastReminderSentAt() == null) {
            return minutesUntilStart <= 15;
        }

        // Then every 5 minutes after the first reminder, until start.
        long minutesSinceLast = Duration.between(ride.getLastReminderSentAt(), now).toMinutes();
        return minutesSinceLast >= 5;
    }
}
