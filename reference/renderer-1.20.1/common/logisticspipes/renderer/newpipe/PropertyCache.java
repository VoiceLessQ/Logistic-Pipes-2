package logisticspipes.renderer.newpipe;



import com.google.common.cache.Cache;

import logisticspipes.renderer.state.PipeRenderState;

// TODO: IUnlistedProperty removed in 1.20.1 — deferred with rest of rendering migration
public class PropertyCache {


	public String getName() {
		return "lpcache";
	}


	public boolean isValid(Cache<PipeRenderState.LocalCacheType, Object> value) {
		return value != null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })

	public Class<Cache<PipeRenderState.LocalCacheType, Object>> getType() {
		return (Class) Cache.class;
	}


	public String valueToString(Cache<PipeRenderState.LocalCacheType, Object> value) {
		return value.toString();
	}

}
