package gmm.util;

import java.util.Objects;

public class TypedString {
	private String value;
	public TypedString(String value) {
		Objects.requireNonNull(value);
		this.value = value;
	}
	public String get() {
		return value;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		TypedString other = (TypedString) obj;
		return value.equals(other.value);
	}
	@Override
	public int hashCode() {
		return value.hashCode();
	};
}