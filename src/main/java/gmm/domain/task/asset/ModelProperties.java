package gmm.domain.task.asset;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class ModelProperties extends AssetProperties {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@XStreamAsAttribute
	private final int polyCount;
	
	@SuppressWarnings("rawtypes")
	private Set textureNames;
	@XStreamOmitField
	private boolean textureNamesFixed = false;// fix compatibility with old 'Set<String> textureNames'
	
	@XStreamOmitField
	private Set<TextureTask> textureTasks = ConcurrentHashMap.newKeySet();
	
	
	public ModelProperties(int polyCount, Set<AssetName> textureNames) {
		this.polyCount = polyCount;
		this.textureNames = textureNames;
	}
	
	@Override
	public void assertAttributes() {
		super.assertAttributes();
		if (polyCount < 0 || textureNames == null) {
			throw new IllegalStateException(assertAttributesException);
		}
	}
	
	public int getPolyCount() {
		return polyCount;
	}

	
	@SuppressWarnings("unchecked")
	public Set<AssetName> getTextureNames() {
		if (!textureNamesFixed) {
			final Set<Object> maybeStrings =  textureNames;
			if(maybeStrings.isEmpty() || !maybeStrings.iterator().next().getClass().equals(String.class)) {
				textureNamesFixed = true;
			}
			if (!textureNamesFixed) {
				final Set<AssetName> tempTextureNames = new HashSet<>();
				maybeStrings.forEach(name -> {
					tempTextureNames.add(new AssetName((String)name));
				});
				this.textureNames = ImmutableSet.copyOf(tempTextureNames);
				textureNamesFixed = true;
			}
		}
//		System.out.println(textureNames);
//		if (textureNames == null) {
//			textureNames = ImmutableSet.of();
//		}
//		System.out.println(textureNames);
		return textureNames;// == null? ImmutableSet.of() :  textureNames;
	}
	
	public Set<TextureTask> getTextureTasks() {
		// no idea why this can be null. xstream sucks
		if (textureTasks == null) {
			textureTasks = ConcurrentHashMap.newKeySet();
		}
		return textureTasks;
	}
}
