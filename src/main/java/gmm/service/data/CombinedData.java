package gmm.service.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import gmm.collections.Set;

/**
 * @author Jan Mothes
 */
public class CombinedData {
	
	private boolean isCustomAdminBannerActive = true;
	private String customAdminBanner = "";
	
	// TODO make thread-safe
	final private Map<Set<Long>, String> tasksToLinkKeys = new HashMap<>();

	public synchronized String getCustomAdminBanner() {
		return customAdminBanner;
	}

	public synchronized void setCustomAdminBanner(String customAdminBanner) {
		Objects.requireNonNull(customAdminBanner);
		this.customAdminBanner = customAdminBanner;
	}

	public synchronized boolean isCustomAdminBannerActive() {
		return isCustomAdminBannerActive;
	}

	public synchronized void setCustomAdminBannerActive(boolean isCustomAdminBannerActive) {
		this.isCustomAdminBannerActive = isCustomAdminBannerActive;
	}
	
	public Map<Set<Long>, String> getTaskToLinkKeys() {
		return tasksToLinkKeys;
	}
}
