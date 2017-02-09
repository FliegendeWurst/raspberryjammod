package mobi.omegacentauri.raspberryjammod;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class KeyBindHelper {

	private static Minecraft mc;
	private static HashMap<String, KeyBinding> BINDINGS = new HashMap<String, KeyBinding>();
	
	@SideOnly(Side.CLIENT)
	public static void init() {
		mc = Minecraft.getMinecraft();
		BINDINGS.put("ATTACK", mc.gameSettings.keyBindAttack);
		BINDINGS.put("BACK", mc.gameSettings.keyBindBack);
		BINDINGS.put("CHAT", mc.gameSettings.keyBindChat);
		BINDINGS.put("COMMAND", mc.gameSettings.keyBindCommand);
		BINDINGS.put("DROP", mc.gameSettings.keyBindDrop);
		BINDINGS.put("FORWARD", mc.gameSettings.keyBindForward);
		BINDINGS.put("FULLSCREEN", mc.gameSettings.keyBindFullscreen);
		BINDINGS.put("INVENTORY", mc.gameSettings.keyBindInventory);
		BINDINGS.put("JUMP", mc.gameSettings.keyBindJump);
		BINDINGS.put("LEFT", mc.gameSettings.keyBindLeft);
		BINDINGS.put("PICKBLOCK", mc.gameSettings.keyBindPickBlock);
		BINDINGS.put("PLAYERLIST", mc.gameSettings.keyBindPlayerList);
		BINDINGS.put("RIGHT", mc.gameSettings.keyBindRight);
		BINDINGS.put("SCREENSHOT", mc.gameSettings.keyBindScreenshot);
		BINDINGS.put("SNEAK", mc.gameSettings.keyBindSneak);
		BINDINGS.put("SPECTATOROUTLINES", mc.gameSettings.keyBindSpectatorOutlines);
		BINDINGS.put("SPRINT", mc.gameSettings.keyBindSprint);
		BINDINGS.put("SWAPHANDS", mc.gameSettings.keyBindSwapHands);
		BINDINGS.put("TOGGLEPERSPECTIVE", mc.gameSettings.keyBindTogglePerspective);
		BINDINGS.put("USEITEM", mc.gameSettings.keyBindUseItem);
	}

	@SideOnly(Side.CLIENT)
	public static void handleCommand(String cmd, Scanner scan) {
		init(); // user might have changed preferences
		String[] splits = cmd.split("\\."); // is a regex
		RaspberryJamMod.LOGGER.info("Splitted " + cmd + " into " + Arrays.toString(splits));
		if (splits.length != 2) {
			RaspberryJamMod.LOGGER.warn("Unknown key.command: " + cmd);
			return;
		}
		switch (splits[1]) {
		case "hold":
			holdKey(scan.next());
			break;
		case "tick":
			tickKey(scan.next());
			break;
		case "release":
			releaseKey(scan.next());
			break;
		default:
			RaspberryJamMod.LOGGER.warn("Unknown key.command: " + splits[1]);
			return;
		}
	}

	private static void holdKey(String cmd) {
		KeyBinding key = BINDINGS.get(cmd);
		if (key != null) {
			KeyBinding.setKeyBindState(key.getKeyCode(), true);
		} else {
			RaspberryJamMod.LOGGER.warn("Unknown key: " + cmd);
		}
	}
	
	private static void tickKey(String cmd) {
		KeyBinding key = BINDINGS.get(cmd);
		if (key != null) {
			KeyBinding.onTick(key.getKeyCode());
		} else {
			RaspberryJamMod.LOGGER.warn("Unknown key: " + cmd);
		}
	}
	
	private static void releaseKey(String cmd) {
		KeyBinding key = BINDINGS.get(cmd);
		if (key != null) {
			KeyBinding.setKeyBindState(key.getKeyCode(), false);
		} else {
			RaspberryJamMod.LOGGER.warn("Unknown key: " + cmd);
		}
	}
	
}