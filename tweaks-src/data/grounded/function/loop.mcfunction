# -15% base movement speed (walking, sprinting and sneaking all scale from it).
# Change -0.15 to tune. The command fails silently if the player already has the modifier,
# and re-applies it after respawn.
execute as @a run attribute @s minecraft:movement_speed modifier add grounded:slower_walk -0.15 add_multiplied_base
schedule function grounded:loop 20t replace
