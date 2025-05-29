package id.kuato.woahelper.main;

import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.topjohnwu.superuser.ShellUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import id.kuato.woahelper.R;
import id.kuato.woahelper.preference.pref;

public class quickboot_tile extends TileService {

	String finduefi;
	String device;
	String win;
	String findwin;
	String winpath;
	String findboot;
	String boot;

	// Called when your app can update your tile.
	@Override
	public void onStartListening() {
		super.onStartListening();
		final Tile tile = getQsTile();
		checkdevice();
		checkuefi();
		findwin = ShellUtils.fastCmd("find /dev/block | grep -i -E \"win|mindows|windows\" | head -1");
		if (finduefi.isEmpty() || (isSecure() && !pref.getSecure(this))||findwin.isEmpty()) tile.setState(0);
		else tile.setState(1);
		if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
			tile.setSubtitle("");
		}
		tile.updateTile();

	}

	// Called when the user taps on your tile in an active or inactive state.
	@Override
	public void onClick() {
		super.onClick();
		final Tile tile = getQsTile();
		if (2 == tile.getState() || !pref.getCONFIRM(this)) {
			mount();
			String found = ShellUtils.fastCmd("ls " + (pref.getMountLocation(this) ? "/mnt/Windows" : "/mnt/sdcard/Windows") + " | grep boot.img");
			if (pref.getMountLocation(this)) {
				if (pref.getBACKUP(this) || (!pref.getAUTO(this) && found.isEmpty())) {
					ShellUtils.fastCmd("dd bs=8m if=" + boot + " of=/mnt/Windows/boot.img");
					final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM HH:mm", Locale.US);
					final String currentDateAndTime = sdf.format(new Date());
					pref.setDATE(this, currentDateAndTime);
				}
			} else {
				if (pref.getBACKUP(this) || (!pref.getAUTO(this) && found.isEmpty())) {
					ShellUtils.fastCmd("dd bs=8m if=" + boot + " of=/mnt/sdcard/Windows/boot.img");
					final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM HH:mm", Locale.US);
					final String currentDateAndTime = sdf.format(new Date());
					pref.setDATE(this, currentDateAndTime);
				}
			}
			found = ShellUtils.fastCmd("find /sdcard | grep boot.img");
			if (pref.getBACKUP_A(this) || (!pref.getAUTO_A(this) && found.isEmpty())) {
				ShellUtils.fastCmd("dd bs=8m if=" + boot + " of=/sdcard/boot.img");
				final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM HH:mm", Locale.US);
				final String currentDateAndTime = sdf.format(new Date());
				pref.setDATE(this, currentDateAndTime);
			}
			flash();
			ShellUtils.fastCmd("su -c svc power reboot");
		} else {
			tile.setState(2);
			if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
				tile.setSubtitle("Press again");
			}
			tile.updateTile();
		}
	}

	private void mount() {
		ShellUtils.fastCmd("mkdir " + winpath + " || true");
		ShellUtils.fastCmd("su -mm -c /data/data/id.kuato.woahelper/files/mount.ntfs " + win + " " + winpath);
	}

	public void flash() {
		ShellUtils.fastCmd("dd if=" + finduefi + " of=/dev/block/bootdevice/by-name/boot$(getprop ro.boot.slot_suffix) bs=16m");
	}

	public void checkdevice() {
		device = ShellUtils.fastCmd("getprop ro.product.device");
	}

	public void checkuefi() {
		checkdevice();
		finduefi = ShellUtils.fastCmd(getString(R.string.uefiChk));
		findwin = ShellUtils.fastCmd("find /dev/block | grep -i -E \"win|mindows|windows\" | head -1");
		win = ShellUtils.fastCmd("realpath " + findwin);
		winpath = (pref.getMountLocation(this) ? "/mnt/Windows" : "/mnt/sdcard/Windows");
		findboot = ShellUtils.fastCmd("find /dev/block | grep boot$(getprop ro.boot.slot_suffix)");
		if (findboot.isEmpty()) findboot = ShellUtils.fastCmd("find /dev/block | grep BOOT$(getprop ro.boot.slot_suffix)");
		boot = ShellUtils.fastCmd("realpath " + findboot);
	}

}
