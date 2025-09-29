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

public class KickCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(KickCommand.class);

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public String getDescription() {
        return "Kick a user from the server";
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
        return new String[]{"KICK_MEMBERS"};
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("❌ This command can only be used in a server!").setEphemeral(true).queue();
            return;
        }
        Member executor = event.getMember();
        if (executor == null || !executor.hasPermission(Permission.KICK_MEMBERS)) {
            event.reply("❌ You need the `Kick Members` permission to use this command!").setEphemeral(true).queue();
            return;
        }
        User targetUser = event.getOption("user").getAsUser();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "No reason provided";
        Member targetMember = event.getGuild().getMember(targetUser);
        if (targetMember == null) {
            event.reply("❌ User is not in this server!").setEphemeral(true).queue();
            return;
        }
        if (!canKickUser(executor, targetMember, event)) {
            return;
        }

        // Proceed to kick
        event.getGuild().kick(targetMember, reason).queue(
                success -> {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("👢 User Kicked")
                            .setColor(Color.YELLOW)
                            .addField("User", targetUser.getAsTag(), true)
                            .addField("Moderator", executor.getUser().getAsTag(), true)
                            .addField("Reason", reason, false)
                            .setFooter("Action performed by ByteGuard", null)
                            .setTimestamp(Instant.now());

                    event.replyEmbeds(embed.build()).queue();
                    sendKickDM(targetMember, executor, event.getGuild().getName(), reason);
                    logger.info("Kick issued: {} kicked {} for '{}'", executor.getUser().getAsTag(), targetUser.getAsTag(), reason);
                },
                error -> {
                    event.reply("❌ Failed to kick the user: " + error.getMessage()).setEphemeral(true).queue();
                    logger.warn("Kick failed: {} tried to kick {}: {}", executor.getUser().getAsTag(), targetUser.getAsTag(), error);
                }
        );
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), getDescription())
                .addOption(OptionType.USER, "user", "User to kick", true)
                .addOption(OptionType.STRING, "reason", "Reason for kick", false);
    }

    private boolean canKickUser(Member executor, Member target, SlashCommandInteractionEvent event) {
        if (executor.equals(target)) {
            event.reply("❌ You cannot kick yourself!").setEphemeral(true).queue();
            return false;
        }
        if (target.getUser().isBot()) {
            event.reply("❌ You cannot kick bots!").setEphemeral(true).queue();
            return false;
        }
        if (target.isOwner()) {
            event.reply("❌ You cannot kick the server owner!").setEphemeral(true).queue();
            return false;
        }
        if (!executor.canInteract(target)) {
            event.reply("❌ You cannot kick this user due to role hierarchy!").setEphemeral(true).queue();
            return false;
        }
        Member botMember = event.getGuild().getSelfMember();
        if (!botMember.canInteract(target)) {
            event.reply("❌ I cannot kick this user due to role hierarchy!").setEphemeral(true).queue();
            return false;
        }
        return true;
    }

    private void sendKickDM(Member target, Member executor, String guildName, String reason) {
        EmbedBuilder dmEmbed = new EmbedBuilder()
                .setTitle("👢 You have been kicked from " + guildName)
                .setColor(Color.YELLOW)
                .addField("Moderator", executor.getUser().getAsTag(), true)
                .addField("Reason", reason, false)
                .setFooter("This action was performed by ByteGuard")
                .setTimestamp(Instant.now());

        target.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(dmEmbed.build()))
                .queue(
                        success -> logger.debug("Kick DM sent to {}", target.getUser().getAsTag()),
                        error -> logger.debug("Could not send kick DM to {}: {}", target.getUser().getAsTag(), error.getMessage())
                );
    }
}
