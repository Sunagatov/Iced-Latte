-- Latte (1e5b295f) — existing: 11111111(4), 44444444(5)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000001-0000-0000-0000-000000000001', '1e5b295f-8f50-4425-90e9-8b590a27b3a9', '22222222-2222-2222-2222-222222222222', 'Smooth and creamy, exactly what a latte should be. Will order again.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000001-0000-0000-0000-000000000002', '1e5b295f-8f50-4425-90e9-8b590a27b3a9', '33333333-3333-3333-3333-333333333333', 'Decent latte but nothing special. Milk could be steamier.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000001-0000-0000-0000-000000000003', '1e5b295f-8f50-4425-90e9-8b590a27b3a9', '55555555-5555-5555-5555-555555555555', 'Watery and bland. Expected much more from this.', 1, 0, 0);

-- Cappuccino (a3c4d3f7) — existing: none
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000002-0000-0000-0000-000000000001', 'a3c4d3f7-1172-4fb2-90a9-59b13b35dfc6', '11111111-1111-1111-1111-111111111111', 'Perfect foam ratio. This is how a cappuccino should taste.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000002-0000-0000-0000-000000000002', 'a3c4d3f7-1172-4fb2-90a9-59b13b35dfc6', '33333333-3333-3333-3333-333333333333', 'Good but the foam deflated quickly. Taste was fine.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000002-0000-0000-0000-000000000003', 'a3c4d3f7-1172-4fb2-90a9-59b13b35dfc6', '55555555-5555-5555-5555-555555555555', 'Too bitter for my taste. Not what I expected at all.', 2, 0, 0);

-- Mocha (418499f3) — existing: 55555555(4)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000003-0000-0000-0000-000000000001', '418499f3-d951-40bf-9414-5cb90ab21ecb', '11111111-1111-1111-1111-111111111111', 'Rich chocolate flavour with a strong espresso kick. Absolutely love it.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000003-0000-0000-0000-000000000002', '418499f3-d951-40bf-9414-5cb90ab21ecb', '22222222-2222-2222-2222-222222222222', 'Chocolate taste is a bit artificial. Still enjoyable though.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000003-0000-0000-0000-000000000003', '418499f3-d951-40bf-9414-5cb90ab21ecb', '44444444-4444-4444-4444-444444444444', 'Way too sweet. Couldn''t finish it.', 1, 0, 0);

-- Espresso (ad0ef2b7) — existing: 33333333(3)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000004-0000-0000-0000-000000000001', 'ad0ef2b7-816b-4a11-b361-dfcbe705fc96', '11111111-1111-1111-1111-111111111111', 'Short, sharp, and intense. Best espresso I''ve had outside Italy.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000004-0000-0000-0000-000000000002', 'ad0ef2b7-816b-4a11-b361-dfcbe705fc96', '44444444-4444-4444-4444-444444444444', 'Solid espresso. Nothing groundbreaking but reliable.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000004-0000-0000-0000-000000000003', 'ad0ef2b7-816b-4a11-b361-dfcbe705fc96', '55555555-5555-5555-5555-555555555555', 'Over-extracted and burnt tasting. Disappointing.', 2, 0, 0);

-- Macchiato (46f97165) — existing: 11111111(1)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000005-0000-0000-0000-000000000001', '46f97165-00a7-4b45-9e5c-09f8168b0047', '22222222-2222-2222-2222-222222222222', 'Love the balance of espresso and milk. Simple and satisfying.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000005-0000-0000-0000-000000000002', '46f97165-00a7-4b45-9e5c-09f8168b0047', '44444444-4444-4444-4444-444444444444', 'It''s fine. Not as bold as I like my coffee.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000005-0000-0000-0000-000000000003', '46f97165-00a7-4b45-9e5c-09f8168b0047', '55555555-5555-5555-5555-555555555555', 'Milk overpowers the espresso completely. Missed the mark.', 2, 0, 0);

