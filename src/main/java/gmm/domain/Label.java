package gmm.domain;

import java.util.Objects;

public class Label implements Linkable{
	private final String label;
	public Label(String label) {
		Objects.requireNonNull(label);
		this.label = label;
	}
	@Override
	public int hashCode() {
		return label.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (obj instanceof Label) {
			return this.label.equals(((Label) obj).label);
		}
		if (obj instanceof String) {
			return this.label.equals((String) obj);
		}
		return false;
	}
	@Override
	public String toString() {
		return this.label.toString();
	}
	@Override
	public String getIdLink() {
		return this.toString();
	}
}
