# Builds the LP test stations east (+X) of the executing position. Stand on open ground, face south.
fill ~-1 ~ ~-1 ~52 ~4 ~6 air
fill ~-1 ~-1 ~-1 ~52 ~-1 ~6 smooth_stone
execute positioned ~-1 ~-1 ~-1 run kill @e[type=item,dx=54,dy=6,dz=8]

# Kit: LP tools and spare parts
setblock ~ ~ ~2 chest{Items:[{Slot:0b,id:"logisticspipes:hud_glasses",Count:1b},{Slot:1b,id:"logisticspipes:pipe_manager",Count:1b},{Slot:2b,id:"logisticspipes:pipe_controller",Count:1b},{Slot:3b,id:"logisticspipes:remote_orderer",Count:1b},{Slot:4b,id:"logisticspipes:logistics_programmer",Count:1b},{Slot:5b,id:"logisticspipes:guide_book",Count:1b},{Slot:9b,id:"logisticspipes:pipe_basic",Count:16b},{Slot:10b,id:"logisticspipes:pipe_transport_basic",Count:16b},{Slot:11b,id:"logisticspipes:pipe_chassis_mk2",Count:4b},{Slot:12b,id:"logisticspipes:pipe_request",Count:2b},{Slot:13b,id:"logisticspipes:pipe_crafting",Count:2b},{Slot:14b,id:"logisticspipes:pipe_satellite",Count:2b},{Slot:15b,id:"logisticspipes:pipe_supplier",Count:2b},{Slot:16b,id:"logisticspipes:pipe_firewall",Count:2b},{Slot:18b,id:"logisticspipes:module_item_sink",Count:4b},{Slot:19b,id:"logisticspipes:module_provider",Count:4b},{Slot:20b,id:"logisticspipes:module_extractor",Count:2b},{Slot:21b,id:"logisticspipes:module_crafter",Count:2b},{Slot:22b,id:"logisticspipes:power_junction",Count:2b},{Slot:23b,id:"logisticspipes:creative_power_source",Count:2b},{Slot:24b,id:"logisticspipes:crafting_table",Count:2b}]}
setblock ~ ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"KIT"','"LP tools and"','"spare parts"','""']}}

# A: sorting. Extractor empties the input chest; cobble and dirt go to their ItemSinks, sand to the default route.
setblock ~2 ~ ~2 chest{Items:[{Slot:0b,id:"minecraft:cobblestone",Count:16b},{Slot:1b,id:"minecraft:dirt",Count:16b},{Slot:2b,id:"minecraft:sand",Count:16b}]}
setblock ~2 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"extractor"}}
setblock ~2 ~ ~4 logisticspipes:power_junction
setblock ~2 ~ ~5 logisticspipes:creative_power_source
setblock ~3 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~4 ~ ~2 chest
setblock ~4 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"item_sink",filterInvitemsCount:9,filterInvitems:[{index:0,id:"minecraft:cobblestone",Count:1b}]}}
setblock ~5 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~6 ~ ~2 chest
setblock ~6 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"item_sink",filterInvitemsCount:9,filterInvitems:[{index:0,id:"minecraft:dirt",Count:1b}]}}
setblock ~7 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~8 ~ ~2 chest
setblock ~8 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_basic",defaultdestination:1b}
setblock ~2 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"A: SORTING"','"input: 16 each"','"cobble dirt sand"','"empties itself"']}}
setblock ~4 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"A: ItemSink"','"gets 16"','"cobblestone"','""']}}
setblock ~6 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"A: ItemSink"','"gets 16 dirt"','""','""']}}
setblock ~8 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"A: default"','"route gets"','"16 sand"','""']}}

# B: supplier. Supplier pipe keeps 16 cobblestone in its chest, pulled from the provider chest.
setblock ~12 ~ ~2 chest{Items:[{Slot:0b,id:"minecraft:cobblestone",Count:64b}]}
setblock ~12 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"provider"}}
setblock ~12 ~ ~4 logisticspipes:power_junction
setblock ~12 ~ ~5 logisticspipes:creative_power_source
setblock ~13 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~14 ~ ~2 chest
setblock ~14 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_supplier",supplierInvitemsCount:9,supplierInvitems:[{index:0,id:"minecraft:cobblestone",Count:16b}]}
setblock ~12 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"B: provider"','"64 cobble,"','"ends at 48"','""']}}
setblock ~14 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"B: SUPPLIER"','"keeps 16"','"cobblestone"','""']}}

