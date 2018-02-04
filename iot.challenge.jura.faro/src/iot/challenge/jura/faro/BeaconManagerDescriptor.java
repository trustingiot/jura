package iot.challenge.jura.faro;

/**
 * Manager configuration descriptor
 */
public class BeaconManagerDescriptor {

	private final Protocol protocol;

	private final WorkingMode workingMode;

	public BeaconManagerDescriptor(Protocol protocol, WorkingMode workingMode) {
		this.protocol = protocol;
		this.workingMode = workingMode;
	}

	public static BeaconManagerDescriptor build(Protocol protocol, WorkingMode workingMode) {
		return new BeaconManagerDescriptor(protocol, workingMode);
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public WorkingMode getWorkingMode() {
		return workingMode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
		result = prime * result + ((workingMode == null) ? 0 : workingMode.hashCode());
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
		BeaconManagerDescriptor other = (BeaconManagerDescriptor) obj;
		if (protocol != other.protocol)
			return false;
		if (workingMode != other.workingMode)
			return false;
		return true;
	}
}
