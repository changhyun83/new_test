package com.lge.dlnaserver;

import com.lge.dlnaserver.IDlnaEnablerListener;

/** @hide */
interface IDlnaEnabler
{
	void registerListener(IDlnaEnablerListener listener);
	void unregisterListener(IDlnaEnablerListener listener);
	
	void startShare();
	void stopShare();
	
	int	getState();
}
