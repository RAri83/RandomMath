package ir.arij83.randommath;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class RandomMath extends JavaPlugin implements Listener {

    private HashMap<Player, Integer> currentAnswers = new HashMap<>();
    private String difficulty;
    private int questionInterval;
    private FileConfiguration messagesConfig;
    private FileConfiguration rewardsConfig;
    private boolean isQuestionActive = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        saveResource("messages.yml", false);
        messagesConfig = getConfig("messages.yml");
        loadRewardsConfig();
        getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Config loaded!");
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Messages loaded!");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "Rewards loaded!");

        getLogger().info("§dRandomMath §f:) plugin enabled!");
        getLogger().info("§dRandomMath §fby §5AriJ83");
        startMathQuestionTask();
    }

    @Override
    public void onDisable() {
        getLogger().info("§dRandomMath §f:( plugin disabled!");
        getLogger().info("§d:) Thankyou for using my plugin !");
        currentAnswers.clear();
    }

    private void loadConfig() {
        questionInterval = getConfig().getInt("question-time", 60);
        difficulty = getConfig().getString("question-dif", "normal").toLowerCase();
    }

    private void loadRewardsConfig() {
        File rewardsFile = new File(getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) {
            saveResource("rewards.yml", false);
        }
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
    }

    private void startMathQuestionTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                askMathQuestion();
            }
        }.runTaskTimer(this, 0, questionInterval * 20L);
    }

    private void askMathQuestion() {
        if (isQuestionActive) return;

        Random random = new Random();
        int num1 = random.nextInt(10) + 1;
        int num2 = random.nextInt(10) + 1;
        int answer = 0;
        String question = "";

        switch (difficulty) {
            case "easy":
                question = num1 + " + " + num2;
                answer = num1 + num2;
                break;
            case "normal":
                int operator = random.nextInt(3);
                switch (operator) {
                    case 0:
                        question = num1 + " + " + num2;
                        answer = num1 + num2;
                        break;
                    case 1:
                        question = num1 + " - " + num2;
                        answer = num1 - num2;
                        break;
                    case 2:
                        question = num1 + " / " + num2;
                        answer = num1 / num2;
                        break;
                }
                break;
            case "hard":
                num1 = random.nextInt(100) + 50;
                num2 = random.nextInt(100) + 50;
                int complexOperator = random.nextInt(4);
                switch (complexOperator) {
                    case 0:
                        question = num1 + " + " + num2;
                        answer = num1 + num2;
                        break;
                    case 1:
                        question = num1 + " - " + num2;
                        answer = num1 - num2;
                        break;
                    case 2:
                        question = num1 + " / " + num2;
                        answer = num1 / num2;
                        break;
                    case 3:
                        question = num1 + " * " + num2;
                        answer = num1 * num2;
                        break;
                }
                break;
        }

        isQuestionActive = true;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(getFormattedMessage("math_question", "%question%", question));
            player.sendMessage(getFormattedMessage("time_limit", "%time%", "30"));
            currentAnswers.put(player, answer);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (isQuestionActive) {
                    isQuestionActive = false;
                    currentAnswers.clear();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(getFormattedMessage("time_up", "", ""));
                    }
                }
            }
        }.runTaskLater(this, 30 * 20L);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (!isQuestionActive) return;

        if (currentAnswers.containsKey(player)) {
            int correctAnswer = currentAnswers.get(player);

            try {
                int playerAnswer = Integer.parseInt(message);
                if (playerAnswer == correctAnswer) {
                    event.setCancelled(true);
                    giveReward(player);
                    currentAnswers.remove(player);
                    isQuestionActive = false;
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void giveReward(Player player) {
        String rewardPath = "normal";
        switch (difficulty) {
            case "easy":
                rewardPath = "easy";
                break;
            case "hard":
                rewardPath = "hard";
                break;
        }

        List<String> rewardCommands = rewardsConfig.getStringList(rewardPath);
        for (String command : rewardCommands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
        }

        player.sendMessage(getFormattedMessage("correct_answer", "%player%", player.getName()));
    }

    private String getFormattedMessage(String key, String placeholder, String replacement) {
        String prefix = messagesConfig.getString("prefix", "&7[RandomMath] ");
        String message = messagesConfig.getString(key, "&cMessage not found!");
        return ChatColor.translateAlternateColorCodes('&', prefix + message.replace(placeholder, replacement));
    }

    private FileConfiguration getConfig(String filename) {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), filename));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rmath")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("rmath.admin")) {
                        reloadConfig();
                        loadConfig();
                        loadRewardsConfig();
                        messagesConfig = getConfig("messages.yml");
                        sender.sendMessage(getFormattedMessage("reload_success", "", ""));
                    } else {
                        sender.sendMessage(getFormattedMessage("no_permission", "", ""));
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("author")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aThis plugin was developed by &dAriJ83."));
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou can read more about author on:"));
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5https://devs.craft-tech.xyz/AriJ83"));
                    return true;
                }
            }
        }
        return false;
    }
}
