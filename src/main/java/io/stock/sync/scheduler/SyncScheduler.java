package io.stock.sync.scheduler;

import io.stock.sync.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final SyncService syncService;
    private final boolean enabled;

    public SyncScheduler(SyncService syncService,
                         @Value("${sync.enabled:true}") boolean enabled) {
        this.syncService = syncService;
        this.enabled = enabled;
    }

    // every minute by default
    @Scheduled(cron = "${sync.cron:0 */1 * * * *}")
    public void run() {
        if (!enabled) {
            log.info("Sync scheduler disabled (sync.enabled=false)");
            return;
        }
        log.info("Starting scheduled stock sync...");
        syncService.syncAll();
        log.info("Finished scheduled stock sync.");
    }
}
