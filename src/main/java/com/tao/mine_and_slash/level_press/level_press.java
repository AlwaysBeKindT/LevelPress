package com.tao.mine_and_slash.level_press;

import com.tao.mine_and_slash.level_press.event.AiExchange;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("level_press")
public class level_press {

    public level_press() {
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(AiExchange.class);
    }
}
