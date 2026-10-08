# Builds the LP test stations east (+X) of the executing position. Stand on open ground, face south.
fill ~-1 ~ ~-1 ~52 ~4 ~6 air
fill ~-1 ~-1 ~-1 ~52 ~-1 ~6 smooth_stone
execute positioned ~-1 ~-1 ~-1 run kill @e[type=item,dx=54,dy=6,dz=8]

# Kit: LP tools and spare parts
setblock ~ ~ ~2 chest{Items:[{Slot:0b,id:"logisticspipes:hud_glasses",count:1},{Slot:1b,id:"logisticspipes:pipe_manager",count:1},{Slot:2b,id:"logisticspipes:pipe_controller",count:1},{Slot:3b,id:"logisticspipes:remote_orderer",count:1},{Slot:4b,id:"logisticspipes:logistics_programmer",count:1},{Slot:5b,id:"logisticspipes:guide_book",count:1},{Slot:9b,id:"logisticspipes:pipe_basic",count:16},{Slot:10b,id:"logisticspipes:pipe_transport_basic",count:16},{Slot:11b,id:"logisticspipes:pipe_chassis_mk2",count:4},{Slot:12b,id:"logisticspipes:pipe_request",count:2},{Slot:13b,id:"logisticspipes:pipe_crafting",count:2},{Slot:14b,id:"logisticspipes:pipe_satellite",count:2},{Slot:15b,id:"logisticspipes:pipe_supplier",count:2},{Slot:16b,id:"logisticspipes:pipe_firewall",count:2},{Slot:18b,id:"logisticspipes:module_item_sink",count:4},{Slot:19b,id:"logisticspipes:module_provider",count:4},{Slot:20b,id:"logisticspipes:module_extractor",count:2},{Slot:21b,id:"logisticspipes:module_crafter",count:2},{Slot:22b,id:"logisticspipes:power_junction",count:2},{Slot:23b,id:"logisticspipes:creative_power_source",count:2},{Slot:24b,id:"logisticspipes:crafting_table",count:2}]}
setblock ~ ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"KIT"','"LP tools and"','"spare parts"','""']}}

# A: sorting. Extractor empties the input chest; cobble and dirt go to their ItemSinks, sand to the default route.
setblock ~2 ~ ~2 chest{Items:[{Slot:0b,id:"minecraft:cobblestone",count:16},{Slot:1b,id:"minecraft:dirt",count:16},{Slot:2b,id:"minecraft:sand",count:16}]}
setblock ~2 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"extractor"}}
setblock ~2 ~ ~4 logisticspipes:power_junction
setblock ~2 ~ ~5 logisticspipes:creative_power_source
setblock ~3 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~4 ~ ~2 chest
setblock ~4 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"item_sink",filterInvitemsCount:9,filterInvitems:[{index:0,id:"minecraft:cobblestone",count:1}]}}
setblock ~5 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~6 ~ ~2 chest
setblock ~6 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"item_sink",filterInvitemsCount:9,filterInvitems:[{index:0,id:"minecraft:dirt",count:1}]}}
setblock ~7 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~8 ~ ~2 chest
setblock ~8 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_basic",defaultdestination:1b}
setblock ~2 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"A: SORTING"','"input: 16 each"','"cobble dirt sand"','"empties itself"']}}
setblock ~4 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"A: ItemSink"','"gets 16"','"cobblestone"','""']}}
setblock ~6 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"A: ItemSink"','"gets 16 dirt"','""','""']}}
setblock ~8 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"A: default"','"route gets"','"16 sand"','""']}}

# B: supplier. Supplier pipe keeps 16 cobblestone in its chest, pulled from the provider chest.
setblock ~12 ~ ~2 chest{Items:[{Slot:0b,id:"minecraft:cobblestone",count:64}]}
setblock ~12 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"provider"}}
setblock ~12 ~ ~4 logisticspipes:power_junction
setblock ~12 ~ ~5 logisticspipes:creative_power_source
setblock ~13 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~14 ~ ~2 chest
setblock ~14 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_supplier",supplierInvitemsCount:9,supplierInvitems:[{index:0,id:"minecraft:cobblestone",count:16}]}
setblock ~12 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"B: provider"','"64 cobble,"','"ends at 48"','""']}}
setblock ~14 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"B: SUPPLIER"','"keeps 16"','"cobblestone"','""']}}

