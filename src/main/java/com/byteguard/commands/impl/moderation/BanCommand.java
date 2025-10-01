package com.byteguard.commands.impl.moderation;

import com.byteguard.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BanCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(BanCommand.class);

    // Single-threaded scheduler for delayed unbans (scales for now – may need distributed/work queue at huge scale)
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public String getName() {
        return "ban";
    }

    @Override
    public String getDescription() {
        return "Ban a user from the server (permanently or temporarily)";
    }

    @Override
    public String getCategory() {
        return "Moderation";
    }

    @Override
    public boolean requiresPermissions() {
        return true;
    }

    @Override
    public String[] getRequiredPermissions() {
        return new String[]{"BAN_MEMBERS"};
    }

    /**
     * Handles the /ban command.
     * If "duration" option is set, executes a tempban (schedules an automatic unban).
     * If not set, executes a traditional permanent ban.
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("❌ This command can only be used in a server!").setEphemeral(true).queue();
            return;
        }

        Member executor = event.getMember();
        if (executor == null || !executor.hasPermission(Permission.BAN_MEMBERS)) {
            event.reply("❌ You need the `Ban Members` permission to use this command!").setEphemeral(true).queue();
            return;
        }

        // Required ban target
        User targetUser = event.getOption("user").getAsUser();

        // Optional reason for the ban
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "No reason provided";
        // Optional duration for tempban (e.g. "1d", "2h")
        OptionMapping durationOption = event.getOption("duration");
        String duration = durationOption != null ? durationOption.getAsString() : null;

        Member targetMember = event.getGuild().getMember(targetUser);

        if (targetMember == null) {
            event.reply("❌ User is not in this server!").setEphemeral(true).queue();
            return;
        }

        // Prevents role hierarchy/logic abuse
        if (!canBanUser(executor, targetMember, event)) {
            return;
        }

        // TEMPBAN: If duration was set, schedule automatic unban
        if (duration != null) {
            executeTempBan(event, targetUser, targetMember, executor, reason, duration);
        } else {
            // PERMABAN: Just a normal ban
            executePermanentBan(event, targetUser, targetMember, executor, reason);
        }
    }

    /**
     * Registers slash command with "duration" as an optional field.
     */
    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), getDescription())
                .addOption(OptionType.USER, "user", "User to ban", true)
                .addOption(OptionType.STRING, "reason", "Reason for ban", false)
                .addOption(OptionType.STRING, "duration", "Duration for temp ban (e.g., 1d, 2h, 30m; optional)", false);
    }

    /**
     * Handles a tempban with duration string and schedules auto-unban.
     * Explanation:
     * - Parses duration string
     * - Bans the user
     * - Schedules unban after that period
     * - TODO: Persist tempban info for durability! See below
     */
    private void executeTempBan(SlashCommandInteractionEvent event, User targetUser, Member targetMember,
                                Member executor, String reason, String durationStr) {
        long durationSeconds = parseDurationToSeconds(durationStr);

        if (durationSeconds == -1) {
            event.reply("❌ Invalid duration format! Use: `1d`, `2h`, `30m`, or combos like `1d2h30m`")
                    .setEphemeral(true).queue();
            return;
        }

        event.getGuild().ban(Collections.singleton(targetUser), Duration.ZERO)
                .reason(reason + " (Temporary - " + durationStr + ")")
                .queue(
                        (success) -> {
                            // Schedules the unban using scheduler provided below
                            scheduleUnban(event.getJDA().getSelfUser().getIdLong(), event.getGuild().getIdLong(), targetUser.getId(), durationSeconds, event);

                            // TODO: Persist this scheduled tempban in a database for restart durability!
                            // storeTempBan(guildId, userId, unbanTimeMillis, reason);

                            EmbedBuilder embed = new EmbedBuilder()
                                    .setTitle("🔨 User Temporarily Banned")
                                    .setColor(Color.ORANGE)
                                    .addField("User", targetUser.getAsTag(), true)
                                    .addField("Moderator", executor.getUser().getAsTag(), true)
                                    .addField("Duration", durationStr, true)
                                    .addField("Reason", reason, false)
                                    .addField("Unban Time", String.format("<t:%d:F>",
                                            (System.currentTimeMillis() / 1000) + durationSeconds), false)
                                    .setFooter("Action performed by ByteGuard", null)
                                    .setTimestamp(Instant.now());

                            event.replyEmbeds(embed.build()).queue();
                            sendTempBanDM(targetMember, executor, event.getGuild().getName(), reason, durationStr);
                            logger.info("Temp ban issued: {} banned {} for '{}' duration: {}",
                                    executor.getUser().getAsTag(), targetUser.getAsTag(), reason, durationStr);
                        },
                        (error) -> {
                            String msg = (error != null && error.getMessage() != null)
                                    ? error.getMessage()
                                    : "Unknown error";
                            event.reply("❌ Failed to ban the user: " + msg).setEphemeral(true).queue();
                            logger.warn("Temp ban failed: {} tried to ban {}: {}",
                                    executor.getUser().getAsTag(), targetUser.getAsTag(), msg);
                        }
                );
    }

    /**
     * Handles a permanent ban (same as your original logic, just slightly refactored for reuse).
     */
    private void executePermanentBan(SlashCommandInteractionEvent event, User targetUser, Member targetMember,
                                     Member executor, String reason) {
        event.getGuild().ban(Collections.singleton(targetUser), Duration.ZERO)
                .reason(reason)
                .queue(
                        (success) -> {
                            EmbedBuilder embed = new EmbedBuilder()
                                    .setTitle("🔨 User Banned")
                                    .setColor(Color.RED)
                                    .addField("User", targetUser.getAsTag(), true)
                                    .addField("Moderator", executor.getUser().getAsTag(), true)
                                    .addField("Reason", reason, false)
                                    .setFooter("Action performed by ByteGuard", null)
                                    .setTimestamp(Instant.now());

                            event.replyEmbeds(embed.build()).queue();
                            sendBanDM(targetMember, executor, event.getGuild().getName(), reason);
                            logger.info("Ban issued: {} banned {} for '{}'",
                                    executor.getUser().getAsTag(), targetUser.getAsTag(), reason);
                        },
                        (error) -> {
                            String msg = (error != null && error.getMessage() != null)
                                    ? error.getMessage()
                                    : "Unknown error";
                            event.reply("❌ Failed to ban the user: " + msg).setEphemeral(true).queue();
                            logger.warn("Ban failed: {} tried to ban {}: {}",
                                    executor.getUser().getAsTag(), targetUser.getAsTag(), msg);
                        }
                );
    }

    /**
     * Parses a string like "1d2h30m" or "45m" into seconds.
     * Use format: days(d), hours(h), minutes(m), or seconds(s).
     * Returns -1 if nothing valid.
     */
    private long parseDurationToSeconds(String duration) {
        if (duration == null || duration.isEmpty()) return -1;
        Pattern pattern = Pattern.compile("(\\d+)([dhms])");
        Matcher matcher = pattern.matcher(duration.toLowerCase());

        long totalSeconds = 0;
        boolean foundMatch = false;

        while (matcher.find()) {
            foundMatch = true;
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "d": totalSeconds += (long) value * 24 * 60 * 60; break;
                case "h": totalSeconds += (long) value * 60 * 60; break;
                case "m": totalSeconds += (long) value * 60; break;
                case "s": totalSeconds += value; break;
            }
        }

        return foundMatch && totalSeconds > 0 ? totalSeconds : -1;
    }

    /**
     * Schedules an unban for tempbans using ScheduledExecutorService.
     * NOTE: For production, this should be moved to a persistent queue so bans survive restarts/crashes!
     *
     * @param botId unused here (included for possible multi-bot use)
     * @param guildId Server to unban in
     * @param userId User ID string to unban
     * @param delaySeconds How many seconds to delay
     * @param event The command interaction (for logger use)
     */
    private void scheduleUnban(long botId, long guildId, String userId, long delaySeconds, SlashCommandInteractionEvent event) {
        scheduler.schedule(() -> {
            try {
                // Use User.fromId() to convert String userId to UserSnowflake
                event.getJDA().getGuildById(guildId).unban(User.fromId(userId)).queue(
                        success -> logger.info("Auto-unbanned user {} from guild {}", userId, guildId),
                        failure -> logger.warn("Failed to auto-unban user {} from guild {}: {}",
                                userId, guildId, failure.getMessage())
                );
            } catch (Exception e) {
                logger.error("Error during scheduled unban for user {} in guild {}: {}",
                        userId, guildId, e.getMessage());
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * Checks if executor is allowed to ban (role logic etc).
     * Returns false and responds if action is forbidden.
     */
    private boolean canBanUser(Member executor, Member target, SlashCommandInteractionEvent event) {
        if (executor.equals(target)) {
            event.reply("❌ You cannot ban yourself!").setEphemeral(true).queue();
            return false;
        }
        if (target.getUser().isBot()) {
            event.reply("❌ You cannot ban bots!").setEphemeral(true).queue();
            return false;
        }
        if (target.isOwner()) {
            event.reply("❌ You cannot ban the server owner!").setEphemeral(true).queue();
            return false;
        }
        if (!executor.canInteract(target)) {
            event.reply("❌ You cannot ban this user due to role hierarchy!").setEphemeral(true).queue();
            return false;
        }
        Member botMember = event.getGuild().getSelfMember();
        if (!botMember.canInteract(target)) {
            event.reply("❌ I cannot ban this user due to role hierarchy!").setEphemeral(true).queue();
            return false;
        }
        return true;
    }

    /**
     * Sends a DM to a user on permaban (notifies why/which server).
     */
    private void sendBanDM(Member target, Member executor, String guildName, String reason) {
        EmbedBuilder dmEmbed = new EmbedBuilder()
                .setTitle("🔨 You have been banned from " + guildName)
                .setColor(Color.RED)
                .addField("Moderator", executor.getUser().getAsTag(), true)
                .addField("Reason", reason, false)
                .setFooter("This action was performed by ByteGuard")
                .setTimestamp(Instant.now());

        target.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(dmEmbed.build()))
                .queue(
                        success -> logger.debug("Ban DM sent to {}", target.getUser().getAsTag()),
                        error -> logger.debug("Could not send ban DM to {}: {}", target.getUser().getAsTag(), error.getMessage())
                );
    }

    /**
     * Sends a DM to a user on tempban (shows duration, auto-unban info).
     */
    private void sendTempBanDM(Member target, Member executor, String guildName, String reason, String duration) {
        EmbedBuilder dmEmbed = new EmbedBuilder()
                .setTitle("🔨 You have been temporarily banned from " + guildName)
                .setColor(Color.ORANGE)
                .addField("Moderator", executor.getUser().getAsTag(), true)
                .addField("Duration", duration, true)
                .addField("Reason", reason, false)
                .addField("You will be unbanned automatically", "No further action needed", false)
                .setFooter("This action was performed by ByteGuard")
                .setTimestamp(Instant.now());

        target.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(dmEmbed.build()))
                .queue(
                        success -> logger.debug("Temp ban DM sent to {}", target.getUser().getAsTag()),
                        error -> logger.debug("Could not send temp ban DM to {}: {}", target.getUser().getAsTag(), error.getMessage())
                );
    }
}
