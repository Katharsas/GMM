package gmm.service.tasks;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import gmm.domain.User;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.AssetProperties;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.ModelTask;
import gmm.domain.task.asset.TextureTask;
import gmm.service.data.DataAccess;
import gmm.service.data.DataAccess.DataChangeCallback;
import gmm.service.data.DataChangeEvent;
import gmm.service.data.DataChangeType;

@Service
public class TextureModelLinkService {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	DataAccess data;
	
	Multimap<AssetName, ModelTask> textureNamesToModels;
	
	final DataChangeCallback<TextureTask> onTextureChangeCallback;
	final DataChangeCallback<ModelTask> onModelChangeCallback;
	
	@Autowired
	public TextureModelLinkService(DataAccess data) {
		this.data = data;
		textureNamesToModels = Multimaps.newSetMultimap(
				new ConcurrentHashMap<>(), () -> ConcurrentHashMap.newKeySet());
		onTextureChangeCallback = this::onTextureChange;
		onModelChangeCallback = this::onModelChange;
		
		data.registerForUpdates(onTextureChangeCallback, TextureTask.class);
		data.registerPostProcessor(onModelChangeCallback, ModelTask.class);
	}
	
	private void onModelChange(DataChangeEvent<ModelTask> event) {
		logger.debug("Updating texture-model linking due to potential model change.");
		
		for (final ModelTask modelTask : event.changed) {
			if (event.type == DataChangeType.REMOVED) {
				getAllExistingModelProps(modelTask, props -> {
					for (final AssetName textureName : props.getTextureNames()) {
						textureNamesToModels.remove(textureName, modelTask);
					}
				});
			} else if (event.type == DataChangeType.EDITED) {
				// TODO properties could have changed everything
			} else if (event.type == DataChangeType.ADDED) {
				getAllExistingModelProps(modelTask, props -> {
					for (final AssetName textureName : props.getTextureNames()) {
						textureNamesToModels.put(textureName, modelTask);
					}
				});
			}
		}
	}

	private void onTextureChange(DataChangeEvent<TextureTask> event) {
		logger.debug("Updating texture-model linking due to potential texture change.");
		
		for (final TextureTask textureTask : event.changed) {
			if (textureNamesToModels.containsKey(textureTask.getAssetName())) {
				onTextureChange(textureTask, event.type);
			}
		}
	}
	
	private void onTextureChange(TextureTask textureTask, DataChangeType eventType) {
		final AssetName textureName = textureTask.getAssetName();
		final Collection<ModelTask> changedModelTasks = textureNamesToModels.get(textureName);
		
		for (final ModelTask modelTask : changedModelTasks) {
			boolean changed = false;
			if (eventType == DataChangeType.REMOVED) {
				changed = getAllExistingModelProps(modelTask, props -> {
					return props.getTextureTasks().remove(textureTask);
				});
			} else if (eventType == DataChangeType.EDITED) {
				// does not handle texture name changes as edit event (must be remove + add)
			} else if (eventType == DataChangeType.ADDED) {
				changed = getAllExistingModelProps(modelTask, props -> {
					return props.getTextureNames().contains(textureName) && 
							props.getTextureTasks().add(textureTask);
				});
			}
			if (changed) {
				data.editBy(modelTask, User.SYSTEM);
			}
		}
	}
	
	/**
	 * @param modelTask - The task whose AssetProperties to get.
	 * @param action
	 * @return
	 */
	private <T extends AssetProperties> boolean getAllExistingModelProps(AssetTask<T> assetTask, Function<T, Boolean> action) {
		boolean hasChanged = false;
		for (final AssetGroupType type : AssetGroupType.values()) {
			final T props = assetTask.getAssetProperties(type);
			if (props != null) {
				hasChanged |= action.apply(props);
			}
		}
		return hasChanged;
	}
	
	/**
	 * @param modelTask - The task whose AssetProperties to get.
	 * @param action
	 * @return
	 */
	private <T extends AssetProperties> void getAllExistingModelProps(AssetTask<T> assetTask, Consumer<T> action) {
		for (final AssetGroupType type : AssetGroupType.values()) {
			final T props = assetTask.getAssetProperties(type);
			if (props != null) {
				action.accept(props);
			}
		}
	}
}
