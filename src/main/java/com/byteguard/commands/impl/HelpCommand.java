package com.byteguard.commands.impl;

import com.byteguard.commands.Command;
import com.byteguard.core.BotManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.Color;
import java.time.Instant;
import java.util.Map;

/**
 * Help command implementation.
 * Shows available commands and bot information.
 *
 * @author ByteGuard Team
 * @version 1.0.0
 * @since 2025-09-23
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
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🛡️ ByteGuard - Help & Commands")
                .setDescription("ByteGuard is an enterprise-grade Discord moderation bot designed for professional server management.")
                .setColor(Color.BLUE)
                .setThumbnail(event.getJDA().getSelfUser().getAvatarUrl())
                .setTimestamp(Instant.now())
                .setFooter("ByteGuard v1.0.0 - Phase 1", null);

        // Add command categories
        addUtilityCommands(embed);
        addInformationSection(embed);
        addSupportSection(embed);

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), getDescription());
    }

    /**
     * Add utility commands section to embed.
     *
     * @param embed EmbedBuilder to add to
     */
    private void addUtilityCommands(EmbedBuilder embed) {
        StringBuilder utilityCommands = new StringBuilder();
        utilityCommands.append("`/ping` - Check bot latency and status\n");
        utilityCommands.append("`/help` - Display this help message\n");

        embed.addField("🔧 Utility Commands", utilityCommands.toString(), false);
    }

    /**
     * Add information section to embed.
     *
     * @param embed EmbedBuilder to add to
     */
    private void addInformationSection(EmbedBuilder embed) {
        StringBuilder info = new StringBuilder();
        info.append("**Current Phase:** Phase 1 - Foundation\n");
        info.append("**Version:** 1.0.0-SNAPSHOT\n");
        info.append("**Language:** Java 17+ with JDA 5.0.2\n");
        info.append("**Architecture:** Enterprise-grade, scalable design\n");

        embed.addField("ℹ️ Bot Information", info.toString(), false);
    }

    /**
     * Add support section to embed.
     *
     * @param embed EmbedBuilder to add to
     */
    private void addSupportSection(EmbedBuilder embed) {
        StringBuilder support = new StringBuilder();
        support.append("**GitHub:** [ByteGuard Repository](https://github.com/ByteGuard-Discord-Bot/ByteGuard)\n");
        support.append("**Issues:** Report bugs and request features on GitHub\n");
        support.append("**Documentation:** Coming in Phase 2\n");

        embed.addField("🆘 Support & Links", support.toString(), false);
    }
}