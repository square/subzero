package com.squareup.subzero.observers;

import com.squareup.subzero.proto.service.Internal;

/**
 * An {@link InternalCommandRequestObserver} which does nothing. This is the default observer in
 * {@link com.squareup.subzero.InternalCommandConnector}.
 */
public class NoopObserver implements InternalCommandRequestObserver {
    @Override
    public void observe(Internal.InternalCommandRequest internalRequest) {
        // Do nothing
    }
}
