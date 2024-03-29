/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022, Max Roncace <mproncace@protonmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.caseif.crosstitles;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
/**
 * An API for sending titles to players.
 */
public class TitleUtil {

    private static Throwable throwable;

    private static final String VERSION_STRING;
    private static final boolean TITLE_SUPPORT;
    private static final boolean USING_BUKKIT_API;

    // fields
    private static Field entityPlayer_playerConnection;

    // constructors
    private static Constructor<?> packetPlayOutTitle_init_LL;
    private static Constructor<?> packetPlayOutTitle_init_III;

    // methods
    private static Method player_sendTitle_2;
    private static Method player_sendTitle_5;
    private static Method chatSerializer_a;
    private static Method craftPlayer_getHandle;
    private static Method playerConnection_sendPacket;

    // enum values
    private static Object enumTitleAction_subtitle;
    private static Object enumTitleAction_title;

    static {
        String[] array = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        VERSION_STRING = array.length == 4 ? array[3] + "." : "";

        boolean titleSupport = true;
        boolean usingBukkit = false;

        try {
            // for specifying title type
            @SuppressWarnings("unchecked")
            Class<? extends Enum> enumTitleAction;
            try {
                // this changed to an inner class at some point during 1.8
                enumTitleAction = (Class<? extends Enum>)getNmsClass("PacketPlayOutTitle$EnumTitleAction");
            } catch (ClassNotFoundException ex) {
                // older 1.8 builds/1.7
                enumTitleAction = (Class<? extends Enum>)getNmsClass("EnumTitleAction");
            }
            enumTitleAction_title = Enum.valueOf(enumTitleAction, "TITLE");
            enumTitleAction_subtitle = Enum.valueOf(enumTitleAction, "SUBTITLE");

            // constructors for the packet
            Class<?> packetPlayOutTitle = getNmsClass("PacketPlayOutTitle");
            packetPlayOutTitle_init_LL =
                    packetPlayOutTitle.getConstructor(enumTitleAction, getNmsClass("IChatBaseComponent"));
            packetPlayOutTitle_init_III = packetPlayOutTitle.getConstructor(int.class, int.class, int.class);

            // for getting an IChatBaseComponent from a String
            try {
                // changed at the same time as EnumTitleAction
                chatSerializer_a =
                        getNmsClass("IChatBaseComponent$ChatSerializer").getDeclaredMethod("a", String.class);
            } catch (ClassNotFoundException ex) {
                // older 1.8 builds/1.7
                chatSerializer_a = getNmsClass("ChatSerializer").getDeclaredMethod("a", String.class);
            }

            // for sending packets
            craftPlayer_getHandle = getCraftClass("entity.CraftPlayer").getMethod("getHandle");
            entityPlayer_playerConnection = getNmsClass("EntityPlayer").getDeclaredField("playerConnection");
            playerConnection_sendPacket =
                    getNmsClass("PlayerConnection").getMethod("sendPacket", getNmsClass("Packet"));
        }
        catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException ex) {
            // check if we can just fall back on the Bukkit API
            try {
                player_sendTitle_2 = Player.class.getMethod("sendTitle", String.class, String.class);
                player_sendTitle_5 = Player.class.getMethod("sendTitle", String.class, String.class, int.class,
                        int.class, int.class);
                usingBukkit = true;
            } catch (NoSuchMethodException ex2) {
                throwable = ex;
                titleSupport = false;
            }
        }

