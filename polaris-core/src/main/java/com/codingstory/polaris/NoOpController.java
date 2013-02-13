package com.codingstory.polaris;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

public final class NoOpController implements RpcController {

    private static final NoOpController INSTANCE = new NoOpController();

    private NoOpController() {}

    public static NoOpController getInstance() {
        return INSTANCE;
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public String errorText() {
        return null;
    }

    @Override
    public void startCancel() {
    }

    @Override
    public void setFailed(String s) {
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void notifyOnCancel(RpcCallback<Object> objectRpcCallback) {
    }
}