-- Americano (e6a4d7f2) — existing: 11111111(5), 22222222(3), 33333333(1), 77777777(3)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000006-0000-0000-0000-000000000001', 'e6a4d7f2-d40e-4e5f-93b8-5d56ce6724c5', '44444444-4444-4444-4444-444444444444', 'Clean and smooth. Great for those who want coffee without the milk.', 4, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000006-0000-0000-0000-000000000002', 'e6a4d7f2-d40e-4e5f-93b8-5d56ce6724c5', '55555555-5555-5555-5555-555555555555', 'Just hot water and espresso — nothing special but does the job.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000006-0000-0000-0000-000000000003', 'e6a4d7f2-d40e-4e5f-93b8-5d56ce6724c5', '66666666-6666-6666-6666-666666666666', 'Tasted stale. The espresso base was clearly old.', 1, 0, 0);

-- Flat White (fa1e12ff) — existing: 22222222(5)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000007-0000-0000-0000-000000000001', 'fa1e12ff-67e4-42d5-bf45-c43576890f8a', '11111111-1111-1111-1111-111111111111', 'Velvety microfoam and strong espresso. My daily driver.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000007-0000-0000-0000-000000000002', 'fa1e12ff-67e4-42d5-bf45-c43576890f8a', '44444444-4444-4444-4444-444444444444', 'Good but I''ve had better flat whites. Ratio felt slightly off.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000007-0000-0000-0000-000000000003', 'fa1e12ff-67e4-42d5-bf45-c43576890f8a', '55555555-5555-5555-5555-555555555555', 'Too milky, lost the espresso character entirely.', 2, 0, 0);

-- Iced Coffee (6d77f8a9) — existing: none
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000008-0000-0000-0000-000000000001', '6d77f8a9-e640-4d2e-ba2c-b7db8ab2c123', '11111111-1111-1111-1111-111111111111', 'Refreshing and bold. Perfect for hot days.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000008-0000-0000-0000-000000000002', '6d77f8a9-e640-4d2e-ba2c-b7db8ab2c123', '33333333-3333-3333-3333-333333333333', 'Ice melts too fast and dilutes the flavour. Decent otherwise.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000008-0000-0000-0000-000000000003', '6d77f8a9-e640-4d2e-ba2c-b7db8ab2c123', '44444444-4444-4444-4444-444444444444', 'Weak coffee flavour. Tastes more like cold milk than iced coffee.', 2, 0, 0);

-- Affogato (ba5f15c4) — existing: none
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000009-0000-0000-0000-000000000001', 'ba5f15c4-1f72-4b97-b9cf-4437e5c6c2fa', '11111111-1111-1111-1111-111111111111', 'Hot espresso over cold ice cream is pure genius. Dessert and coffee in one.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000009-0000-0000-0000-000000000002', 'ba5f15c4-1f72-4b97-b9cf-4437e5c6c2fa', '22222222-2222-2222-2222-222222222222', 'Nice concept but the ice cream was too sweet for the espresso.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000009-0000-0000-0000-000000000003', 'ba5f15c4-1f72-4b97-b9cf-4437e5c6c2fa', '55555555-5555-5555-5555-555555555555', 'Ice cream melted before I could enjoy it. Presentation was poor.', 2, 0, 0);

-- Cortado (3e9c1f94) — existing: 55555555(4)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000010-0000-0000-0000-000000000001', '3e9c1f94-ee3c-4e0b-9a6e-bb6e9c61c6f5', '11111111-1111-1111-1111-111111111111', 'Equal parts espresso and milk — perfectly balanced. A hidden gem.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000010-0000-0000-0000-000000000002', '3e9c1f94-ee3c-4e0b-9a6e-bb6e9c61c6f5', '22222222-2222-2222-2222-222222222222', 'Smaller than expected but the flavour is there.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000010-0000-0000-0000-000000000003', '3e9c1f94-ee3c-4e0b-9a6e-bb6e9c61c6f5', '44444444-4444-4444-4444-444444444444', 'Not worth the price for such a tiny drink.', 2, 0, 0);

-- Cold Brew (3ea8e601) — existing: 33333333(2)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000011-0000-0000-0000-000000000001', '3ea8e601-24c9-49b1-8c65-8db8b3a5c7a3', '11111111-1111-1111-1111-111111111111', 'Incredibly smooth with zero bitterness. Cold brew done right.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000011-0000-0000-0000-000000000002', '3ea8e601-24c9-49b1-8c65-8db8b3a5c7a3', '44444444-4444-4444-4444-444444444444', 'Good cold brew but a bit weak for my taste. Needs more concentrate.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000011-0000-0000-0000-000000000003', '3ea8e601-24c9-49b1-8c65-8db8b3a5c7a3', '55555555-5555-5555-5555-555555555555', 'Tastes like it was sitting too long. Flat and stale.', 1, 0, 0);

