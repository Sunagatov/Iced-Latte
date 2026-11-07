CREATE TABLE public.cart_merge_receipt (
    user_id UUID NOT NULL REFERENCES public.user_details(id) ON DELETE CASCADE,
    merge_key UUID NOT NULL,
    request_items TEXT NOT NULL,
    PRIMARY KEY (user_id, merge_key)
);
