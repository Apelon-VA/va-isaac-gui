package gov.va.isaac.util;

import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.coordinate.PremiseType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ViewCoordinateComponents {
	private final PremiseType relAssertionType;
	private final UUID path;
	private final Set<State> statuses = new HashSet<>();
	private final long time;
	private final Set<UUID> modules = new HashSet<>();
	
	public ViewCoordinateComponents(
			PremiseType relAssertionType,
			UUID path,
			Set<State> statuses, 
			long time, 
			Set<UUID> modules) {
		super();
		this.relAssertionType = relAssertionType;
		this.path = path;
		this.statuses.addAll(statuses);
		this.time = time;
		this.modules.addAll(modules);
	}

	public PremiseType getStatedInferredOption() {
		return relAssertionType;
	}

	public UUID getPath() {
		return path;
	}

	public Set<State> getStatuses() {
		return Collections.unmodifiableSet(statuses);
	}

	public long getTime() {
		return time;
	}

	public Set<UUID> getModules() {
		return Collections.unmodifiableSet(modules);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((modules == null) ? 0 : modules.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime
				* result
				+ ((relAssertionType == null) ? 0 : relAssertionType.hashCode());
		result = prime * result
				+ ((statuses == null) ? 0 : statuses.hashCode());
		result = prime * result + (int) (time ^ (time >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ViewCoordinateComponents other = (ViewCoordinateComponents) obj;
		if (modules == null) {
			if (other.modules != null)
				return false;
		} else if (!modules.equals(other.modules))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (relAssertionType != other.relAssertionType)
			return false;
		if (statuses == null) {
			if (other.statuses != null)
				return false;
		} else if (!statuses.equals(other.statuses))
			return false;
		if (time != other.time)
			return false;
		return true;
	}
}
