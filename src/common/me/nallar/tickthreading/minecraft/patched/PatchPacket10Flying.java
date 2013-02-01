package me.nallar.tickthreading.minecraft.patched;

import me.nallar.tickthreading.minecraft.TickThreading;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet10Flying;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;

public abstract class PatchPacket10Flying extends Packet10Flying {
	@Override
	public boolean canProcessAsync() {
		return true;
	}

	@Override
	public void processPacket(NetHandler par1NetHandler) {
		if (TickThreading.instance.antiCheat && moving && yPosition != -999.0D && stance != -999.0D && par1NetHandler instanceof NetServerHandler) {
			NetServerHandler nsh = (NetServerHandler) par1NetHandler;
			if (nsh.teleported) {
				nsh.teleported = false;
			} else {
				long currentTime = System.currentTimeMillis();
				EntityPlayerMP entityPlayerMP = nsh.playerEntity;
				long time = Math.min(5000, currentTime - nsh.lastMovement);
				double dX = (xPosition - nsh.lastPX);
				double dZ = (zPosition - nsh.lastPZ);
				if (time == 0) {
					nsh.lastPZ += dZ;
					nsh.lastPX += dX;
				} else {
					nsh.lastMovement = currentTime;
					if (time < 1) {
						time = 1;
					}
					double speed = (Math.sqrt(dX*dX + dZ*dZ) * 1000) / time;
					//Log.info(speed + "\t" + dX + '\t' + dZ + '\t' + time + '\t' + moving + '\t' + yPosition + '\t' + stance);
					if (Double.isInfinite(speed) || Double.isNaN(speed)) {
						speed = 1;
					}
					double averageSpeed = (nsh.averageSpeed = ((nsh.averageSpeed * 10 + speed) / 11));
					ServerConfigurationManager serverConfigurationManager = MinecraftServer.getServer().getConfigurationManager();
					if (!serverConfigurationManager.areCommandsAllowed(entityPlayerMP.username) && (averageSpeed > 50 || (!entityPlayerMP.isRiding() && averageSpeed > 20))) {
						nsh.kickPlayerFromServer("You moved too quickly!");
						serverConfigurationManager.sendPacketToAllPlayers(new Packet3Chat(entityPlayerMP.username + " was caught speed-hacking or has a terrible connection"));
					}
					nsh.lastPZ = this.zPosition;
					nsh.lastPX = this.xPosition;
				}
			}
		}
		par1NetHandler.handleFlying(this);
	}
}
