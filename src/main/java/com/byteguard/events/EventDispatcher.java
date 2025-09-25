package com.byteguard.events;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central event dispatcher for ByteGuard.
 * Handles and routes Discord events to appropriate handlers.
 */
public class EventDispatcher extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(EventDispatcher.class);

    public EventDispatcher() {
        logger.info("EventDispatcher initialized");
    }

    // Additional event handling can be added here as needed
    // This class serves as a central hub for event processing
}