package com.squareup.subzero.observers;

import com.squareup.subzero.proto.service.Internal.InternalCommandRequest;

/**
 * Interface for an observer that takes some action on an
 * {@link com.squareup.subzero.proto.service.Internal.InternalCommandRequest} before it's sent to Subzero Core
 * by {@link com.squareup.subzero.InternalCommandConnector}. The actions are allowed to have side effects, such as
 * writing to a file.
 */
public interface InternalCommandRequestObserver {
    void observe(InternalCommandRequest internalRequest);
}