# C: crafting on demand. Supplier wants 8 sticks; the crafting pipe crafts them from provider planks.
setblock ~18 ~ ~2 chest{Items:[{Slot:0b,id:"minecraft:oak_planks",count:16}]}
setblock ~18 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"provider"}}
setblock ~18 ~ ~4 logisticspipes:power_junction
setblock ~18 ~ ~5 logisticspipes:creative_power_source
setblock ~19 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~20 ~ ~2 logisticspipes:crafting_table{matrixitemsCount:9,matrixitems:[{index:1,id:"minecraft:oak_planks",count:1},{index:4,id:"minecraft:oak_planks",count:1}]}
setblock ~20 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_crafting",dummyInvitemsCount:11,dummyInvitems:[{index:1,id:"minecraft:oak_planks",count:1},{index:4,id:"minecraft:oak_planks",count:1},{index:9,id:"minecraft:stick",count:4}]}
setblock ~21 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~22 ~ ~2 chest
setblock ~22 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_supplier",supplierInvitemsCount:9,supplierInvitems:[{index:0,id:"minecraft:stick",count:8}]}
setblock ~18 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"C: provider"','"16 planks,"','"ends at 12"','""']}}
setblock ~20 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"C: CRAFTING"','"planks->sticks"','"on demand"','""']}}
setblock ~22 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"C: supplier"','"gets 8 sticks"','""','""']}}

# D: manual request. Right-click the request pipe; request items or sticks (crafted), try the Content button.
setblock ~26 ~ ~2 chest{Items:[{Slot:0b,id:"minecraft:iron_ingot",count:32},{Slot:1b,id:"minecraft:diamond",count:16},{Slot:2b,id:"minecraft:oak_log",count:64},{Slot:3b,id:"minecraft:oak_planks",count:32},{Slot:4b,id:"minecraft:cobblestone",count:64}]}
setblock ~26 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"provider"}}
setblock ~26 ~ ~4 logisticspipes:power_junction
setblock ~26 ~ ~5 logisticspipes:creative_power_source
setblock ~27 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~28 ~ ~2 chest
setblock ~28 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_request_mk2"}
setblock ~29 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~30 ~ ~2 logisticspipes:crafting_table{matrixitemsCount:9,matrixitems:[{index:1,id:"minecraft:oak_planks",count:1},{index:4,id:"minecraft:oak_planks",count:1}]}
setblock ~30 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_crafting",dummyInvitemsCount:11,dummyInvitems:[{index:1,id:"minecraft:oak_planks",count:1},{index:4,id:"minecraft:oak_planks",count:1},{index:9,id:"minecraft:stick",count:4}]}
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
setblock ~34 ~ ~4 chest{Items:[{Slot:0b,id:"minecraft:wooden_pickaxe",count:1},{Slot:1b,id:"minecraft:iron_pickaxe",count:1}]}
setblock ~34 ~1 ~4 oak_sign[rotation=8]{front_text:{messages:['"E: DROPS"','"pickaxe: drops"','"hand: nothing"','""']}}

# F: finite power. Junction starts with 500 LP and has no source; sorting stops when it runs dry and the pipes turn red.
setblock ~47 ~ ~2 chest{Items:[{Slot:0b,id:"minecraft:cobblestone",count:64}]}
setblock ~47 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"extractor"}}
setblock ~47 ~ ~4 logisticspipes:power_junction{powerLevel:500}
setblock ~48 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_transport_basic"}
setblock ~49 ~ ~2 chest
setblock ~49 ~ ~3 logisticspipes:pipe{pipeIdName:"logisticspipes:pipe_chassis_mk1","modules.listSize":1,"modules.listItem.0":{"slotted_module.slot":0,"slotted_module.name":"item_sink",filterInvitemsCount:9,filterInvitems:[{index:0,id:"minecraft:cobblestone",count:1}]}}
setblock ~47 ~1 ~2 oak_sign[rotation=8]{front_text:{messages:['"F: FINITE"','"POWER 500 LP"','"no source:"','"stops, pipes red"']}}

tellraw @a {"text":"LP test stations built: A sorting, B supplier, C crafting, D request, E drops, F finite power. Rerun to reset.","color":"green"}
tellraw @a {"text":"With Mekanism installed: /function lp_testbed:power_mekanism powers A-D with FE instead of LP creative sources.","color":"gray"}
