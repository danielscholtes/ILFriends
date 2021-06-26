package me.scholtes.ilfriends.data;

import me.scholtes.ilfriends.ILFriends;
import org.bukkit.Bukkit;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlayerDataHandler {

    private ILFriends plugin;

    /**
     * Initializes the Data Handler
     * @param plugin Instance of the plugin
     */
    public PlayerDataHandler(ILFriends plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets the friend list of the player
     *
     * @param uuid The UUID of the player
     * @return A completable future containing the friend list
     */
    public CompletableFuture<Map<UUID, LocalDate>> getFriendList(UUID uuid) {
        CompletableFuture<Map<UUID, LocalDate>> completableFuture = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<UUID, LocalDate> friendList = new HashMap<>();
            Connection con = null;

            String getFriends = "SELECT * FROM friends WHERE player1=? OR player2=?";

            PreparedStatement preparedStatement = null;

            try {
                con = plugin.getMySQLDataBase().getDataSource().getConnection();
                preparedStatement = con.prepareStatement(getFriends);
                preparedStatement.setString(1, uuid.toString());
                preparedStatement.setString(2, uuid.toString());

                // Goes through each row (containing the player) and adds the data to the map
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    String player1 = resultSet.getString("player1");
                    String player2 = resultSet.getString("player2");
                    LocalDate date = resultSet.getDate("date").toLocalDate();

                    if (UUID.fromString(player1).equals(uuid)) {
                        friendList.put(UUID.fromString(player2), date);
                    } else {
                        friendList.put(UUID.fromString(player1), date);
                    }
                }
                completableFuture.complete(friendList);

                if (resultSet != null) {
                    try {
                        resultSet.close();
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.WARNING, "Error while closing result set");
                        e.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                // Error handling
                completableFuture.completeExceptionally(e);
                plugin.getLogger().log(Level.WARNING, "Error while retrieving player data");
                e.printStackTrace();
            } finally {
                MySQLDataBase.close(con, preparedStatement);
            }

        });

        return completableFuture;
    }

    /**
     * Makes 2 players friends and stores it in the database (order in parameters doesn't matter)
     *
     * @param player1 The first player
     * @param player2 The second player
     */
    public void addFriend(UUID player1, UUID player2) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection con = null;

            String save = "INSERT INTO friends VALUES(DEFAULT,?,?,?)";

            PreparedStatement preparedStatement = null;

            try {
                con = plugin.getMySQLDataBase().getDataSource().getConnection();

                preparedStatement = con.prepareStatement(save);
                preparedStatement.setString(1, player1.toString());
                preparedStatement.setString(2, player2.toString());
                preparedStatement.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
                preparedStatement.execute();
            } catch (SQLException e) {
                // Error handling
                plugin.getLogger().log(Level.WARNING, "Error while removing friend");
                e.printStackTrace();
            } finally {
                MySQLDataBase.close(con, preparedStatement);
            }
        });
    }

    /**
     * Removes the friendship between 2 players and their entry from the database (order in parameters doesn't matter)
     *
     * @param player1 The first player
     * @param player2 The second player
     */
    public void removeFriend(UUID player1, UUID player2) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection con = null;

            String remove = "DELETE FROM friends WHERE (player1=? AND player2=?) OR (player1=? AND player2=?)";

            PreparedStatement preparedStatement = null;

            try {
                con = plugin.getMySQLDataBase().getDataSource().getConnection();
                preparedStatement = con.prepareStatement(remove);
                preparedStatement.setString(1, player1.toString());
                preparedStatement.setString(2, player2.toString());
                preparedStatement.setString(3, player2.toString());
                preparedStatement.setString(4, player1.toString());

                preparedStatement.execute();
            } catch (SQLException e) {
                // Error handling
                plugin.getLogger().log(Level.WARNING, "Error while removing friend");
                e.printStackTrace();
            } finally {
                MySQLDataBase.close(con, preparedStatement);
            }
        });
    }

    /**
     * Sends a friend request from the sender to the receiver and saves it in the database
     *
     * @param sender The sender of the friend request
     * @param receiver The receiver of the friend request
     */
    public void sendFriendRequest(UUID sender, UUID receiver) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection con = null;

            String save = "INSERT INTO requests VALUES(DEFAULT,?,?)";

            PreparedStatement preparedStatement = null;

            try {
                con = plugin.getMySQLDataBase().getDataSource().getConnection();
                preparedStatement = con.prepareStatement(save);
                preparedStatement.setString(1, sender.toString());
                preparedStatement.setString(2, receiver.toString());
                preparedStatement.execute();
            } catch (SQLException e) {
                // Error handling
                plugin.getLogger().log(Level.WARNING, "Error while sending friend request");
                e.printStackTrace();
            } finally {
                MySQLDataBase.close(con, preparedStatement);
            }
        });
    }

    /**
     * Accepts a friend request from the sender to the receiver and makes them friends
     *
     * @param sender The sender of the friend request
     * @param receiver The receiver of the friend request
     */
    public void acceptFriendRequest(UUID receiver, UUID sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection con = null;

            String remove = "DELETE FROM requests WHERE sender=? AND receiver=?";

            PreparedStatement preparedStatement = null;

            try {
                con = plugin.getMySQLDataBase().getDataSource().getConnection();
                preparedStatement = con.prepareStatement(remove);
                preparedStatement.setString(1, sender.toString());
                preparedStatement.setString(2, receiver.toString());
                preparedStatement.execute();
            } catch (SQLException e) {
                // Error handling
                plugin.getLogger().log(Level.WARNING, "Error while accepting friend request");
                e.printStackTrace();
            } finally {
                MySQLDataBase.close(con, preparedStatement);
            }
        });

        addFriend(receiver, sender);
    }

    /**
     * Checks if the receiver has a friend request from the sender
     *
     * @param sender The sender of the friend request
     * @param receiver The receiver of the friend request
     * @return A completable future containing if there is a friend request
     */
    public CompletableFuture<Boolean> hasFriendRequest(UUID receiver, UUID sender) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection con = null;
            String isFriend = "SELECT * FROM requests WHERE sender=? AND receiver=?";

            PreparedStatement preparedStatement = null;

            try {
                con = plugin.getMySQLDataBase().getDataSource().getConnection();
                preparedStatement = con.prepareStatement(isFriend);
                preparedStatement.setString(1, sender.toString());
                preparedStatement.setString(2, receiver.toString());

                ResultSet resultSet = preparedStatement.executeQuery();

                completableFuture.complete(resultSet.next());

                if(resultSet != null) {
                    try {
                        resultSet.close();
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.WARNING, "Error while closing result set");
                        e.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                // Error handling
                plugin.getLogger().log(Level.WARNING, "Error while checking if there is a friend request");
                e.printStackTrace();
                completableFuture.completeExceptionally(e);
            } finally {
                MySQLDataBase.close(con, preparedStatement);
            }
        });

        return completableFuture;
    }

    /**
     * Checks if 2 players are friends
     *
     * @param player1 The first player
     * @param player2 The second player
     * @return A completable future containing if the two players are friends
     */
    public CompletableFuture<Boolean> isFriends(UUID player1, UUID player2) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Connection con = null;
            String isFriend = "SELECT * FROM friends WHERE (player1=? AND player2=?) OR (player1=? AND player2=?)";

            PreparedStatement preparedStatement = null;

            try {
                con = plugin.getMySQLDataBase().getDataSource().getConnection();
                preparedStatement = con.prepareStatement(isFriend);
                preparedStatement.setString(1, player1.toString());
                preparedStatement.setString(2, player2.toString());
                preparedStatement.setString(3, player2.toString());
                preparedStatement.setString(4, player1.toString());

                ResultSet resultSet = preparedStatement.executeQuery();

                completableFuture.complete(resultSet.next());

                if(resultSet != null) {
                    try {
                        resultSet.close();
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.WARNING, "Error while closing result set");
                        e.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                // Error handling
                plugin.getLogger().log(Level.WARNING, "Error while checking if players are friends");
                e.printStackTrace();
                completableFuture.completeExceptionally(e);
            } finally {
                MySQLDataBase.close(con, preparedStatement);
            }
        });

        return completableFuture;
    }

}
