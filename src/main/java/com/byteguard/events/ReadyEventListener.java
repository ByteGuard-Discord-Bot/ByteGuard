package com.byteguard.events;

import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the ReadyEvent when the bot comes online.
 * Logs bot statistics and connection information.
 */
public class ReadyEventListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ReadyEventListener.class);

    @Override
    public void onReady(ReadyEvent event) {
        logger.info("🚀 ByteGuard is now ready!");
        logger.info("📊 Bot Statistics:");
        logger.info("   • Logged in as: {}#{}",
                event.getJDA().getSelfUser().getName(),
                event.getJDA().getSelfUser().getDiscriminator());
        logger.info("   • Connected to {} guilds", event.getGuildTotalCount());
        logger.info("   • Serving {} users",
                event.getJDA().getGuilds().stream()
                        .mapToInt(guild -> guild.getMemberCount())
                        .sum());
        logger.info("   • Gateway Ping: {}ms", event.getJDA().getGatewayPing());
        logger.info("   • Shard: {}/{}",
                event.getJDA().getShardInfo().getShardId() + 1,
                event.getJDA().getShardInfo().getShardTotal());
        logger.info("✅ All systems operational - ByteGuard is protecting your servers!");
    }
}