/*
 * This file is part of the AutoModpack project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2023 Skidam and contributors
 *
 * AutoModpack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AutoModpack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with AutoModpack.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.skidam.automodpack;

import pl.skidam.automodpack.client.ScreenImpl;
import pl.skidam.automodpack.client.audio.AudioManager;
import pl.skidam.automodpack.modpack.Commands;
import pl.skidam.automodpack.networking.ModPackets;
import pl.skidam.automodpack_common.GlobalVariables;
import pl.skidam.automodpack_core.loader.LoaderManager;
import pl.skidam.automodpack_core.loader.LoaderService;
import pl.skidam.automodpack_core.screen.ScreenManager;
import pl.skidam.automodpack_server.modpack.HttpServer;

import static pl.skidam.automodpack_common.GlobalVariables.*;

//#if FORGE
//$$ import net.minecraftforge.common.MinecraftForge;
//$$ import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//$$ import net.minecraftforge.fml.common.Mod;
//$$
//$$ @Mod(MOD_ID)
//#endif
public class AutoModpack {

//#if FORGE
//$$public AutoModpack() {
//$$    MinecraftForge.EVENT_BUS.register(this);
//#elseif FABRICLIKE
    public static void onInitialize() {
//#endif

        preload = false;
        ScreenManager.screenImpl = new ScreenImpl();

        long start = System.currentTimeMillis();
        LOGGER.info("Launching AutoModpack...");

        if (new LoaderManager().getEnvironmentType() == LoaderService.EnvironmentType.SERVER) {
            if (serverConfig.generateModpackOnStart) {
                LOGGER.info("Generating modpack...");
                long genStart = System.currentTimeMillis();
                if (ModpackGenAdditions.generate()) {
                    LOGGER.info("Modpack generated! took " + (System.currentTimeMillis() - genStart) + "ms");
                } else {
                    LOGGER.error("Failed to generate modpack!");
                }
            }
            ModPackets.registerS2CPackets();
        } else {
            ModPackets.registerC2SPackets();
//#if FORGE
//$$        new AudioManager(FMLJavaModLoadingContext.get().getModEventBus());
//#else
            new AudioManager();
//#endif
        }

        Commands.register();

        LOGGER.info("AutoModpack launched! took " + (System.currentTimeMillis() - start) + "ms");
    }

    public static void afterSetupServer() {
        if (new LoaderManager().getEnvironmentType() != LoaderService.EnvironmentType.SERVER) {
            return;
        }

        new HttpServer(serverConfig);
        GlobalVariables.serverFullyStarted = true;
    }

    public static void beforeShutdownServer() {
        if (new LoaderManager().getEnvironmentType() != LoaderService.EnvironmentType.SERVER) {
            return;
        }

        if (HttpServer.fileChangeChecker != null) {
            HttpServer.fileChangeChecker.stopChecking();
        }

        HttpServer.stop();
    }
}