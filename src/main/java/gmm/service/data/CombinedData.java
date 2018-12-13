package gmm.service.data;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import gmm.collections.Set;

/**
 * Anything that is not task/users is in here.
 * @author Jan Mothes
 */
public class CombinedData {
	
	private boolean isCustomAdminBannerActive = true;
	private String customAdminBanner = "";
	
	final private Map<Set<Long>, String> tasksToLinkKeys = new ConcurrentHashMap<>();
	
	public CombinedData() {
	}

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
