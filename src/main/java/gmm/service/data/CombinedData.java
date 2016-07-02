package gmm.service.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import gmm.collections.Set;

@Service
public class CombinedData {
	
	private boolean isCustomAdminBannerActive = true;
	private String customAdminBanner = "";
	final private Map<Set<Long>, String> tasksToLinkKeys = new HashMap<>();

	public String getCustomAdminBanner() {
		return customAdminBanner;
	}

	public void setCustomAdminBanner(String customAdminBanner) {
		Objects.requireNonNull(customAdminBanner);
		this.customAdminBanner = customAdminBanner;
	}

	public boolean isCustomAdminBannerActive() {
		return isCustomAdminBannerActive;
	}

	public void setCustomAdminBannerActive(boolean isCustomAdminBannerActive) {
		this.isCustomAdminBannerActive = isCustomAdminBannerActive;
	}
	
	public Map<Set<Long>, String> getTaskToLinkKeys() {
		return tasksToLinkKeys;
	}
}
