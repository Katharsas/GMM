package gmm.service.data;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import gmm.collections.Set;
import gmm.web.forms.AssetTaskTemplateForm;

/**
 * Multithreading-safe.
 * Anything that is not task/users is in here.
 * @author Jan Mothes
 */
public class CombinedData {
	
	private boolean isCustomAdminBannerActive = true;
	private String customAdminBanner = "";
	private boolean isTaskAutoImportEnabled = false;
	private AssetTaskTemplateForm importTaskForm = null;
	
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
	
	public synchronized boolean isTaskAutoImportEnabled() {
		return isTaskAutoImportEnabled;
	}
	
	public synchronized void setTaskAutoImportEnabled(boolean isTaskAutoImportEnabled) {
		this.isTaskAutoImportEnabled = isTaskAutoImportEnabled;
	}
	
	public synchronized AssetTaskTemplateForm getImportTaskForm() {
		if (importTaskForm == null) {
			importTaskForm = new AssetTaskTemplateForm();
		}
		return importTaskForm;
	}

	public void setImportTaskForm(AssetTaskTemplateForm importTaskForm) {
		Objects.requireNonNull(importTaskForm);
		this.importTaskForm = importTaskForm;
	}

	public Map<Set<Long>, String> getTaskToLinkKeys() {
		return tasksToLinkKeys;
	}
}