-- Nitro Coffee (eec8a1d8) — existing: 11111111(1)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000012-0000-0000-0000-000000000001', 'eec8a1d8-4864-4c1b-aa8b-dedfddc6e356', '22222222-2222-2222-2222-222222222222', 'The nitrogen bubbles make it feel like a stout beer. Silky and delicious.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000012-0000-0000-0000-000000000002', 'eec8a1d8-4864-4c1b-aa8b-dedfddc6e356', '44444444-4444-4444-4444-444444444444', 'Interesting texture but the novelty wears off quickly.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000012-0000-0000-0000-000000000003', 'eec8a1d8-4864-4c1b-aa8b-dedfddc6e356', '55555555-5555-5555-5555-555555555555', 'Overpriced gimmick. Just drink regular cold brew.', 2, 0, 0);

-- Frappuccino (7edf3a1e) — existing: none
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000013-0000-0000-0000-000000000001', '7edf3a1e-c391-4d76-9002-2f0dd3e9c6e9', '11111111-1111-1111-1111-111111111111', 'Thick, creamy, and indulgent. A treat rather than a coffee.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000013-0000-0000-0000-000000000002', '7edf3a1e-c391-4d76-9002-2f0dd3e9c6e9', '33333333-3333-3333-3333-333333333333', 'Very sweet. More of a dessert than a coffee drink.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000013-0000-0000-0000-000000000003', '7edf3a1e-c391-4d76-9002-2f0dd3e9c6e9', '55555555-5555-5555-5555-555555555555', 'Sugar overload. I felt sick after half of it.', 1, 0, 0);

-- Turkish Coffee (e0323d1b) — existing: 11111111(4)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000014-0000-0000-0000-000000000001', 'e0323d1b-1169-4a0e-8d7b-07ff3bfe7f7e', '22222222-2222-2222-2222-222222222222', 'Authentic and intense. The cardamom aroma is wonderful.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000014-0000-0000-0000-000000000002', 'e0323d1b-1169-4a0e-8d7b-07ff3bfe7f7e', '44444444-4444-4444-4444-444444444444', 'Very strong. Not for the faint-hearted but I respect it.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000014-0000-0000-0000-000000000003', 'e0323d1b-1169-4a0e-8d7b-07ff3bfe7f7e', '55555555-5555-5555-5555-555555555555', 'Too much sediment at the bottom. Unpleasant to drink.', 2, 0, 0);

-- Red Eye (e70f1d94) — existing: 44444444(5)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000015-0000-0000-0000-000000000001', 'e70f1d94-d55f-4e0e-8a6b-28e2ca3c6c34', '11111111-1111-1111-1111-111111111111', 'Double the caffeine, double the satisfaction. Gets me through any morning.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000015-0000-0000-0000-000000000002', 'e70f1d94-d55f-4e0e-8a6b-28e2ca3c6c34', '22222222-2222-2222-2222-222222222222', 'Strong but the drip coffee base was a bit flat.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000015-0000-0000-0000-000000000003', 'e70f1d94-d55f-4e0e-8a6b-28e2ca3c6c34', '33333333-3333-3333-3333-333333333333', 'Way too harsh. Gave me heart palpitations. Not ordering again.', 1, 0, 0);

-- Chai Latte (b5faee5d) — existing: 11111111(4)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000016-0000-0000-0000-000000000001', 'b5faee5d-6e6d-4319-ba9f-8d1bf7ee3f63', '22222222-2222-2222-2222-222222222222', 'Warming spices and creamy milk. Perfect autumn drink.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000016-0000-0000-0000-000000000002', 'b5faee5d-6e6d-4319-ba9f-8d1bf7ee3f63', '44444444-4444-4444-4444-444444444444', 'Spice blend is pleasant but the sweetness is a bit much.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000016-0000-0000-0000-000000000003', 'b5faee5d-6e6d-4319-ba9f-8d1bf7ee3f63', '55555555-5555-5555-5555-555555555555', 'Tastes like a syrup packet, not real chai. Very artificial.', 2, 0, 0);

