# Admin Mode
A plugin that gives administrators (particularly in survival mode) abilities that make their jobs easier to do.

## Features
- Upon entering admin mode, the player's current inventory, food, experience, status effects, and location is stored in memory. When the player leaves admin mode, their inventory, food, experience, status effects, and location are restored
- Configurable list of permissions that are given to players upon entering admin mode
- A command to easily remove all permissions that have been given to players by this plugin ('/amclearperms' or '/adminmodeclearperms')
- Entering admin mode with a victim name (the player the admin is going to help) and a reason. Both of which are entirely optional ('/adminmode [reason]')
- Messages that broadcast to other admins (such as when a player enters/leaves admin mode) (uses the permission 'adminmode.notify')
- View a list of players that are currently in admin mode ('/amlist' or '/adminmodelist')
- Kick a player out of admin mode ('/amkick' or '/adminmodekick')
- Modifiable (on/off) attributes: godmode, damage other entities, pickup/drop items, break/place blocks
- Players in admin mode are put back to their pre-admin mode state when they disconnect from the server
- Crash safe. If the server crashes, all players that were in admin mode are restored back to their pre-admin mode state upon reconnection to the server. (If the server crashed, the file amplayerdata.dat will contain all the information needed for the plugin to restore the player's pre-admin mode state. Most of the time, this file should be empty when the server is off)

## Permissions
```
# Gives access to '/adminmode'. Recommended rank: admins
adminmode.command.adminmode

# Gives access to '/amlist' and '/adminmodelist'. Recommended rank: higher-class admins
adminmode.command.list

# Gives access to '/amkick' and '/adminmodekick'. Recommended rank: higher-class admins
adminmode.command.kick

# Gives access to '/amclearperms' and '/adminmodeclearperms'. Recommended rank: server operators
adminmode.command.clearperms

# Allows the player to see admin mode messages in chat (such as a player leaving/entering admin mode). Recommended rank: admins
adminmode.notify
```

## Commands
```
# Command argument legend:
# [optional]
# <required>

# Toggle on/off admin mode
# [reason] is optional and is only displayed when entering admin mode: "'Player' has entered admin mode to [reason]"
# Otherwise the message is: "'Player' has entered admin mode!" or "'Player' has left admin mode!"
/adminmode [reason]

# Gives a list of players currently in admin mode
/amlist
/adminmodelist

# Kick a player out of admin mode
/amkick <player>
/adminmodekick <player>

# Clears all contextual permissions ("contextual" as in permissions that are set "true" only when they are in the correct context (i.e. in admin mode)) from all players
# This command should be ran after updating the 'permissions' option in the configuration file (adminmode.conf) to not leave any leftover contextual permissions from the previous configuration
# This command should also be ran if you wish to completely remove this plugin from your server, not leaving any leftover contextual permissions behind
# Note that it will clear contextual permissions for all players, regardless if they are online or offline, and regardless if they are in admin mode. If you run this command while (a) player(s) is in admin mode, that/those player(s) will need to re-enable admin mode to get their contextual permissions back/updated
/amclearperms
/adminmodeclearperms
```

## Default Configuration File
```
# Permissions given to players in admin mode
# Note: whenever you change or update these permissions, you should run '/amclearperms' to clear all previously existing permissions given by the plugin
permissions: [
    "prism.*",
    "griefprevention.*"
]

# Items given to players when they enter admin mode
items: [
    {
        id: compass
        quantity: 1
    },

    {
        id: stick
        quantity: 1
    },

    {
        id: golden_shovel
        quantity: 1
    }
]

# Attributes available to players in admin mode
attributes: {
    godmode: true
    damage_other_entities: false
    break_blocks: false
    place_blocks: false
    drop_items: false
    pickup_items: false
}
```

### Known bugs
- '/amclearperms' and '/adminmodeclearperms' may have a delay (up to 3 seconds or so) between command execution and the permissions being removed. Sometimes may fail to work at all (just re-execute the command if this happens)
- Even with the "drop_items" attribute false, dropping an item will cause it to disappear from the inventory (but no item entity will be spawned as a result of the drop)
- Items in the player's main inventory (3*9 slots) and ender chest will not retain their original positions after entering and leaving admin mode