        TITLE_SUPPORT = titleSupport;
        USING_BUKKIT_API = usingBukkit;
    }

    /**
     * Returns whether titles are supported on the current server.
     * @return whether titles are supported on the current server
     */
    public static boolean areTitlesSupported() {
        return TITLE_SUPPORT;
    }

    /**
     * Returns the {@link Throwable} preventing title support, if applicable.
     *
     * @return The {@link Throwable} preventing title support, or
     * <code>null</code> if titles are supported
     */
    public Throwable getException() {
        return throwable;
    }

    private static void sendTitle(Player player, String title, ChatColor color, boolean sub) {
        if (TITLE_SUPPORT) {
            try {
                if (USING_BUKKIT_API) {
                    String mainTitle = sub ? null : color + title;
                    String subtitle = sub ? color + title : null;
                    player_sendTitle_2.invoke(player, mainTitle, subtitle);
                } else {
                    String json = "{\"text\":\"" + title + "\"";
                    if (color != null) { // append color info
                        json += ",\"color\":\"" + color.name().toLowerCase() + "\"";
                    }
                    json += "}";

                    Object packet = packetPlayOutTitle_init_LL.newInstance(
                            // the type of information contained by the packet
                            sub ? enumTitleAction_subtitle : enumTitleAction_title,
                            // the serialized JSON to send via the packet
                            chatSerializer_a.invoke(null, json)
                    );
                    Object vanillaPlayer = craftPlayer_getHandle.invoke(player);
                    Object playerConnection = entityPlayer_playerConnection.get(vanillaPlayer);
                    playerConnection_sendPacket.invoke(playerConnection, packet);
                }
            }
            catch (IllegalAccessException | InvocationTargetException | InstantiationException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Sends a title to a player.
     * @param player the player to send the title to
     * @param title the content of the title
     * @param color the color of the title
     */
    public static void sendTitle(Player player, String title, ChatColor color) {
        sendTitle(player, title, color, false);
    }

    /**
     * Sends a subtitle to a player if one is already displaying; otherwise sets
     * the subtitle to display when a title is next sent.
     * @param player the player to send the subtitle to
     * @param subtitle the content of the subtitle
     * @param color the color of the subtitle
     */
    public static void sendSubtitle(Player player, String subtitle, ChatColor color) {
        sendTitle(player, subtitle, color, true);
    }

    /**
     * Sets the timing for the current title if one is displaying; otherwise
     * sets the timing for the next title sent.
     * @param player the player to set title timing for
     * @param fadeIn the time in ticks the title should fade in over (default
     *               20)
     * @param stay the time in ticks the title should remain on the screen for
     *             between fades (default 60)
     * @param fadeOut the time in ticks the title should fade out over (default
     *                20)
     *
     * @deprecated This will not work properly on newer Minecraft versions.
     */
    @Deprecated
    public static void sendTimes(Player player, int fadeIn, int stay, int fadeOut) {
        if (TITLE_SUPPORT && !USING_BUKKIT_API) {
            try {
                Object packet = packetPlayOutTitle_init_III.newInstance(fadeIn, stay, fadeOut);
                Object vanillaPlayer = craftPlayer_getHandle.invoke(player);
                Object playerConnection = entityPlayer_playerConnection.get(vanillaPlayer);
                playerConnection_sendPacket.invoke(playerConnection, packet);
            }
            catch (IllegalAccessException | InstantiationException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Sends a title and subtitle to a player.
     * @param player the player to send the title to
     * @param title the content of the title
     * @param titleColor the color of the title
     * @param subtitle the content of the subtitle
     * @param subColor the color of the subtitle
     */
    public static void sendTitle(Player player,
                                 String title, ChatColor titleColor,
                                 String subtitle, ChatColor subColor) {
        sendSubtitle(player, subtitle, subColor);
        sendTitle(player, title, titleColor);
    }

    /**
     * Sends a title and subtitle with the given timing to a player.
     * @param player the player to send the title to
     * @param title the content of the title
     * @param titleColor the color of the title
     * @param subtitle the content of the subtitle
     * @param subColor the color of the subtitle
     * @param fadeIn the time in ticks the title should fade in over (default
     *               20)
     * @param stay the time in ticks the title should remain on the screen for
     *             between fades (default 60)
     * @param fadeOut the time in ticks the title should fade out over (default
     *                20)
     */
    public static void sendTitle(Player player,
                                 String title, ChatColor titleColor,
                                 String subtitle, ChatColor subColor,
                                 int fadeIn, int stay, int fadeOut) {
        if (USING_BUKKIT_API) {
            String titleJson = title != null ? titleColor + title : null;
            String subtitleJson = subtitle != null ? subColor + subtitle : null;
            try {
                player_sendTitle_5.invoke(player, titleJson, subtitleJson, fadeIn, stay, fadeOut);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
        } else {
            sendTimes(player, fadeIn, stay, fadeOut);
            if (subtitle != null) {
                sendSubtitle(player, subtitle, subColor);
            }
            if (title != null) {
                sendTitle(player, title, titleColor);
            }
        }
    }

    /**
     * Sends a title with the given timing to a player.
     * @param player the player to send the title to
     * @param title the content of the title
     * @param titleColor the color of the title
     * @param fadeIn the time in ticks the title should fade in over (default
     *               20)
     * @param stay the time in ticks the title should remain on the screen for
     *             between fades (default 60)
     * @param fadeOut the time in ticks the title should fade out over (default
     *                20)
     */
    public static void sendTitle(Player player,
                                 String title, ChatColor titleColor,
                                 int fadeIn, int stay, int fadeOut) {
        sendTitle(player, title, titleColor, null, ChatColor.RESET, fadeIn, stay, fadeOut);
    }

    /**
     * Retrieves a class by the given name from the package
     * <code>net.minecraft.server</code>.
     *
     * @param name the class to retrieve
     * @return the class object from the package
     * <code>net.minecraft.server</code>
     * @throws ClassNotFoundException if the class does not exist in the
     * package
     */
    private static Class<?> getNmsClass(String name) throws ClassNotFoundException {
        String className = "net.minecraft.server." + VERSION_STRING + name;
        return Class.forName(className);
    }

    /**
     * Retrieves a class by the given name from the package
     * <code>org.bukkit.craftbukkit</code>.
     *
     * @param name the class to retrieve
     * @return the class object from the package
     * <code>org.bukkit.craftbukkit</code>
     * @throws ClassNotFoundException if the class does not exist in the
     * package
     */
    private static Class<?> getCraftClass(String name) throws ClassNotFoundException {
        String className = "org.bukkit.craftbukkit." + VERSION_STRING + name;
        return Class.forName(className);
    }

}
