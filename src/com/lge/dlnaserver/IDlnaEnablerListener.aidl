package com.lge.dlnaserver;

interface IDlnaEnablerListener {
    void startShareComplete(int err);
    void stopShareComplete(int err);
    
    void statusUpdate();
}