-- Green Tea Latte (25a8e8c1) — existing: 44444444(4)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000017-0000-0000-0000-000000000001', '25a8e8c1-37ba-4a8b-927f-5f1b4b5b5c3c', '11111111-1111-1111-1111-111111111111', 'Earthy matcha with creamy milk. Calming and delicious.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000017-0000-0000-0000-000000000002', '25a8e8c1-37ba-4a8b-927f-5f1b4b5b5c3c', '33333333-3333-3333-3333-333333333333', 'Matcha flavour is mild. Could use a stronger tea base.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000017-0000-0000-0000-000000000003', '25a8e8c1-37ba-4a8b-927f-5f1b4b5b5c3c', '55555555-5555-5555-5555-555555555555', 'Grassy and bitter. Not enjoyable at all.', 1, 0, 0);

-- Hot Chocolate (4e9a7d28) — existing: 44444444(1)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000018-0000-0000-0000-000000000001', '4e9a7d28-5e40-4b14-bc72-a5d1b547c3d0', '11111111-1111-1111-1111-111111111111', 'Rich, thick, and deeply chocolatey. The whipped cream on top is perfect.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000018-0000-0000-0000-000000000002', '4e9a7d28-5e40-4b14-bc72-a5d1b547c3d0', '22222222-2222-2222-2222-222222222222', 'Warm and comforting. A bit too sweet but kids would love it.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000018-0000-0000-0000-000000000003', '4e9a7d28-5e40-4b14-bc72-a5d1b547c3d0', '33333333-3333-3333-3333-333333333333', 'Tasted like powder mix, not real chocolate. Very disappointing.', 2, 0, 0);

-- Iced Latte (cc0b6e5d) — existing: 33333333(2)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000019-0000-0000-0000-000000000001', 'cc0b6e5d-71f1-44cd-8a5b-1bdb978b5ca6', '11111111-1111-1111-1111-111111111111', 'Cold, smooth, and perfectly balanced. My summer staple.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000019-0000-0000-0000-000000000002', 'cc0b6e5d-71f1-44cd-8a5b-1bdb978b5ca6', '44444444-4444-4444-4444-444444444444', 'Fine but nothing that stands out from any other iced latte.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000019-0000-0000-0000-000000000003', 'cc0b6e5d-71f1-44cd-8a5b-1bdb978b5ca6', '55555555-5555-5555-5555-555555555555', 'Ice watered it down completely. No coffee flavour left.', 1, 0, 0);

-- Pumpkin Spice Latte (aa1d3e8f) — existing: 44444444(5), 55555555(5), 22222222(5), 33333333(4)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000020-0000-0000-0000-000000000001', 'aa1d3e8f-9866-4e07-bc61-b5d8c2a3df4b', '11111111-1111-1111-1111-111111111111', 'Seasonal and cosy. The pumpkin spice blend is spot on.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000020-0000-0000-0000-000000000002', 'aa1d3e8f-9866-4e07-bc61-b5d8c2a3df4b', '66666666-6666-6666-6666-666666666666', 'Smells better than it tastes. Spice is overpowering.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000020-0000-0000-0000-000000000003', 'aa1d3e8f-9866-4e07-bc61-b5d8c2a3df4b', '77777777-7777-7777-7777-777777777777', 'Overhyped. Just tastes like cinnamon syrup in coffee.', 2, 0, 0);

-- Iced Macchiato (eedc6cde) — existing: 55555555(5)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000021-0000-0000-0000-000000000001', 'eedc6cde-1e80-4ebf-a9d1-8e5e4eb2cacf', '11111111-1111-1111-1111-111111111111', 'Beautiful layers and great espresso flavour over ice.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000021-0000-0000-0000-000000000002', 'eedc6cde-1e80-4ebf-a9d1-8e5e4eb2cacf', '33333333-3333-3333-3333-333333333333', 'Looks great but the espresso sinks too fast. Stir it immediately.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000021-0000-0000-0000-000000000003', 'eedc6cde-1e80-4ebf-a9d1-8e5e4eb2cacf', '44444444-4444-4444-4444-444444444444', 'Milk was sour. Ruined the whole drink.', 1, 0, 0);

