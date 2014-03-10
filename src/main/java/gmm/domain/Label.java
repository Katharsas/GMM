package gmm.domain;

public class Label {
	private final String label;
	public Label(String label) {
		this.label = label;
	}
	@Override
	public int hashCode() {
		return (label == null) ? 0 : label.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (obj instanceof Label) {
			return this.label.equals(((Label) obj).get());
		}
		if (obj instanceof String) {
			return this.label.equals((String) obj);
		}
		return false;
	}
	private String get() {
		return label;
	}
	@Override
	public String toString() {
		return this.label.toString();
	}
}
