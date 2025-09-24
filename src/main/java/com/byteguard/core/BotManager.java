package com.byteguard.core;

import com.byteguard.commands.Command;
import com.byteguard.commands.impl.HelpCommand;
import com.byteguard.commands.impl.PingCommand;
import com.byteguard.events.EventDispatcher;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Core bot management system for ByteGuard.
 * Handles command registration, event dispatching, and resource management.
 *
 * @author ByteGuard Team
 * @version 1.0.0
 * @since 2025-09-23
 */
public class BotManager {
    private static final Logger logger = LoggerFactory.getLogger(BotManager.class);

    private final Map<String, Command> commands;
    private final EventDispatcher eventDispatcher;
    private final ExecutorService threadPool;

    public BotManager() {
        logger.info("Initializing BotManager...");

        this.commands = new HashMap<>();
        this.eventDispatcher = new EventDispatcher();
        this.threadPool = Executors.newFixedThreadPool(5);

        loadCommands();

        logger.info("✅ BotManager initialized successfully");
    }

    /**
     * Load all available commands.
     */
    private void loadCommands() {
        logger.info("Loading commands...");

        // Phase 1 Commands
        registerCommand(new PingCommand());
        registerCommand(new HelpCommand());

        logger.info("✅ Loaded {} commands", commands.size());
    }

    /**
     * Register a command.
     *
     * @param command Command to register
     */
    private void registerCommand(Command command) {
        commands.put(command.getName().toLowerCase(), command);
        logger.debug("Registered command: {}", command.getName());
    }

    /**
     * Get a command by name.
     *
     * @param name Command name
     * @return Command instance or null if not found
     */
    public Command getCommand(String name) {
        return commands.get(name.toLowerCase());
    }

    /**
     * Get all registered commands.
     *
     * @return Map of all commands
     */
    public Map<String, Command> getCommands() {
        return new HashMap<>(commands);
    }

    /**
     * Register slash commands with Discord.
     *
     * @param jda JDA instance
     */
    public void registerCommands(JDA jda) {
        logger.info("Registering slash commands with Discord...");

        List<CommandData> commandData = commands.values().stream()
                .map(command -> {
                    logger.debug("Added command data for: {}", command.getName());
                    return command.getCommandData();
                })
                .toList();

        // Register commands globally (takes up to 1 hour to update)
        // For development, use jda.getGuildById("guildId").updateCommands() for instant updates
        jda.updateCommands().addCommands(commandData).queue(
                success -> logger.info("✅ Commands registered successfully"),
                error -> logger.error("❌ Failed to register commands", error)
        );
    }

    /**
     * Execute a command asynchronously.
     *
     * @param commandName Command name to execute
     * @param args Arguments for command execution
     * @return CompletableFuture for async execution
     */
    public CompletableFuture<Void> executeCommandAsync(String commandName, Object... args) {
        return CompletableFuture.runAsync(() -> {
            Command command = getCommand(commandName);
            if (command != null) {
                try {
                    // Command execution logic would go here
                    logger.debug("Executing command: {}", commandName);
                } catch (Exception e) {
                    logger.error("Error executing command: {}", commandName, e);
                }
            } else {
                logger.warn("Unknown command: {}", commandName);
            }
        }, threadPool);
    }

    /**
     * Get the event dispatcher.
     *
     * @return EventDispatcher instance
     */
    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    /**
     * Get the thread pool for async operations.
     *
     * @return ExecutorService thread pool
     */
    public ExecutorService getThreadPool() {
        return threadPool;
    }

    /**
     * Shutdown the bot manager and cleanup resources.
     */
    public void shutdown() {
        logger.info("Shutting down BotManager...");

        try {
            threadPool.shutdown();
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Thread pool did not terminate gracefully, forcing shutdown...");
                threadPool.shutdownNow();
            }

            logger.info("✅ BotManager shutdown complete");
        } catch (InterruptedException e) {
            logger.error("Error during BotManager shutdown", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get bot statistics.
     *
     * @return Map containing various bot statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("commandCount", commands.size());
        stats.put("activeThreads", ((Executors) threadPool).toString());
        // Add more statistics as needed
        return stats;
    }
}