-- Vanilla Latte (fc88cd5d) — existing: 44444444(2)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000022-0000-0000-0000-000000000001', 'fc88cd5d-5049-4b00-8d88-df1d9b4a3ce1', '11111111-1111-1111-1111-111111111111', 'Subtle vanilla sweetness with a smooth espresso base. Lovely.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000022-0000-0000-0000-000000000002', 'fc88cd5d-5049-4b00-8d88-df1d9b4a3ce1', '22222222-2222-2222-2222-222222222222', 'Vanilla flavour is pleasant but fades quickly.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000022-0000-0000-0000-000000000003', 'fc88cd5d-5049-4b00-8d88-df1d9b4a3ce1', '55555555-5555-5555-5555-555555555555', 'Artificial vanilla taste. Like drinking a candle.', 1, 0, 0);

-- Caramel Macchiato (5efb7cfd) — existing: none
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000023-0000-0000-0000-000000000001', '5efb7cfd-744b-4af9-b713-ef8d30abf628', '11111111-1111-1111-1111-111111111111', 'The caramel drizzle on top is generous and the espresso cuts right through. Brilliant.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000023-0000-0000-0000-000000000002', '5efb7cfd-744b-4af9-b713-ef8d30abf628', '33333333-3333-3333-3333-333333333333', 'Sweet and enjoyable. A bit too caramel-heavy for everyday drinking.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000023-0000-0000-0000-000000000003', '5efb7cfd-744b-4af9-b713-ef8d30abf628', '44444444-4444-4444-4444-444444444444', 'Cloyingly sweet. The caramel completely masks the coffee.', 2, 0, 0);

-- Peppermint Mocha (b183776e) — existing: 22222222(1)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000024-0000-0000-0000-000000000001', 'b183776e-6c3b-459a-bb3a-e93a4c8e4e56', '11111111-1111-1111-1111-111111111111', 'Festive and delicious. Chocolate and peppermint is a classic combo.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000024-0000-0000-0000-000000000002', 'b183776e-6c3b-459a-bb3a-e93a4c8e4e56', '44444444-4444-4444-4444-444444444444', 'Peppermint is a bit strong but the chocolate balances it out.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000024-0000-0000-0000-000000000003', 'b183776e-6c3b-459a-bb3a-e93a4c8e4e56', '55555555-5555-5555-5555-555555555555', 'Tastes like toothpaste mixed with coffee. Not for me.', 1, 0, 0);

-- Hazelnut Latte (c3f45eec) — existing: 55555555(3)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000025-0000-0000-0000-000000000001', 'c3f45eec-18d8-43e0-9d7b-d85a4a9b6bda', '11111111-1111-1111-1111-111111111111', 'Nutty and smooth. The hazelnut syrup is not too sweet.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000025-0000-0000-0000-000000000002', 'c3f45eec-18d8-43e0-9d7b-d85a4a9b6bda', '22222222-2222-2222-2222-222222222222', 'Hazelnut flavour is subtle. Could be stronger.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000025-0000-0000-0000-000000000003', 'c3f45eec-18d8-43e0-9d7b-d85a4a9b6bda', '44444444-4444-4444-4444-444444444444', 'Fake hazelnut flavour. Nothing like the real thing.', 2, 0, 0);

-- Lemonade Iced Tea (123f7a2d) — existing: 55555555(3)
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000026-0000-0000-0000-000000000001', '123f7a2d-cb34-4e5f-9a1d-4e4b456a03a7', '11111111-1111-1111-1111-111111111111', 'Tangy lemonade with a refreshing tea base. Great non-coffee option.', 5, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000026-0000-0000-0000-000000000002', '123f7a2d-cb34-4e5f-9a1d-4e4b456a03a7', '22222222-2222-2222-2222-222222222222', 'Refreshing but the lemon is too sharp. Needs more sweetness.', 3, 0, 0);
INSERT INTO public.product_reviews (id, product_id, user_id, text, rating, likes_count, dislikes_count) VALUES ('b1000026-0000-0000-0000-000000000003', '123f7a2d-cb34-4e5f-9a1d-4e4b456a03a7', '44444444-4444-4444-4444-444444444444', 'Tastes like diluted lemonade. No real tea flavour at all.', 2, 0, 0);
