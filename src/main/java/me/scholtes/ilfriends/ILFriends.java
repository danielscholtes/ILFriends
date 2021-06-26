package me.scholtes.ilfriends;

import me.scholtes.ilfriends.commands.FriendCommand;
import me.scholtes.ilfriends.data.MySQLDataBase;
import me.scholtes.ilfriends.data.PlayerDataHandler;
import org.bukkit.plugin.java.JavaPlugin;

public final class ILFriends extends JavaPlugin {

    private MySQLDataBase mySQLDataBase;
    private PlayerDataHandler playerDataHandler;

    /**
     * Enables the plugin and initializes the data handler and database
     */
    @Override
    public void onEnable() {
        mySQLDataBase = new MySQLDataBase(this);
        playerDataHandler = new PlayerDataHandler(this);

        getCommand("friend").setExecutor(new FriendCommand(this));
    }

    /**
     * Returns the MySQL DataBase
     * @return Instance of {@link MySQLDataBase}
     */
    public MySQLDataBase getMySQLDataBase() {
        return mySQLDataBase;
    }

    /**
     * Returns the Player Data Handler
     * @return Instance of {@link PlayerDataHandler}
     */
    public PlayerDataHandler getPlayerDataHandler() {
        return playerDataHandler;
    }

}
