# Admin Mode
A plugin that gives administrators (particularly in survival mode) abilities that make their jobs easier to do.

## Features
- Upon entering admin mode, the player's current inventory, food, experience, and location is stored in memory. When the player leaves admin mode, their inventory, food, experience, and location are restored
- Configurable list of permissions that are given to players upon entering admin mode
- A command to easily remove all permissions that have been given to players by this plugin ('/amclearperms' or '/adminmodeclearperms')
- Entering admin mode with a victim name (the player the admin is going to help) and a reason. Both of which are entirely optional ('/adminmode [victim] [reason]'
- Messages that broadcast to other admins (such as when a player enters/leaves admin mode) (uses the permission 'adminmode.notify')
- View a list of players that are currently in admin mode ('/amlist' or '/adminmodelist')
- Kick a player out of admin mode ('/amkick' or '/adminmodekick')
- Modifiable (on/off) attributes: godmode, vanish, damage other entities, pickup/drop items, break/place blocks
- Players in admin mode are put back to their pre-admin mode state when they disconnect from the server
- Crash safe. If the server crashes, all players that were in admin mode are restored back to their pre-admin mode state upon reconnection to the server

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

### Known bugs
- Items in the main inventory (the 3*9 grid inventory of the player) do not retain their original positions when the player enters and leaves admin mode -- the player's GridInventory slots appear to have no property "SlotIndex" or "SlotPos", so I am unable to fix this issue
- '/amclearperms' and '/adminmodeclearperms' have a delay (up to 3 seconds or so) between executing the command, and the permissions being removed. Sometimes may fail to work at all (just re-execute the command if this happens)
- Even with the "drop_items" attribute false, dropping an item will cause it to disappear from the inventory (but no item entity will be spawned as a result of the drop)
