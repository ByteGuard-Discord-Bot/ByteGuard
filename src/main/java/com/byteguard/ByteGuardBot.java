package com.byteguard;

import com.byteguard.config.ConfigManager;
import com.byteguard.core.BotManager;
import com.byteguard.events.ReadyEventListener;
import com.byteguard.events.SlashCommandListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the ByteGuard Discord Bot.
 * Initializes JDA, configures intents, and starts the bot.
 *
 * @author ByteGuard Team
 * @version 1.0.0
 * @since 2025-09-23
 */
public class ByteGuardBot {
    private static final Logger logger = LoggerFactory.getLogger(ByteGuardBot.class);
    private static JDA jda;
    private static BotManager botManager;

    public static void main(String[] args) {
        try {
            logger.info("🛡️ Starting ByteGuard Discord Bot v1.0.0");

            // Validate configuration
            ConfigManager.validateRequiredConfig();
            ConfigManager.printConfig();

            // Initialize bot manager
            botManager = new BotManager();

            // Build and start JDA instance
            jda = createJDAInstance();

            // Wait for JDA to be ready
            jda.awaitReady();

            // Register slash commands
            botManager.registerCommands(jda);

            logger.info("✅ ByteGuard is now online and ready!");

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(ByteGuardBot::shutdown));

        } catch (Exception e) {
            logger.error("❌ Failed to start ByteGuard", e);
            System.exit(1);
        }
    }

    /**
     * Creates and configures the JDA instance with optimal settings for ByteGuard.
     *
     * @return Configured JDA instance
     */
    private static JDA createJDAInstance() {
        logger.info("Initializing JDA with optimized configuration...");

        JDABuilder builder = JDABuilder.createDefault(ConfigManager.getBotToken())
                // Gateway Intents - Only enable what we need
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS,
                        GatewayIntent.GUILD_VOICE_STATES
                )
                .disableIntents(
                        GatewayIntent.GUILD_PRESENCES,
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.DIRECT_MESSAGE_REACTIONS
                )

                // Cache Configuration - Optimize memory usage
                .setMemberCachePolicy(MemberCachePolicy.VOICE.or(MemberCachePolicy.OWNER))
                .setChunkingFilter(ChunkingFilter.include(ConfigManager.getMaxGuildsCache()))
                .disableCache(
                        CacheFlag.ACTIVITY,
                        CacheFlag.CLIENT_STATUS,
                        CacheFlag.ONLINE_STATUS
                )
                .enableCache(
                        CacheFlag.VOICE_STATE,
                        CacheFlag.ROLE_TAGS
                )

                // Performance Settings
                .setBulkDeleteSplittingEnabled(false)
                .setCompression(net.dv8tion.jda.api.utils.Compression.ZLIB)
                .setLargeThreshold(50)

                // Bot Activity
                .setActivity(Activity.watching("servers | /help"));

        // Add event listeners
        builder.addEventListeners(
                new ReadyEventListener(),
                new SlashCommandListener(botManager),
                botManager.getEventDispatcher()
        );

        return builder.build();
    }

    /**
     * Gracefully shutdown the bot and cleanup resources.
     */
    public static void shutdown() {
        logger.info("🛑 Shutting down ByteGuard...");

        try {
            if (botManager != null) {
                botManager.shutdown();
            }

            if (jda != null) {
                jda.shutdown();
                logger.info("✅ JDA shutdown complete");
            }

            logger.info("👋 ByteGuard shutdown complete");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }

    /**
     * Get the JDA instance.
     *
     * @return JDA instance
     */
    public static JDA getJDA() {
        return jda;
    }

    /**
     * Get the bot manager instance.
     *
     * @return BotManager instance
     */
    public static BotManager getBotManager() {
        return botManager;
    }
}