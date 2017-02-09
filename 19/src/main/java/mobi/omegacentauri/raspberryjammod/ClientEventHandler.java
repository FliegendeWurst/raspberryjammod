package mobi.omegacentauri.raspberryjammod;

import java.io.IOException;
import java.net.InetSocketAddress;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ClientEventHandler implements IWorldEventListener {
	private volatile boolean nightVision = false;
	private int clientTickCount = 0;
	private MCEventHandlerClientOnly apiEventHandler = null;
	private APIServer apiServer = null;
//	private boolean registeredCommands = false;
	private boolean blockBroken = false;

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (nightVision && clientTickCount % 1024 == 0) {
			Minecraft mc = Minecraft.getMinecraft();
			if (mc != null) {
				EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
				
				if (player != null) {
					player.addPotionEffect(new PotionEffect(MobEffects.nightVision, 4096));
				}
			}
		}
		clientTickCount++;
	} 

	public void setNightVision(boolean b) {
		nightVision = b;
	}

	public boolean getNightVision() {
		return nightVision;
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
		try {
			Object address = event.getManager().getRemoteAddress();
			if (address instanceof InetSocketAddress) {
				RaspberryJamMod.serverAddress = ((InetSocketAddress) address).getAddress().getHostAddress();
				RaspberryJamMod.LOGGER.info("Server address "+RaspberryJamMod.serverAddress);
			}
			else {
				RaspberryJamMod.LOGGER.info("No IP address");
			}
		} catch(Exception e) {
			RaspberryJamMod.serverAddress = null;
		}
	}


	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onWorldUnloaded(WorldEvent.Unload event) {
		if (! RaspberryJamMod.clientOnlyAPI)
			return;

		RaspberryJamMod.LOGGER.info("Closing world: "+event.getWorld());
		
		closeAPI();
	}	
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onSpecialsPre(RenderLivingEvent.Specials.Pre<?> event) {
		if (RaspberryJamMod.noNameTags)
			event.setCanceled(true);
	}
	
	@SideOnly(Side.CLIENT)
	public void registerCommand(ScriptExternalCommand c) {
		net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(c);
		RaspberryJamMod.scriptExternalCommands.add(c);
	}
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onWorldLoaded(WorldEvent.Load event) {
		event.getWorld().addEventListener(this);

		RaspberryJamMod.synchronizeConfig();
		RaspberryJamMod.LOGGER.info("Loading world: "+event.getWorld());

		if (RaspberryJamMod.clientOnlyAPI) {
			registerCommand(new PythonExternalCommand(true));
			registerCommand(new AddPythonExternalCommand(true));
		}
//		else 
//       {
			registerCommand(new LocalPythonExternalCommand(true));
			registerCommand(new AddLocalPythonExternalCommand(true));
//		}
		
		if (!RaspberryJamMod.clientOnlyAPI)
			return;
		
		if (apiEventHandler == null) {
            apiEventHandler = new MCEventHandlerClientOnly();
			MinecraftForge.EVENT_BUS.register(apiEventHandler);
		}

        if (apiServer == null)
			try {
				RaspberryJamMod.LOGGER.info("RaspberryJamMod client only API");
				RaspberryJamMod.apiActive = true;
				if (apiServer == null) {
					RaspberryJamMod.currentPortNumber = -1;
					apiServer = new APIServer(apiEventHandler, RaspberryJamMod.portNumber, RaspberryJamMod.searchForPort ? 65535 : RaspberryJamMod.portNumber, 
							RaspberryJamMod.wsPort,
							true);
					RaspberryJamMod.currentPortNumber = apiServer.getPortNumber();
	
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								apiServer.communicate();
							} catch(IOException e) {
								RaspberryJamMod.LOGGER.catching(e);
							}
							finally {
								closeAPI();
							}
						}
					}).start();
				}
			}
			catch (Exception e) {}
	}

	public void playEvent(EntityPlayer player, int code, BlockPos pos, int blockId) {
		EntityPlayerSP realPlayer = Minecraft.getMinecraft().thePlayer;
		if (code == 2001 && player == null && realPlayer != null
				&& realPlayer.getDistanceSq(pos) < 46
				&& realPlayer.rayTrace(4.5, 0).getBlockPos().equals(pos)
				&& realPlayer.isSwingInProgress) { // 50 == 5 blocks (max. 4)
			this.blockBroken = true;
		}
	}

	public boolean getBlockBroken() {
		boolean x = this.blockBroken;
		if (this.blockBroken) {
			this.blockBroken = false;
		}
		return x;
	}

	public void closeAPI() {
		RaspberryJamMod.closeAllScripts();
		for (int i = RaspberryJamMod.scriptExternalCommands.size()-1; i>=0; i--) {
			ScriptExternalCommand c = RaspberryJamMod.scriptExternalCommands.get(i);
			if (c.clientSide) {
				RaspberryJamMod.LOGGER.info("Unregistering "+c.getClass());
				RaspberryJamMod.unregisterCommand(net.minecraftforge.client.ClientCommandHandler.instance,c);
				RaspberryJamMod.scriptExternalCommands.remove(i);
			}		
		}
		RaspberryJamMod.apiActive = false;
		if (apiEventHandler != null) {
            MinecraftForge.EVENT_BUS.unregister(apiEventHandler);
            apiEventHandler = null;
		}
		if (apiServer != null) {
			apiServer.close();
			apiServer = null;
		}
	}

	public void notifyBlockUpdate(World worldIn, BlockPos pos,
			IBlockState oldState, IBlockState newState, int flags) {}

	public void notifyLightSet(BlockPos pos) {}

	public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2,
			int y2, int z2) {}

	public void playSoundToAllNearExcept(EntityPlayer player,
			SoundEvent soundIn, SoundCategory category, double x, double y,
			double z, float volume, float pitch) {}

	public void playRecord(SoundEvent soundIn, BlockPos pos) {}

	public void spawnParticle(int particleID, boolean ignoreRange,
			double xCoord, double yCoord, double zCoord, double xOffset,
			double yOffset, double zOffset, int... parameters) {}

	public void onEntityAdded(Entity entityIn) {}

	public void onEntityRemoved(Entity entityIn) {}

	public void broadcastSound(int soundID, BlockPos pos, int data) {}

	public void playAuxSFX(EntityPlayer player, int sfxType,
			BlockPos blockPosIn, int data) {}

	@Override
	public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {}
}
