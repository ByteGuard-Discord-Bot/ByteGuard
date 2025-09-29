package com.byteguard.commands.impl.moderation;

import com.byteguard.commands.Command;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

/**
 * Kick command implementation.
 * Kicks people from the server
 */
public class KickCommand implements Command {
    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public String getDescription() {
        return "Kicks a user from the server";
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
        if (!canKickUser(executor, targetMember, event)) {
            return;
        }

        // Execute kicking
        executeKicking(event, executor, targetMember, reason);
    }

    @Override
    public CommandData getCommandData() {

    }

    /**
     * Check if the executor can kick the target user.
     */
    private boolean canKickUser(Member executor, Member target, SlashCommandInteractionEvent event) {
        // Can't kick yourself
        if (executor.equals(target)) {
            event.reply("❌ You cannot kick yourself!").setEphemeral(true).queue();
            return false;
        }

        // Can't kick bots
        if (target.getUser().isBot()) {
            event.reply("❌ You cannot kick bots!").setEphemeral(true).queue();
            return false;
        }

        // Can't kick server owner
        if (target.isOwner()) {
            event.reply("❌ You cannot kick the server owner!").setEphemeral(true).queue();
            return false;
        }

        // Check role hierarchy
        if (!executor.canInteract(target)) {
            event.reply("❌ You cannot kick this user due to role hierarchy!")
                    .setEphemeral(true).queue();
            return false;
        }

        // Check if bot can interact with target
        Member botMember = event.getGuild().getSelfMember();
        if (!botMember.canInteract(target)) {
            event.reply("❌ I cannot kick this user due to role hierarchy!")
                    .setEphemeral(true).queue();
            return false;
        }

        return true;
    }

    private void executeKicking(SlashCommandInteractionEvent event, Member executor,
                                Member target, String reason) {

    }
}
