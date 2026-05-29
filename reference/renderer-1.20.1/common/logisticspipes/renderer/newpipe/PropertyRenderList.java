package logisticspipes.renderer.newpipe;

import java.util.List;



// TODO: IUnlistedProperty removed in 1.20.1 — deferred with rest of rendering migration
public class PropertyRenderList {


	public String getName() {
		return "lprenderentrylist";
	}


	public boolean isValid(List<RenderEntry> value) {
		return value != null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })

	public Class<List<RenderEntry>> getType() {
		return (Class) List.class;
	}


	public String valueToString(List<RenderEntry> value) {
		return value.toString();
	}

}
