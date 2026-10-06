package com.site21.bittermelon.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.site21.bittermelon.common.systems.rage.RageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import org.jetbrains.annotations.NotNull;

public class MimicPlayerTestCommand {
    public static final PermissionCheck PERMISSION_CHECK = new PermissionCheck.Require(Permissions.COMMANDS_ADMIN);

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mimicplayer")
                .requires(Commands.hasPermission(PERMISSION_CHECK))
                .executes(context -> {
                    ServerLevel level = context.getSource().getLevel();
                    ServerPlayer player = context.getSource().getPlayer();
                    RageUtil.setRage(player, 200);
                    context.getSource().sendSuccess(() -> Component.nullToEmpty("MimicPlayer test command executed"), false);
                    return 1;
                })
        );
    }
}
