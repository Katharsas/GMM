package gmm.service.data;

import java.util.Objects;

import org.springframework.stereotype.Service;

@Service
public class CombinedData {
	private String customAdminBanner = "";

	public String getCustomAdminBanner() {
		return customAdminBanner;
	}

	public void setCustomAdminBanner(String customAdminBanner) {
		Objects.requireNonNull(customAdminBanner);
		this.customAdminBanner = customAdminBanner;
	}
}
