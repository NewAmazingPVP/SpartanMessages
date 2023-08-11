package mc.spartanmessages;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpartanMessages extends JavaPlugin {

    private JDA jda;
    private String discordChannelId = "";
    private List<String> hackNamesList = Arrays.asList(
            "NoFall", "XRay", "Exploits", "EntityMove", "NoSwing",
            "IrregularMovements", "ImpossibleActions", "ItemDrops",
            "AutoRespawn", "InventoryClicks", "NoSlowdown", "Criticals",
            "GhostHand", "BlockReach", "FastBow", "FastClicks",
            "FastHeal", "ImpossibleInventory", "HitReach", "FastBreak",
            "Speed", "FastPlace", "MorePackets", "FastEat", "Velocity",
            "KillAura"
    );

    private List<String> probabilityList = Arrays.asList("definitely");
    private Map<Player, Integer> playerPingMap = new HashMap<>();


    @Override
    public void onEnable() {
        // Initialize the Discord bot
        try {
            jda = JDABuilder.createDefault("").build();
        } catch (LoginException e) {
            getLogger().log(Level.SEVERE, "Failed to initialize Discord bot!", e);
            return;
        }

        // Register the Log4j 2 appender
        /*LogAppender appender = new LogAppender();
        org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        logger.addAppender(appender);*/
        Bukkit.getScheduler().runTaskTimer(this, this::monitorPlayerPings, 0L, 20L);
    }

    @Override
    public void onDisable() {
        // Shutdown the Discord bot on plugin disable
        if (jda != null) {
            jda.shutdownNow();
        }
    }

    /*private class LogAppender extends AbstractAppender {

        private SimpleDateFormat formatter;

        public LogAppender() {
            super("MyLogAppender", null, null);
            formatter = new SimpleDateFormat("HH:mm:ss");
            start();
        }

        @Override
        public void append(LogEvent event) {
            LogEvent log = event.toImmutable();
            String message = log.getMessage().getFormattedMessage();

            String info;
            if (message.contains("Spartan")) {
                for (String probability : probabilityList) {
                    if (message.contains(probability)) {
                        for (String hackName : hackNamesList) {
                            if (message.contains(hackName)) {
                                for (Player playerName : getPlayersWhoEverJoined()) {
                                    String name = playerName.getName();
                                    if (name != null && message.contains(name)) {
                                        if (highPing(playerName)) {
                                            info = getModifier(probability) + " " + name + " could be" + " using " + hackName + " hack but its probably their high ping of " + getPlayerPing(playerName) + " ms or could also be false flagged";
                                        } else {
                                            info = getModifier(probability) + " " + name + " probably is" + " using " + hackName + " hack or could also be false flagged";
                                        }
                                        if (probability.equals("definitely")) {
                                            info = info.toUpperCase();
                                        }
                                        TextChannel channel = jda.getTextChannelById(discordChannelId);
                                        if (channel != null) {
                                            channel.sendMessage(info).queue();
                                        } else {
                                            getLogger().warning("Discord channel not found!");
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
    }*/

    private int getPlayerPing(Player player) {
        return player.getPing();
    }
    private void monitorPlayerPings() {
        for (Player player : getServer().getOnlinePlayers()) {
            int currentPing = getPlayerPing(player);
            Integer lastPing = playerPingMap.get(player);

            if (lastPing != null) {
                double percentIncrease = (currentPing - lastPing) * 100.0 / lastPing;
                if (percentIncrease >= 50 && currentPing > 20 && !(lastPing < 2)) {
                    String pingInfo = getPingModifier(percentIncrease) + player.getName() + "'s ping has increased by " + String.format("%.2f", percentIncrease) + "% to " + currentPing + " ms!!!";
                    String pingMessage = ChatColor.YELLOW + "Your ping has increased by " + ChatColor.RED + String.format("%.2f", percentIncrease) + "%" + ChatColor.YELLOW + "!!! Its recommended that you be careful as your client might lag";
                    player.sendMessage(pingMessage);
                    if(percentIncrease >= 75){
                        player.sendMessage(pingMessage);
                    }
                    if(percentIncrease >= 100){
                        player.sendMessage(pingMessage + " and maybe anticheat can false detect you");
                        player.sendMessage(pingMessage + " and maybe anticheat can false detect you");
                    }
                    TextChannel channel = jda.getTextChannelById(discordChannelId);
                    if (channel != null) {
                        channel.sendMessage(pingInfo).queue();
                    } else {
                        getLogger().warning("Discord channel not found!");
                    }
                }
            }
            playerPingMap.put(player, currentPing);
        }
    }



    private boolean highPing(Player player){
        if(getPlayerPing(player) > 150){
            return true;
        } else {
            return false;
        }
    }

    public List<Player> getPlayersWhoEverJoined() {
        List<Player> playersWhoEverJoined = new ArrayList<>();
        File playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata");

        if (playerDataFolder.exists() && playerDataFolder.isDirectory()) {
            for (File playerDataFile : playerDataFolder.listFiles()) {
                String fileName = playerDataFile.getName();
                UUID uuid;
                try {
                    uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                } catch (IllegalArgumentException e) {
                    continue;
                }

                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                Player player = offlinePlayer.getPlayer();
                if (player != null) {
                    playersWhoEverJoined.add(player);
                }
            }
        }

        return playersWhoEverJoined;
    }


    public String getModifier(String caseVariable) {
        switch (caseVariable) {
            case "unlikely":
                return "###";
            case "potentially":
                return "##";
            case "certainly":
                return "#";
            case "definitely":
                TextChannel channel = jda.getTextChannelById(discordChannelId);
                Role adminRole = channel.getGuild().getRolesByName("Admin", true).stream().findFirst().orElse(null);
                if (adminRole != null) {
                    return "# " + adminRole.getAsMention();
                } else {
                    getLogger().warning("Admin role not found!");
                    return "###";
                }
            default:
                return "unknown modifier";
        }
    }

    public String getPingModifier(Double pingAlert) {
        if (pingAlert > 50 && pingAlert <= 75) {
            return "### ";
        } else if (pingAlert > 75 && pingAlert <= 100){
            return "## ";
        } else if (pingAlert > 100){
            return "# ";
        }
        return "";
    }

}