# C: crafting on demand. Supplier wants 8 sticks; the crafting pipe crafts them from provider planks.
setblock ~18 ~ ~2 chest{Items:[{Slot:0b,id:"minecraft:oak_planks",Count:16b}]}
setblock ~18 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"provider"}}
setblock ~18 ~ ~4 logisticspipes:power_junction
setblock ~18 ~ ~5 logisticspipes:creative_power_source
setblock ~19 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~20 ~ ~2 logisticspipes:crafting_table{matrixitemsCount:9,matrixitems:[{index:1,id:"minecraft:oak_planks",Count:1b},{index:4,id:"minecraft:oak_planks",Count:1b}]}
setblock ~20 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_crafting",dummyInvitemsCount:11,dummyInvitems:[{index:1,id:"minecraft:oak_planks",Count:1b},{index:4,id:"minecraft:oak_planks",Count:1b},{index:9,id:"minecraft:stick",Count:4b}]}
setblock ~21 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~22 ~ ~2 chest
setblock ~22 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_supplier",supplierInvitemsCount:9,supplierInvitems:[{index:0,id:"minecraft:stick",Count:8b}]}
setblock ~18 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"C: provider"','"16 planks,"','"ends at 12"','""']}}
setblock ~20 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"C: CRAFTING"','"planks->sticks"','"on demand"','""']}}
setblock ~22 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"C: supplier"','"gets 8 sticks"','""','""']}}

# D: manual request. Right-click the request pipe; request items or sticks (crafted), try the Content button.
setblock ~26 ~ ~2 chest{Items:[{Slot:0b,id:"minecraft:iron_ingot",Count:32b},{Slot:1b,id:"minecraft:diamond",Count:16b},{Slot:2b,id:"minecraft:oak_log",Count:64b},{Slot:3b,id:"minecraft:oak_planks",Count:32b},{Slot:4b,id:"minecraft:cobblestone",Count:64b}]}
setblock ~26 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"provider"}}
setblock ~26 ~ ~4 logisticspipes:power_junction
setblock ~26 ~ ~5 logisticspipes:creative_power_source
setblock ~27 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~28 ~ ~2 chest
setblock ~28 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_request_mk2"}
setblock ~29 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~30 ~ ~2 logisticspipes:crafting_table{matrixitemsCount:9,matrixitems:[{index:1,id:"minecraft:oak_planks",Count:1b},{index:4,id:"minecraft:oak_planks",Count:1b}]}
setblock ~30 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_crafting",dummyInvitemsCount:11,dummyInvitems:[{index:1,id:"minecraft:oak_planks",Count:1b},{index:4,id:"minecraft:oak_planks",Count:1b},{index:9,id:"minecraft:stick",Count:4b}]}
setblock ~26 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"D: provider"','"iron diamond"','"logs planks"','"cobble"']}}
setblock ~28 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"D: REQUEST"','"right-click pipe"','"order items +"','"Content button"']}}
setblock ~30 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"D: crafter"','"sticks show"','"as craftable"','""']}}

# E: drops. Pickaxe break drops the block; bare hand drops nothing (LP1 behaviour).
setblock ~34 ~ ~2 logisticspipes:frame
setblock ~35 ~ ~2 logisticspipes:power_junction
setblock ~36 ~ ~2 logisticspipes:security_station
setblock ~37 ~ ~2 logisticspipes:crafting_table
setblock ~38 ~ ~2 logisticspipes:crafting_table_fuzzy
setblock ~39 ~ ~2 logisticspipes:statistics_table
setblock ~40 ~ ~2 logisticspipes:power_provider_rf
setblock ~41 ~ ~2 logisticspipes:power_provider_eu
setblock ~42 ~ ~2 logisticspipes:power_provider_mj
setblock ~43 ~ ~2 logisticspipes:program_compiler
setblock ~34 ~ ~4 chest{Items:[{Slot:0b,id:"minecraft:wooden_pickaxe",Count:1b},{Slot:1b,id:"minecraft:iron_pickaxe",Count:1b}]}
setblock ~34 ~1 ~4 oak_sign[rotation=8]{front_text:{messages:['"E: DROPS"','"pickaxe: drops"','"hand: nothing"','""']}}

# F: finite power. Junction starts with 500 LP and has no source; sorting stops when it runs dry and the pipes turn red.
setblock ~47 ~ ~2 chest{Items:[{Slot:0b,id:"minecraft:cobblestone",Count:64b}]}
setblock ~47 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"extractor"}}
setblock ~47 ~ ~4 logisticspipes:power_junction{powerLevel:500}
setblock ~48 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~49 ~ ~2 chest
setblock ~49 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"item_sink",filterInvitemsCount:9,filterInvitems:[{index:0,id:"minecraft:cobblestone",Count:1b}]}}
setblock ~47 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"F: FINITE"','"POWER 500 LP"','"no source:"','"stops, pipes red"']}}

tellraw @a {"text":"LP test stations built: A sorting, B supplier, C crafting, D request, E drops, F finite power. Rerun to reset.","color":"green"}
