package com.byteguard.events;

import com.byteguard.commands.Command;
import com.byteguard.core.BotManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles slash command interactions for ByteGuard.
 * Routes commands to appropriate handlers and manages execution.
 *
 * @author ByteGuard Team
 * @version 1.0.0
 * @since 2025-09-23
 */
public class SlashCommandListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SlashCommandListener.class);
    private final BotManager botManager;

    public SlashCommandListener(BotManager botManager) {
        this.botManager = botManager;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName().toLowerCase();

        logger.debug("Received slash command: {} from user: {} in guild: {}",
                commandName,
                event.getUser().getAsTag(),
                event.getGuild() != null ? event.getGuild().getName() : "DM");

        Command command = botManager.getCommand(commandName);

        if (command == null) {
            logger.warn("Unknown command received: {}", commandName);
            event.reply("❌ Unknown command. Use `/help` to see available commands.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!command.isEnabled()) {
            logger.debug("Disabled command attempted: {}", commandName);
            event.reply("❌ This command is currently disabled.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        try {
            // Execute command asynchronously to prevent blocking
            botManager.getThreadPool().execute(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    command.execute(event);
                    long executionTime = System.currentTimeMillis() - startTime;

                    logger.debug("Command {} executed in {}ms", commandName, executionTime);

                } catch (Exception e) {
                    logger.error("Error executing command: {}", commandName, e);

                    // Try to send error message if the interaction hasn't been acknowledged
                    try {
                        if (!event.isAcknowledged()) {
                            event.reply("❌ An error occurred while executing this command.")
                                    .setEphemeral(true)
                                    .queue();
                        } else {
                            event.getHook().editOriginal("❌ An error occurred while executing this command.")
                                    .queue();
                        }
                    } catch (Exception hookError) {
                        logger.error("Failed to send error message for command: {}", commandName, hookError);
                    }
                }
            });

        } catch (Exception e) {
            logger.error("Failed to submit command for execution: {}", commandName, e);
            event.reply("❌ Command execution failed.")
                    .setEphemeral(true)
                    .queue();
        }
    }
}