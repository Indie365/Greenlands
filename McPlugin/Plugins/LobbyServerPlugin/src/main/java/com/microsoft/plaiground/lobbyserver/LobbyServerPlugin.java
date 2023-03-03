package com.microsoft.plaiground.lobbyserver;

import com.microsoft.plaiground.common.config.CommonApplicationConfig;
import com.microsoft.plaiground.common.helpers.PlaigroundPlugin;
import com.microsoft.plaiground.common.listeners.CommonWorldListener;
import com.microsoft.plaiground.common.providers.JedisClient;
import com.microsoft.plaiground.common.providers.JedisClientProvider;
import com.microsoft.plaiground.common.utils.AsyncHelper;
import com.microsoft.plaiground.common.utils.MinecraftLogger;
import com.microsoft.plaiground.common.utils.ServerUtils;
import com.microsoft.plaiground.common.utils.WorldUtils;
import com.microsoft.plaiground.lobbyserver.commands.LobbyConsoleCommands;
import com.microsoft.plaiground.lobbyserver.listeners.LobbyPluginMessageListener;
import com.microsoft.plaiground.lobbyserver.listeners.LobbyWorldListener;
import com.microsoft.plaiground.lobbyserver.utils.AgentPairingEHListener;
import org.bukkit.Bukkit;

public class LobbyServerPlugin extends PlaigroundPlugin {

  @Override
  protected void loadDependencies() {
    super.loadDependencies();

    injector.getInstance(JedisClient.class);

    var appConfig = injector.getInstance(CommonApplicationConfig.class);

    // register game loops
    AsyncHelper.registerLoop();
    AgentPairingEHListener.registerLoop(appConfig);
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();

    loadDependencies();

    // register listeners
    var pluginManager = Bukkit.getPluginManager();
    pluginManager.registerEvents(new CommonWorldListener(), this);
    pluginManager.registerEvents(new LobbyWorldListener(), this);

    // setup communication with BungeeCord
    var pluginMessenger = this.getServer().getMessenger();
    pluginMessenger.registerOutgoingPluginChannel(this, "BungeeCord");
    pluginMessenger.registerIncomingPluginChannel(this, "BungeeCord",
        LobbyPluginMessageListener.getInstance());

    // create worlds if necessary
    WorldUtils.createLobbyWorld();
    ServerUtils.dispatchCommand("mv conf firstspawnoverride true");
    // set default world for server
    ServerUtils.dispatchCommand("mv conf firstspawnworld " + WorldUtils.LOBBY_WORLD_NAME);

    // register console commands
    var appConfig = injector.getInstance(CommonApplicationConfig.class);
    LobbyConsoleCommands.registerCommands(appConfig);

    MinecraftLogger.info("Done loading plugin!");
  }

  @Override
  public void onDisable() {
    JedisClientProvider.getInstance().closePool();
    this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    this.getServer().getMessenger().unregisterIncomingPluginChannel(this);

    var jedisClientProvider = injector.getInstance(JedisClient.class);
    jedisClientProvider.closePool();
  }

}
