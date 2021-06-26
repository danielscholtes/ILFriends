package me.scholtes.ilfriends.commands;

import me.scholtes.ilfriends.ILFriends;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class FriendCommand implements CommandExecutor {

    private ILFriends plugin;

    public FriendCommand(ILFriends plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Checks if the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("You need to be a player.");
            return true;
        }

        Player player = (Player) sender;

        // If not enough arguments are specified sends a help message
        if (args.length < 1) {
            player.sendMessage("/friend add <player>");
            player.sendMessage("/friend remove <player>");
            player.sendMessage("/friend list");
            return true;
        }

        // If the first argument is specified switches through the commands
        switch (args[0].toLowerCase()) {
            case "add": {
                // If not enough arguments are specified sends a help message
                if (args.length < 2) {
                    player.sendMessage("Please specify a player");
                    player.sendMessage("/friend add <player>");
                    return true;
                }

                // Prevents player from friending himself
                if (args[1].equalsIgnoreCase(player.getName())) {
                    player.sendMessage("You cannot friend yourself");
                    player.sendMessage("/friend add <player>");
                    return true;
                }

                UUID targetUUID = getUUIDFromName(args[1]);

                // Checks if the player has joined before
                if (targetUUID == null) {
                    player.sendMessage("That player has never joined the server!");
                    player.sendMessage("/friend add <player>");
                    return true;
                }

                // Checks if the player and the target are already friends
                plugin.getPlayerDataHandler().isFriends(player.getUniqueId(), targetUUID).whenComplete((isFriend, e1) -> {
                    if (isFriend) {
                        player.sendMessage("You're already friends with this player");
                        return;
                    }

                    // Checks if player sent a friend request to target
                    plugin.getPlayerDataHandler().hasFriendRequest(targetUUID, player.getUniqueId()).whenComplete((sentRequest, e2) -> {
                        if (sentRequest) {
                            player.sendMessage("You already sent a friend request to this player");
                            return;
                        }

                        // Checks if player has a friend request from target
                        plugin.getPlayerDataHandler().hasFriendRequest(player.getUniqueId(), targetUUID).whenComplete((hasRequest, e3) -> {
                            if (hasRequest) {
                                plugin.getPlayerDataHandler().acceptFriendRequest(player.getUniqueId(), targetUUID);
                                player.sendMessage("You are now friends with " + args[1]);
                                return;
                            }

                            // Sends a friend request to the target player
                            plugin.getPlayerDataHandler().sendFriendRequest(player.getUniqueId(), targetUUID);
                            player.sendMessage("You sent a friend request to " + args[1]);
                            return;

                        });
                    });

                });
                return true;
            }

            case "remove": {
                // If not enough arguments are specified sends a help message
                if (args.length < 2) {
                    player.sendMessage("Please specify a player");
                    player.sendMessage("/friend remove <player>");
                    return true;
                }

                // Prevents player from unfriending himself
                if (args[1].equalsIgnoreCase(player.getName())) {
                    player.sendMessage("You cannot unfriend yourself");
                    player.sendMessage("/friend remove <player>");
                    return true;
                }

                UUID targetUUID = getUUIDFromName(args[1]);

                // Checks if the player has joined before
                if (targetUUID == null) {
                    player.sendMessage("That player has never joined the server!");
                    player.sendMessage("/friend remove <player>");
                    return true;
                }

                // Checks if the player and the target are already friends
                plugin.getPlayerDataHandler().isFriends(player.getUniqueId(), targetUUID).whenComplete((result, exception) -> {
                    if (!result) {
                        player.sendMessage("You're not friends with this player");
                        return;
                    }

                    // Removes player from friend list
                    player.sendMessage("Removed " + args[1] + " from your friend list!");
                    plugin.getPlayerDataHandler().removeFriend(player.getUniqueId(), targetUUID);
                    return;

                });
                return true;
            }

            case "list": {
                // Sends a list of all the players friends
                player.sendMessage("------ Friend List ------");
                plugin.getPlayerDataHandler().getFriendList(player.getUniqueId()).whenComplete((result, exception) -> {
                    if (exception != null) {
                        System.out.println("Error when retrieving friend list");
                    } else {
                        for (UUID friend : result.keySet()) {
                            String username = Bukkit.getOfflinePlayer(friend).getName();
                            DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM d yyyy");
                            String date = format.format(result.get(friend));

                            player.sendMessage(username + " - " + date);
                        }
                    }
                });
                return true;
            }

        }

        return true;
    }

    /**
     * Gets the UUID of a player from their username (offline/online)
     *
     * @param name Username of the player
     * @return The UUID of the player
     */
    private UUID getUUIDFromName(String name) {
        if (Bukkit.getPlayerExact(name) != null) {
            return Bukkit.getPlayerExact(name).getUniqueId();
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
            if (!offlinePlayer.hasPlayedBefore()) {
                return null;
            }
            return offlinePlayer.getUniqueId();
        }
    }

}
