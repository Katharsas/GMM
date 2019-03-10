package gmm.domain.task.asset;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import gmm.collections.HashSet;
import gmm.collections.Set;

/**
 * @see {@link AssetProperties}
 * @author Jan Mothes
 */
public class ModelProperties extends AssetProperties {
	
	/**
	 * Stores non-persistent, runtime-generated references and info that are useful for view rendering.
	 */
	public static class ViewModel {
		
		// redundant compared to "textureNames" field; this mapping to tasks is used by view
		private final java.util.Set<TextureTask> texturesWithTasks = ConcurrentHashMap.newKeySet();
		private final java.util.Set<AssetName> texturesWithoutTasks = ConcurrentHashMap.newKeySet();
		
		public ViewModel(Set<AssetName> textureNames) {
			texturesWithoutTasks.addAll(textureNames);
		}
		
		public java.util.Set<TextureTask> getTexturesWithTasks() {
			return texturesWithTasks;
		}
		
		public java.util.Set<AssetName> getTexturesWithoutTasks() {
			return texturesWithoutTasks;
		}
		
		public boolean addTextureTask(TextureTask task) {
			texturesWithTasks.add(task);
			return texturesWithoutTasks.remove(task.getAssetName());
		}
		
		public boolean removeTextureTask(TextureTask task) {
			texturesWithTasks.remove(task);
			return texturesWithoutTasks.add(task.getAssetName());
		}
	}

	@XStreamAsAttribute
	private final int polyCount;
	
	// TODO: change to proper type and possibly use immutable set
	@SuppressWarnings("rawtypes") // fix compatibility with string asset names
	private /*final*/ Set/*<AssetName>*/ textureNames;
	
	@XStreamOmitField
	private ViewModel viewModel;
	
	ModelProperties() {
		polyCount = -1;
		textureNames = null;
	}
	
	public ModelProperties(int polyCount, Set<AssetName> textureNames) {
		if (polyCount <= 0) {
			throw new IllegalArgumentException("PolyCount must be positive!");
		}
		Objects.requireNonNull(textureNames);
		this.polyCount = polyCount;
		this.textureNames = textureNames;
		viewModel = new ViewModel(textureNames);
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
		viewModel = new ViewModel(textureNames);
		return this;
	}
	
	public int getPolyCount() {
		return polyCount;
	}
	
	public Set<AssetName> getTextureNames() {
		return textureNames.copy();
	}
	
	public ViewModel getViewModel() {
		return viewModel;
	}
}
