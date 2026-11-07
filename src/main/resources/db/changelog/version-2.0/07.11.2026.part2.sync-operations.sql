CREATE TABLE public.sync_resource_state (
    user_id UUID NOT NULL REFERENCES public.user_details(id) ON DELETE CASCADE,
    resource VARCHAR(32) NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, resource)
);

CREATE TABLE public.sync_operation (
    user_id UUID NOT NULL REFERENCES public.user_details(id) ON DELETE CASCADE,
    resource VARCHAR(32) NOT NULL,
    operation_id UUID NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    outcome VARCHAR(16) NOT NULL CHECK (outcome IN ('APPLIED', 'REJECTED')),
    code VARCHAR(64) NOT NULL,
    message TEXT NOT NULL,
    conflicts TEXT NOT NULL,
    applied_revision BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, resource, operation_id)
);
