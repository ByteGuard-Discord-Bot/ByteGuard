package com.byteguard.commands.impl;

import com.byteguard.commands.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.Color;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Ping command implementation.
 * Shows bot latency and response time.
 */
public class PingCommand implements Command {

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getDescription() {
        return "Check bot latency and response time";
    }

    @Override
    public String getCategory() {
        return "Utility";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Defer reply to prevent timeout for async operations
        event.deferReply().queue();

        long responseTime = System.currentTimeMillis();

        CompletableFuture.runAsync(() -> {
            try {
                // Simulate some processing time for more realistic response time
                Thread.sleep(100);

                long gatewayPing = event.getJDA().getGatewayPing();
                long totalResponseTime = System.currentTimeMillis() - responseTime;

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("🏓 Pong!")
                        .setColor(getStatusColor(gatewayPing))
                        .addField("📊 Response Time", totalResponseTime + "ms", true)
                        .addField("🌐 Gateway Ping", gatewayPing + "ms", true)
                        .addField("✅ Status", getStatusText(gatewayPing), true)
                        .setFooter("ByteGuard v1.0.0", null)
                        .setTimestamp(Instant.now());

                event.getHook().editOriginalEmbeds(embed.build()).queue();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                event.getHook().editOriginal("❌ An error occurred while processing the ping command.").queue();
            } catch (Exception e) {
                event.getHook().editOriginal("❌ An unexpected error occurred.").queue();
            }
        });
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), getDescription());
    }

    /**
     * Get status color based on ping.
     *
     * @param ping Gateway ping in milliseconds
     * @return Color for embed
     */
    private Color getStatusColor(long ping) {
        if (ping < 100) {
            return Color.GREEN;  // Excellent
        } else if (ping < 200) {
            return Color.YELLOW; // Good
        } else if (ping < 500) {
            return Color.ORANGE; // Fair
        } else {
            return Color.RED;    // Poor
        }
    }

    /**
     * Get status text based on ping.
     *
     * @param ping Gateway ping in milliseconds
     * @return Status description
     */
    private String getStatusText(long ping) {
        if (ping < 100) {
            return "Online and ready!";
        } else if (ping < 200) {
            return "Good connection";
        } else if (ping < 500) {
            return "Moderate latency";
        } else {
            return "High latency";
        }
    }
}