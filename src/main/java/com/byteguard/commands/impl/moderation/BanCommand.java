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
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

public class BanCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(BanCommand.class);

    @Override
    public String getName() {
        return "ban";
    }

    @Override
    public String getDescription() {
        return "Ban a user from the server";
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
        User targetUser = event.getOption("user").getAsUser();
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "No reason provided";
        Member targetMember = event.getGuild().getMember(targetUser);

        if (targetMember == null) {
            event.reply("❌ User is not in this server!").setEphemeral(true).queue();
            return;
        }
        if (!canBanUser(executor, targetMember, event)) {
            return;
        }

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
                            logger.info("Ban issued: {} banned {} for '{}'", executor.getUser().getAsTag(), targetUser.getAsTag(), reason);
                        },
                        (error) -> {
                            String msg = (error != null && error.getMessage() != null)
                                    ? error.getMessage()
                                    : "Unknown error";
                            event.reply("❌ Failed to ban the user: " + msg).setEphemeral(true).queue();
                            logger.warn("Ban failed: {} tried to ban {}: {}", executor.getUser().getAsTag(), targetUser.getAsTag(), msg);
                        }
                );
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), getDescription())
                .addOption(OptionType.USER, "user", "User to ban", true)
                .addOption(OptionType.STRING, "reason", "Reason for ban", false);
    }

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
}
