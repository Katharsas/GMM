package gmm.service.tasks;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import gmm.collections.ArrayList;
import gmm.collections.Collection;
import gmm.domain.User;
import gmm.domain.task.asset.AssetGroupType;
import gmm.domain.task.asset.AssetKey;
import gmm.domain.task.asset.AssetName;
import gmm.domain.task.asset.AssetProperties;
import gmm.domain.task.asset.AssetTask;
import gmm.domain.task.asset.ModelProperties;
import gmm.domain.task.asset.ModelTask;
import gmm.domain.task.asset.TextureTask;
import gmm.service.assets.AssetService;
import gmm.service.data.DataAccess;
import gmm.service.data.DataAccess.DataChangeCallback;
import gmm.service.data.DataChangeEvent;
import gmm.service.data.DataChangeType;

@Service
public class TextureModelLinkService {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final DataAccess data;
//	AssetService assets;
	
	private final Map<AssetKey, AssetTask<?>> assetTasks;
	private final Multimap<AssetName, ModelTask> textureNamesToModels;
	private final Multimap<ModelTask, AssetName> modelsToTextureNames;
	
	private final DataChangeCallback<TextureTask> onTextureChangeCallback;
	private final DataChangeCallback<ModelTask> onModelChangeCallback;
	
	@Autowired
	public TextureModelLinkService(DataAccess data, AssetService assets) {
		this.data = data;
		
		this.assetTasks = assets.getNewAssetFoldersTaskEvents().getLiveView();
		// only one thread writes to these at a time, but any number of threads may read at the same time
		// TODO check if unsynchronized MultiMap is thread-safe for this scenario
		// see https://github.com/google/guava/issues/135
		textureNamesToModels = Multimaps.synchronizedSetMultimap(HashMultimap.create());
		modelsToTextureNames = Multimaps.synchronizedSetMultimap(HashMultimap.create());
		
		onTextureChangeCallback = this::onTextureChange;
		onModelChangeCallback = this::onModelChange;
		
		// these handlers MUST be registered after AssetService registers its handler, and handlers must be called in order
		data.registerPostProcessor(onTextureChangeCallback, TextureTask.class);
		data.registerPostProcessor(onModelChangeCallback, ModelTask.class);
	}
	
	private void onModelChange(DataChangeEvent<? extends ModelTask> event) {
		logger.debug("Updating texture-model linking due to potential model change.");
		
		for (final ModelTask modelTask : event.changed) {
			if (event.type == DataChangeType.REMOVED) {
				modelsToTextureNames.removeAll(modelTask);
				forEachGroupType(modelTask, props -> {
					for (final AssetName textureName : props.getTextureNames()) {
						textureNamesToModels.remove(textureName, modelTask);
					}
				});
			} else if (event.type == DataChangeType.EDITED) {
				synchronized(modelsToTextureNames) {
					for (final AssetName textureName : modelsToTextureNames.get(modelTask)) {
						textureNamesToModels.remove(textureName, modelTask);
					}
				}
				modelsToTextureNames.removeAll(modelTask);
				forEachGroupType(modelTask, props -> {
					modelsToTextureNames.putAll(modelTask, props.getTextureNames());
					for (final AssetName textureName : props.getTextureNames()) {
						textureNamesToModels.put(textureName, modelTask);
						updateModelPropsViewModel(props, textureName);
					}
				});
			} else if (event.type == DataChangeType.ADDED) {
				forEachGroupType(modelTask, props -> {
					modelsToTextureNames.putAll(modelTask, props.getTextureNames());
					for (final AssetName textureName : props.getTextureNames()) {
						textureNamesToModels.put(textureName, modelTask);
						updateModelPropsViewModel(props, textureName);
					}
				});
			}
		}
	}
	
	private void updateModelPropsViewModel(ModelProperties modelProps, AssetName textureName) {
		AssetTask<?> assetTask = assetTasks.get(textureName.getKey());
		// if asset/props changed, then viewModel is in default state (all textures are texturesWithoutTasks)
		// if it didn't, then re-adding textures wont actually do anything
		if (assetTask != null) {
			if (!(assetTask instanceof TextureTask)) {
				logger.warn("Expected to find 'TextureTask' for asset '" + textureName + "',"
						+ " but was '" + assetTask.getClass().getSimpleName() +"'!");
				return;
			}
			TextureTask textureTask = (TextureTask) assetTask;
			modelProps.getViewModel().addTextureTask(textureTask);
		}
	}

	private void onTextureChange(DataChangeEvent<? extends TextureTask> event) {
		logger.debug("Updating texture-model linking due to potential texture change.");
		final Collection<ModelTask> changedModelTasks = new ArrayList<>(ModelTask.class);
		for (final TextureTask textureTask : event.changed) {
			if (textureNamesToModels.containsKey(textureTask.getAssetName())) {
				changedModelTasks.addAll(onTextureChange(textureTask, event.type));
			}
		}
		data.editAllBy(changedModelTasks, User.SYSTEM);
	}
	
	private Collection<ModelTask> onTextureChange(TextureTask textureTask, DataChangeType eventType) {
		final AssetName textureName = textureTask.getAssetName();
		final java.util.Collection<ModelTask> dependentModelTasks = textureNamesToModels.get(textureName);
		final Collection<ModelTask> changedModelTasks = new ArrayList<>(ModelTask.class);
		
		for (final ModelTask modelTask : dependentModelTasks) {
			boolean changed = false;
			if (eventType == DataChangeType.REMOVED) {
				changed = forEachGroupType(modelTask, props -> {
//					System.out.println("TEX_REM: for model '" + modelTask + "' texture '" + textureName + "'");
					return props.getViewModel().removeTextureTask(textureTask);
				});
			} else if (eventType == DataChangeType.EDITED) {
				// TODO: does not handle texture name changes as edit event (must be remove + add)
				changed = forEachGroupType(modelTask, props -> {
//					System.out.println("TEX_REM_ADD: for model '" + modelTask + "' texture '" + textureName + "'");
					return props.getViewModel().removeTextureTask(textureTask)
							| props.getViewModel().addTextureTask(textureTask);
				});
			} else if (eventType == DataChangeType.ADDED) {
				changed = forEachGroupType(modelTask, props -> {
					if (props.getTextureNames().contains(textureName)) {
//						System.out.println("TEX_ADD: for model '" + modelTask + "' texture '" + textureName + "'");
						return props.getViewModel().addTextureTask(textureTask);
					}
					return false;
				});
			}
			if (changed) {
				changedModelTasks.add(modelTask);
			}
		}
		return changedModelTasks;
	}
	
	/**
	 * @param assetTask - The task whose AssetProperties to get.
	 * @param action
	 * @return
	 */
	private <T extends AssetProperties> boolean forEachGroupType(AssetTask<T> assetTask, Function<T, Boolean> action) {
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
	 * @param assetTask - The task whose AssetProperties to get.
	 * @param action
	 * @return
	 */
	private <T extends AssetProperties> void forEachGroupType(AssetTask<T> assetTask, Consumer<T> action) {
		for (final AssetGroupType type : AssetGroupType.values()) {
			final T props = assetTask.getAssetProperties(type);
			if (props != null) {
				action.accept(props);
			}
		}
	}
}
