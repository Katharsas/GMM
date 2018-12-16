package gmm.domain.task.asset;

import java.util.concurrent.ConcurrentHashMap;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import gmm.collections.HashSet;
import gmm.collections.Set;

public class ModelProperties extends AssetProperties {

	@XStreamAsAttribute
	private final int polyCount;
	
	@SuppressWarnings("rawtypes") // fix compatibility with string asset names
	private /*final*/ Set/*<AssetName>*/ textureNames;
	
	@XStreamOmitField
	private final java.util.Set<TextureTask> textureTasks = ConcurrentHashMap.newKeySet();
	
	ModelProperties() {
		polyCount = -1;
		textureNames = null;
	}
	
	public ModelProperties(int polyCount, Set<AssetName> textureNames) {
		this.polyCount = polyCount;
		this.textureNames = textureNames.copy();
	}
	
	@SuppressWarnings("unchecked")
	private Object readResolve() {
		// fix compatibility with string asset names
		if (!textureNames.isEmpty()) {
			if (textureNames.iterator().next().getClass().equals(String.class)) {
				final Set<String> textureNamesString = new HashSet<>(String.class, textureNames);
				textureNames = new HashSet<>(AssetName.class);
				textureNamesString.forEach(name -> {
					textureNames.add(new AssetName(name));
				});
			}
		}
		return this;
	}
	
	@Deprecated
	@Override
	public void assertAttributes() {
		super.assertAttributes();
		if (polyCount < 0 || textureNames == null) {
			throw new IllegalStateException(assertAttributesException);
		}
	}
	
	public final int getPolyCount() {
		return polyCount;
	}

	
	@SuppressWarnings("unchecked")
	public final Set<AssetName> getTextureNames() {
		return textureNames;
	}
	
	public java.util.Set<TextureTask> getTextureTasks() {
		return textureTasks;
	}
}
