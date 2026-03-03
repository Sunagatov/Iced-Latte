-- Insert 1
INSERT INTO public.login_attempts (id, user_email, attempts, is_user_locked, expiration_datetime, last_modified)
VALUES ('1f7c3842-74e7-4e3b-8a7c-05221c3f5815', 'john@example.com', 3, false, NULL, '2023-10-06 15:30:00');

-- Insert 2
INSERT INTO public.login_attempts (id, user_email, attempts, is_user_locked, expiration_datetime, last_modified)
VALUES ('2d5e9e19-13ac-4f0d-a09d-c0b1c79541a8', 'jane@example.com', 1, false, NULL, '2023-10-06 14:45:00');

-- Insert 3
INSERT INTO public.login_attempts (id, user_email, attempts, is_user_locked, expiration_datetime, last_modified)
VALUES ('78f5d57d-0c50-4aa2-a5e9-1e4a594c77f3', 'michael@example.com', 2, false, NULL, '2023-10-06 16:15:00');
