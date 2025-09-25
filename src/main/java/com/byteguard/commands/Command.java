package com.byteguard.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

/**
 * Base interface for all ByteGuard commands.
 * Provides structure for command implementation.
 */
public interface Command {

    /**
     * Get the command name.
     *
     * @return Command name
     */
    String getName();

    /**
     * Get the command description.
     *
     * @return Command description
     */
    String getDescription();

    /**
     * Get the command category.
     *
     * @return Command category
     */
    default String getCategory() {
        return "General";
    }

    /**
     * Check if command requires permissions.
     *
     * @return true if requires permissions
     */
    default boolean requiresPermissions() {
        return false;
    }

    /**
     * Get required permissions.
     *
     * @return Array of required permissions
     */
    default String[] getRequiredPermissions() {
        return new String[0];
    }

    /**
     * Check if command is enabled.
     *
     * @return true if enabled
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Execute the command.
     *
     * @param event SlashCommandInteractionEvent
     */
    void execute(SlashCommandInteractionEvent event);

    /**
     * Get the JDA CommandData for registration.
     *
     * @return CommandData object
     */
    CommandData getCommandData();
}