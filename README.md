# Admin Mode
A plugin that gives administrators (particularly in survival mode) abilities that make their jobs easier to do.  
*Still in development/testing*

## Features
- Upon entering admin mode, the player's current inventory, food, experience, status effects, and location is stored in memory. When the player leaves admin mode, their inventory, food, experience, status effects, and location are restored
- Configurable list of permissions that are given to players upon entering admin mode
- A command to easily remove all permissions that have been given to players by this plugin ('/amclearperms')
- Entering admin mode with a victim name (the player the admin is going to help) and a reason. Both of which are entirely optional ('/adminmode [reason]' or '/am [reason]')
- Messages that broadcast to other admins (such as when a player enters/leaves admin mode) (uses the permission 'adminmode.notify')
- View a list of players that are currently in admin mode ('/amlist')
- Kick a player out of admin mode ('/amkick')
- Modifiable (on/off) attributes controlling the abilities that players in admin mode have (e.g. taking damage, giving damage, block and entity interactions, inventory interaction, etc.)
- Players in admin mode are put back to their pre-admin mode state when they disconnect from the server
- Crash safe. If the server crashes, all players that were in admin mode are restored back to their pre-admin mode state upon reconnection to the server. (If the server crashed, the file amplayerdata.dat will contain all the information needed for the plugin to restore the player's pre-admin mode state)

## Permissions
```
# Gives access to '/adminmode' and '/am'. Recommended rank: admins
adminmode.command.adminmode

# Gives access to '/amlist'. Recommended rank: higher-class admins
adminmode.command.list

# Gives access to '/amkick'. Recommended rank: higher-class admins
adminmode.command.kick

# Gives access to '/amclearperms'. Recommended rank: server operators
adminmode.command.clearperms

# Gives access to 'amreload'. Recommended rank: server operators
adminmode.command.reload

# Allows the player to see admin mode messages in chat (such as a player leaving/entering admin mode). Recommended rank: admins
adminmode.notify
```

## Commands
```
# Command argument legend:
# [optional]
# <required>

# Toggle on/off admin mode
# [reason] is optional and is only displayed when entering admin mode: "'Player' has entered admin mode! Reason: [reason]"
# Otherwise the message is: "'Player' has entered admin mode!" or "'Player' has left admin mode!"
/am [reason]
/adminmode [reason]

# Gives a list of players currently in admin mode
/amlist

# Kick a player out of admin mode
/amkick <player>

# Clears all contextual permissions ("contextual" as in permissions that are set "true" only when they are in the correct context (i.e. in admin mode)) from all players
# This command should be ran after updating the 'permissions' option in the configuration file (adminmode.conf) to not leave any leftover contextual permissions from the previous configuration
# This command should also be ran if you wish to completely remove this plugin from your server, not leaving any leftover contextual permissions behind
# Note that it will clear contextual permissions for all players, regardless if they are online or offline, and regardless if they are in admin mode. If you run this command while (a) player(s) is in admin mode, that/those player(s) will need to re-enable admin mode to get their contextual permissions back/updated
/amclearperms

# Reloads the configuration
/amreload
```

## Default Configuration File
```
# Permissions given to players in admin mode
# Note: whenever you change or update these permissions, you should run '/amclearperms' to clear all previously existing permissions given by the plugin
permissions: [
    "prism",
    "griefprevention"
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
    # damage_other_entities determines whether or not the player can damage other entities (including other players)
    damage_other_entities: false

    # interact_entity determines whether or not the player can right-click (interact with) an entity. Useful for preventing interaction with ridable entities (horses, saddled pigs, etc) and villagers
    interact_entities: false

    # break_blocks determines whether or not the player can break blocks
    break_blocks: false

    # place_blocks determines whether or not the player can place blocks
    place_blocks: false

    # interact_blocks determines whether or not the player can right-click interact with blocks (opening doors, trapdoors, buttons, etc.)
    interact_blocks: false

    # interact_tile_entity_carriers determines whether or not the player can open the inventories of blocks with inventories (chests, beacons, furnaces, droppers, hoppers, etc.)
    interact_tile_entity_carriers: true

    # drop_items determines whether the player can drop items / create item entities
    drop_items: false

    # pickup_items determines whether the player can pick up item entities
    pickup_items: false

    # interact_inventories determines whether items can be moved around in inventories (chests, ender chests, player inventory, shulker boxes, etc.)
    interact_inventories: false
}
```

### Known bugs
- '/amclearperms' *may* have a delay (up to 3 seconds or so) between command execution and the permissions being removed. Rarely may fail to work at all (just re-execute the command if this happens)
- In admin mode, with the "drop_items" attribute false, dropped items are returned to the player in the first available hotbar or grid slot. However, if a player drops an item while their inventory ui is open, the reappeared item may not display despite it actually being there.
- 'interact_blocks' attribute prevents the usage of right-click interaction with items in the player's hand if the player right-clicks on a non-air block. (e.g. Setting pos2 with the worldedit wand, Right-clicking a wall go /thru it with a compass)
- Player experience is not cached like the inventory. Any experience the player collects while in admin mode will be carried over when they leave admin mode. This was added intentionally to prevent problems with players not getting the same experience levels back when they enter and then leave admin mode.
- If the server crashes, some player data *may* sometimes not be recoverable after they reconnect to the server. In this case, the player's data in 'config/adminmode/amplayerdata.dat' should still contain mostly human-readable data which can be used to manually restore the player's data (e.g. using the '/give' command).
