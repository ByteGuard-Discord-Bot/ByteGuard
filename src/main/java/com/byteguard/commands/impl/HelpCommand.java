package com.byteguard.commands.impl;

import com.byteguard.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.Color;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced Help command implementation with category and command-specific help.
 */
public class HelpCommand implements Command {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Display available commands and bot information";
    }

    @Override
    public String getCategory() {
        return "Information";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String category = event.getOption("category") != null ?
                event.getOption("category").getAsString().toLowerCase() : null;
        String command = event.getOption("command") != null ?
                event.getOption("command").getAsString().toLowerCase() : null;

        if (category != null && command != null) {
            // Show specific command help
            showCommandHelp(event, command);
        } else if (category != null) {
            // Show category help
            showCategoryHelp(event, category);
        } else {
            // Show main help with categories
            showMainHelp(event);
        }
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), getDescription())
                .addOption(OptionType.STRING, "category", "Command category (utility, moderation, fun)", false)
                .addOption(OptionType.STRING, "command", "Specific command name", false);
    }

    /**
     * Show main help with available categories
     */
    private void showMainHelp(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🛡️ ByteGuard - Help Categories")
                .setDescription("Select a category to see available commands. Use `/help [category]` for more details.")
                .setColor(Color.BLUE)
                .setThumbnail(event.getJDA().getSelfUser().getAvatarUrl())
                .setTimestamp(Instant.now())
                .setFooter("ByteGuard v1.0.1", null);

        embed.addField("🔧 **Utility**",
                "Basic bot utilities and information commands\n" +
                        "*Use: `/help utility` to see commands*", false);

        embed.addField("🛡️ **Moderation**",
                "Server moderation and management commands\n" +
                        "*Use: `/help moderation` to see commands*", false);

        embed.addField("🎯 **Fun**",
                "Entertainment and interactive commands\n" +
                        "*Use: `/help fun` to see commands*", false);

        embed.addField("ℹ️ **Information**",
                "Server and user information commands\n" +
                        "*Use: `/help information` to see commands*", false);

        embed.addBlankField(false);
        embed.addField("💡 **Usage Examples:**",
                "`/help` - Show this menu\n" +
                        "`/help moderation` - Show moderation commands\n" +
                        "`/help moderation ban` - Show detailed ban command help", false);

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    /**
     * Show category-specific help
     */
    private void showCategoryHelp(SlashCommandInteractionEvent event, String category) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTimestamp(Instant.now())
                .setFooter("Use /help [category] [command] for detailed command info", null);

        switch (category) {
            case "utility":
                embed.setTitle("🔧 Utility Commands")
                        .setDescription("Basic bot utilities and system commands")
                        .addField("Available Commands:",
                                "`/ping` - Check bot latency and status\n" +
                                        "`/help` - Display help information\n", false);
                break;

            case "moderation":
                embed.setTitle("🛡️ Moderation Commands")
                        .setDescription("Server moderation and member management commands")
                        .addField("Available Commands:",
                                "`/warn <user> [reason]` - Issue a warning to a user\n" +
                                        "`/kick <user> [reason]` - Kick a user from the server\n" +
                                        "`/ban <user> [reason]` - Ban a user from the server\n", false)
                        .addField("Requirements:",
                                "• Appropriate permissions (Moderate Members, Kick Members, Ban Members)\n" +
                                        "• Cannot target users with higher roles\n" +
                                        "• Cannot target server owner or bots", false);
                break;

            case "fun":
                embed.setTitle("🎯 Fun Commands")
                        .setDescription("Entertainment and interactive commands")
                        .addField("Available Commands:",
                                "*Coming soon in Phase 2!*\n" +
                                        "• Games and mini-games\n" +
                                        "• Random generators\n" +
                                        "• Interactive activities", false);
                break;

            case "information":
                embed.setTitle("ℹ️ Information Commands")
                        .setDescription("Server and user information commands")
                        .addField("Available Commands:",
                                "*Coming soon in Phase 2!*\n" +
                                        "• Server statistics\n" +
                                        "• User profiles\n" +
                                        "• Role information", false);
                break;

            default:
                embed.setTitle("❌ Unknown Category")
                        .setDescription("Category not found. Available categories:")
                        .addField("Valid Categories:",
                                "• `utility` - Basic utilities\n" +
                                        "• `moderation` - Moderation tools\n" +
                                        "• `fun` - Entertainment commands\n" +
                                        "• `information` - Info commands", false)
                        .setColor(Color.RED);
                break;
        }

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    /**
     * Show specific command help
     */
    private void showCommandHelp(SlashCommandInteractionEvent event, String command) {
        Map<String, CommandInfo> commandDetails = getCommandDetails();
        CommandInfo info = commandDetails.get(command);

        EmbedBuilder embed = new EmbedBuilder()
                .setTimestamp(Instant.now())
                .setFooter("ByteGuard Command Help", null);

        if (info != null) {
            embed.setTitle("📖 Command: /" + command)
                    .setDescription(info.description)
                    .setColor(Color.CYAN)
                    .addField("Usage:", info.usage, false)
                    .addField("Category:", info.category, true)
                    .addField("Permissions:", info.permissions, true);

            if (!info.examples.isEmpty()) {
                embed.addField("Examples:", info.examples, false);
            }
        } else {
            embed.setTitle("❌ Command Not Found")
                    .setDescription("The command `/" + command + "` was not found.")
                    .setColor(Color.RED)
                    .addField("Available Commands:",
                            "Use `/help [category]` to see available commands in each category.", false);
        }

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    /**
     * Get detailed command information
     */
    private Map<String, CommandInfo> getCommandDetails() {
        Map<String, CommandInfo> commands = new HashMap<>();

        commands.put("ping", new CommandInfo(
                "Check bot latency and response time",
                "`/ping`",
                "Utility",
                "None",
                "`/ping` - Check current bot latency"
        ));

        commands.put("help", new CommandInfo(
                "Display help information and command usage",
                "`/help [category] [command]`",
                "Information",
                "None",
                "`/help` - Show categories\n" +
                        "`/help moderation` - Show moderation commands\n" +
                        "`/help moderation ban` - Show ban command details"
        ));

        commands.put("warn", new CommandInfo(
                "Issue a warning to a user with optional reason",
                "`/warn <user> [reason]`",
                "Moderation",
                "Moderate Members",
                "`/warn @user Spamming` - Warn user for spamming\n" +
                        "`/warn @user` - Warn user without specific reason"
        ));

        commands.put("kick", new CommandInfo(
                "Kick a user from the server with optional reason",
                "`/kick <user> [reason]`",
                "Moderation",
                "Kick Members",
                "`/kick @user Inappropriate behavior` - Kick user with reason\n" +
                        "`/kick @user` - Kick user without reason"
        ));

        commands.put("ban", new CommandInfo(
                "Ban a user from the server with optional reason",
                "`/ban <user> [reason]`",
                "Moderation",
                "Ban Members",
                "`/ban @user Severe rule violation` - Ban user with reason\n" +
                        "`/ban @user` - Ban user without reason"
        ));

        return commands;
    }

    /**
     * Helper class to store command information
     */
    private static class CommandInfo {
        final String description;
        final String usage;
        final String category;
        final String permissions;
        final String examples;

        CommandInfo(String description, String usage, String category, String permissions, String examples) {
            this.description = description;
            this.usage = usage;
            this.category = category;
            this.permissions = permissions;
            this.examples = examples;
        }
    }
}
