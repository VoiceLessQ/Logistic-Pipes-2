# Needs Mekanism. Run from the same spot as build, after it: swaps the LP creative sources (A-D) for a Mekanism creative cube -> Universal Cable -> junction.
# Junctions start at 0 LP so you can watch FE arrive. setblock-placed cubes get no output side or energy, so both are set here.
setblock ~2 ~ ~6 mekanism:creative_energy_cube[facing=north]{energy_containers:[{container:0b,stored:9223372036854775807L}],component_config:{config0:[I;4,1,1,1,1,1],eject0:1b}}
setblock ~12 ~ ~6 mekanism:creative_energy_cube[facing=north]{energy_containers:[{container:0b,stored:9223372036854775807L}],component_config:{config0:[I;4,1,1,1,1,1],eject0:1b}}
setblock ~18 ~ ~6 mekanism:creative_energy_cube[facing=north]{energy_containers:[{container:0b,stored:9223372036854775807L}],component_config:{config0:[I;4,1,1,1,1,1],eject0:1b}}
setblock ~26 ~ ~6 mekanism:creative_energy_cube[facing=north]{energy_containers:[{container:0b,stored:9223372036854775807L}],component_config:{config0:[I;4,1,1,1,1,1],eject0:1b}}
setblock ~2 ~ ~5 mekanism:basic_universal_cable
setblock ~12 ~ ~5 mekanism:basic_universal_cable
setblock ~18 ~ ~5 mekanism:basic_universal_cable
setblock ~26 ~ ~5 mekanism:basic_universal_cable
data merge block ~2 ~ ~4 {powerLevel:0}
data merge block ~12 ~ ~4 {powerLevel:0}
data merge block ~18 ~ ~4 {powerLevel:0}
data merge block ~26 ~ ~4 {powerLevel:0}
tellraw @a {"text":"A-D now run on Mekanism FE (creative cube -> cable -> junction). Junctions reset to 0 LP.","color":"aqua"}
