package com.cfryan.beyondchat.service;

public interface IConnectionStatusCallback {
	/**
	 * @param connectedState
	 * @param reason
	 */
	public void connectionStatusChanged(int connectedState, String reason);
}
