package com.byteguard.commands.impl.moderation;

import com.byteguard.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.time.Instant;

/**
 * Warn command implementation.
 * Issues warnings to users with optional reasons and logging.
 */
public class WarnCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(WarnCommand.class);

    @Override
    public String getName() {
        return "warn";
    }

    @Override
    public String getDescription() {
        return "Issue a warning to a user";
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
        return new String[]{"MODERATE_MEMBERS"};
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Check if command is executed in a guild
        if (event.getGuild() == null) {
            event.reply("❌ This command can only be used in a server!").setEphemeral(true).queue();
            return;
        }

        // Check if user has required permissions
        Member executor = event.getMember();
        if (executor == null || !executor.hasPermission(Permission.MODERATE_MEMBERS)) {
            event.reply("❌ You need the `Moderate Members` permission to use this command!")
                    .setEphemeral(true).queue();
            return;
        }

        // Get command options
        User targetUser = event.getOption("user").getAsUser();
        String reason = event.getOption("reason") != null ?
                event.getOption("reason").getAsString() : "No reason provided";

        // Get target member
        Member targetMember = event.getGuild().getMember(targetUser);
        if (targetMember == null) {
            event.reply("❌ User is not in this server!").setEphemeral(true).queue();
            return;
        }

        // Validation checks
        if (!canWarnUser(executor, targetMember, event)) {
            return;
        }

        // Execute warning
        executeWarning(event, executor, targetMember, reason);
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), getDescription())
                .addOption(OptionType.USER, "user", "User to warn", true)
                .addOption(OptionType.STRING, "reason", "Reason for the warning", false);
    }

    /**
     * Check if the executor can warn the target user.
     */
    private boolean canWarnUser(Member executor, Member target, SlashCommandInteractionEvent event) {
        // Can't warn yourself
        if (executor.equals(target)) {
            event.reply("❌ You cannot warn yourself!").setEphemeral(true).queue();
            return false;
        }

        // Can't warn bots
        if (target.getUser().isBot()) {
            event.reply("❌ You cannot warn bots!").setEphemeral(true).queue();
            return false;
        }

        // Can't warn server owner
        if (target.isOwner()) {
            event.reply("❌ You cannot warn the server owner!").setEphemeral(true).queue();
            return false;
        }

        // Check role hierarchy
        if (!executor.canInteract(target)) {
            event.reply("❌ You cannot warn this user due to role hierarchy!")
                    .setEphemeral(true).queue();
            return false;
        }

        // Check if bot can interact with target
        Member botMember = event.getGuild().getSelfMember();
        if (!botMember.canInteract(target)) {
            event.reply("❌ I cannot warn this user due to role hierarchy!")
                    .setEphemeral(true).queue();
            return false;
        }

        return true;
    }

    /**
     * Execute the warning and send appropriate responses.
     */
    private void executeWarning(SlashCommandInteractionEvent event, Member executor,
                                Member target, String reason) {
        try {
            /**
             * What will be done later:
             * 1) Save the warning inside a database
             * 2) Check for automatic punishments (mute/kick after X warnings)
             * 3) Log to audit channel if configured
             * 4) Update user's warning count
             */

            // Log the warning
            logger.info("Warning issued: {} warned {} in {} for: {}",
                    executor.getUser().getAsTag(),
                    target.getUser().getAsTag(),
                    event.getGuild().getName(),
                    reason);

            // Create warning embed
            EmbedBuilder warningEmbed = new EmbedBuilder()
                    .setTitle("⚠️ Warning Issued")
                    .setColor(Color.ORANGE)
                    .addField("User", target.getUser().getAsTag(), true)
                    .addField("Moderator", executor.getUser().getAsTag(), true)
                    .addField("Reason", reason, false)
                    .setFooter("Warning ID: " + generateWarningId(), null)
                    .setTimestamp(Instant.now());

            // Send warning to channel
            event.replyEmbeds(warningEmbed.build()).queue();

            // Try to DM the user about the warning
            sendWarningDM(target, executor, event.getGuild().getName(), reason);


        } catch (Exception e) {
            logger.error("Error executing warn command", e);
            event.reply("❌ An error occurred while issuing the warning.")
                    .setEphemeral(true).queue();
        }
    }

    /**
     * Send a DM to the warned user.
     */
    private void sendWarningDM(Member target, Member executor, String guildName, String reason) {
        EmbedBuilder dmEmbed = new EmbedBuilder()
                .setTitle("⚠️ You have received a warning")
                .setColor(Color.ORANGE)
                .addField("Server", guildName, true)
                .addField("Moderator", executor.getUser().getAsTag(), true)
                .addField("Reason", reason, false)
                .setFooter("Please follow the server rules to avoid further warnings.")
                .setTimestamp(Instant.now());

        target.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(dmEmbed.build()))
                .queue(
                        success -> logger.debug("Warning DM sent to {}", target.getUser().getAsTag()),
                        error -> logger.debug("Could not send warning DM to {}: {}",
                                target.getUser().getAsTag(), error.getMessage())
                );
    }

    /**
     * Generate a unique warning ID (in real implementation, use database ID).
     */
    private String generateWarningId() {
        return "W" + System.currentTimeMillis() % 100000;
    }
}