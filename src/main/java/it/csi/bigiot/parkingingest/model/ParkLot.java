package it.csi.bigiot.parkingingest.model;

public class ParkLot {
	public String status = "unknown";
	public Long timestamp = 0L;
	
	public ParkLot(String status, Long timestamp) {
		super();
		this.status = status;
		this.timestamp = timestamp;
	}
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}
	
}
