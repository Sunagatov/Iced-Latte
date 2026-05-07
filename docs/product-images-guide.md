# Product Images Guide

How to generate, name, and deploy premium product images for the Iced Latte marketplace.

---

## Quick Reference

| Item | Value |
|------|-------|
| Aspect ratio | **4:5** (matches frontend `aspect-[4/5]` product cards) |
| Format | PNG |
| File name | `card_logo.png` |
| S3 key pattern | `<ProductName>_<UUID>/card_logo.png` |
| Bucket | `iced-latte-products` |
| Local seed path | `seed/products/<ProductName>_<UUID>/card_logo.png` |
| Generator | ChatGPT Images (GPT-4o image gen, April 2026+) |
| Typical file size | ~2 MB per image (36 images = ~72 MB total) |
| Prod server | `root@116.203.197.65` |
| Container | `iced-latte-backend` |
| Website | https://www.iced-latte.uk/ |

---

## Workflow

1. **Write a prompt** per product (see style guide below).
2. **Paste into ChatGPT** → download the PNG.
3. **Drop into** `seed/products/<ProductName>_<UUID>/card_logo.png`.
4. **Upload to S3** (see [supabase-s3-operations.md](supabase-s3-operations.md)).
5. **Restart backend** — the startup migration re-indexes the bucket.
6. **Verify** — hit the product API or check the website.

---

## Prompt Style Guide

### Core Rules

- **Packaged products, not beverages in cups** — bags, tins, bottles, cans, cartons.
  - Exception: Starbucks uses white paper cups (hot), clear plastic cups (cold), and black cans (Nitro).
- **Brand name as logo** on the package.
- **Product name as text** on the label.
- **Photorealistic studio photography** — soft directional lighting, shallow depth of field, clean background.
- **Aspect ratio 4:5** — specify explicitly in the prompt.
- **One product per image** — centered, hero shot.

### Prompt Template

```
Photorealistic studio product photograph, 4:5 aspect ratio.

A [PACKAGE TYPE] of "[PRODUCT NAME]" by [BRAND].
[PACKAGE DESCRIPTION — color, material, closure, label details].
The brand logo "[BRAND]" is [logo placement]. The product name "[PRODUCT NAME]" is [label placement].

Background: [BACKGROUND DESCRIPTION].
Lighting: soft directional studio light from the left, gentle shadow on the right.
Style: commercial product photography, shallow depth of field, clean and premium.
```

### Example Prompt

```
Photorealistic studio product photograph, 4:5 aspect ratio.

A brushed silver aluminum canister of "Classico Espresso" by Illy.
The canister has a red screw-top lid, a clean silver body with the Illy logo in red near the top,
and "Classico Espresso" in elegant serif type on the center label.

Background: light grey marble surface, minimal, soft gradient to white.
Lighting: soft directional studio light from the left, gentle shadow on the right.
Style: commercial product photography, shallow depth of field, clean and premium.
```

---

## Brand Visual Identities

Each brand has a consistent package style and background. Products within a brand differ by label text and minor color accents.

| Brand | Package Type | Colors/Materials | Background |
|-------|-------------|-----------------|------------|
| **Folgers** | Metal tin, gold lid | Red/burgundy body, gold accents | Warm kitchen, wood countertop |
| **Illy** | Aluminum canister, red lid | Brushed silver body, red logo | Light grey marble, minimal |
| **Dunkin' Donuts** | Paper bag | Bold orange-pink, playful type | Bright white, vibrant |
| **Nescafé** | Glass jar, black lid | Dark amber glass, red/gold label | Dark slate, moody lighting |
| **Lavazza** | Foil bag, gold accents | Navy blue body, gold trim | Dark walnut, Italian kitchen |
| **Peet's Coffee** | Kraft paper bag, tin-tie | Natural brown, black logo | Reclaimed wood, exposed brick |
| **Starbucks** | White cup (hot) / clear cup (cold) / black can (nitro) | Green siren logo, white/black | White marble, modern café |
| **Oatly** | Tetra pak carton | Oat beige, hand-drawn type | Light birch, Scandinavian minimal |

---

## Lessons Learned

1. **Delete old images before uploading new ones** — having two files per product folder (e.g., `Latte.jpeg` + `card_logo.png`) causes a `Duplicate key` crash in the backend's metadata indexer.

2. **Consistent file naming matters** — the backend doesn't care about the filename itself, but it must be exactly one file per product folder. Use `card_logo.png` everywhere.

3. **ChatGPT image gen tips:**
   - Be explicit about aspect ratio — say "4:5 aspect ratio" in the prompt.
   - Specify "photorealistic studio product photograph" to avoid illustrations.
   - Name the brand and product in quotes to get accurate text rendering.
   - Describe the package material and color precisely for consistency across a brand's lineup.

4. **Batch workflow** — generate one prompt per product, paste sequentially. ChatGPT maintains quality across a session. Download each PNG immediately.

5. **Verify with direct S3 URL** before restarting the backend:
   ```
   https://fzvwwpzdudxrdzwbucaw.supabase.co/storage/v1/object/public/iced-latte-products/<KEY>
   ```

6. **Old file formats we replaced:** The bucket previously contained `.jpeg`, `.webp`, and `.png` files with product-name filenames (e.g., `Vanilla Latte.jpeg`, `card_logo.webp`). All were replaced with `card_logo.png`.

7. **Speed tip:** Give the prompt first, handle file operations after. Don't wait for the upload to finish before writing the next prompt.

8. **Underscore in product names is safe** — the backend splits the folder name by `_` and takes `packageName[1]` as the UUID. Since UUIDs have a fixed format (`8-4-4-4-12` hex), the parser uses `UUID.fromString()` which validates correctly even if the product name contains underscores. However, the current code does `parts[0].split("_")` and takes index `[1]` — so a product name with underscores (e.g., `Cold_Brew`) would break parsing. **Avoid underscores in product folder names.** Use spaces instead.

---

## Full Product Catalog (36 products)

### Original Products (v1.0 — 26 products)

| # | Product | Brand | UUID | S3 Folder |
|---|---------|-------|------|-----------|
| 1 | Latte | Folgers | `1e5b295f-8f50-4425-90e9-8b590a27b3a9` | `Latte_1e5b295f-...` |
| 2 | Cappuccino | Illy | `a3c4d3f7-1172-4fb2-90a9-59b13b35dfc6` | `Cappuccino_a3c4d3f7-...` |
| 3 | Mocha | Dunkin-Donuts | `418499f3-d951-40bf-9414-5cb90ab21ecb` | `Mocha_418499f3-...` |
| 4 | Espresso | Nescafé | `ad0ef2b7-816b-4a11-b361-dfcbe705fc96` | `Espresso_ad0ef2b7-...` |
| 5 | Macchiato | Lavazza | `46f97165-00a7-4b45-9e5c-09f8168b0047` | `Macchiato_46f97165-...` |
| 6 | Americano | Peet's Coffee | `e6a4d7f2-d40e-4e5f-93b8-5d56ce6724c5` | `Americano_e6a4d7f2-...` |
| 7 | Flat White | Starbucks | `fa1e12ff-67e4-42d5-bf45-c43576890f8a` | `Flat White_fa1e12ff-...` |
| 8 | Iced Coffee | Starbucks | `6d77f8a9-e640-4d2e-ba2c-b7db8ab2c123` | `Iced Coffee_6d77f8a9-...` |
| 9 | Affogato | Folgers | `ba5f15c4-1f72-4b97-b9cf-4437e5c6c2fa` | `Affogato_ba5f15c4-...` |
| 10 | Cortado | Nescafé | `3e9c1f94-ee3c-4e0b-9a6e-bb6e9c61c6f5` | `Cortado_3e9c1f94-...` |
| 11 | Cold Brew | Peet's Coffee | `3ea8e601-24c9-49b1-8c65-8db8b3a5c7a3` | `Cold Brew_3ea8e601-...` |
| 12 | Nitro Coffee | Starbucks | `eec8a1d8-4864-4c1b-aa8b-dedfddc6e356` | `Nitro Coffee_eec8a1d8-...` |
| 13 | Frappuccino | Illy | `7edf3a1e-c391-4d76-9002-2f0dd3e9c6e9` | `Frappuccino_7edf3a1e-...` |
| 14 | Turkish Coffee | Lavazza | `e0323d1b-1169-4a0e-8d7b-07ff3bfe7f7e` | `Turkish Coffee_e0323d1b-...` |
| 15 | Red Eye | Dunkin-Donuts | `e70f1d94-d55f-4e0e-8a6b-28e2ca3c6c34` | `Red Eye_e70f1d94-...` |
| 16 | Chai Latte | Peet's Coffee | `b5faee5d-6e6d-4319-ba9f-8d1bf7ee3f63` | `Chai Latte_b5faee5d-...` |
| 17 | Green Tea Latte | Lavazza | `25a8e8c1-37ba-4a8b-927f-5f1b4b5b5c3c` | `Green Tea Latte_25a8e8c1-...` |
| 18 | Hot Chocolate | Starbucks | `4e9a7d28-5e40-4b14-bc72-a5d1b547c3d0` | `Hot Chocolate_4e9a7d28-...` |
| 19 | Iced Latte | Illy | `cc0b6e5d-71f1-44cd-8a5b-1bdb978b5ca6` | `Iced Latte_cc0b6e5d-...` |
| 20 | Pumpkin Spice Latte | Folgers | `aa1d3e8f-9866-4e07-bc61-b5d8c2a3df4b` | `Pumpkin Spice Latte_aa1d3e8f-...` |
| 21 | Iced Macchiato | Nescafé | `eedc6cde-1e80-4ebf-a9d1-8e5e4eb2cacf` | `Iced Macchiato_eedc6cde-...` |
| 22 | Vanilla Latte | Peet's Coffee | `fc88cd5d-5049-4b00-8d88-df1d9b4a3ce1` | `Vanilla Latte_fc88cd5d-...` |
| 23 | Caramel Macchiato | Starbucks | `5efb7cfd-744b-4af9-b713-ef8d30abf628` | `Caramel Macchiato_5efb7cfd-...` |
| 24 | Peppermint Mocha | Dunkin-Donuts | `b183776e-6c3b-459a-bb3a-e93a4c8e4e56` | `Peppermint Mocha_b183776e-...` |
| 25 | Hazelnut Latte | Lavazza | `c3f45eec-18d8-43e0-9d7b-d85a4a9b6bda` | `Hazelnut Latte_c3f45eec-...` |
| 26 | Lemonade Iced Tea | Nescafé | `123f7a2d-cb34-4e5f-9a1d-4e4b456a03a7` | `Lemonade Iced Tea_123f7a2d-...` |

### New Products (v2.0 — 10 products, added July 2026)

| # | Product | Brand | UUID | S3 Folder |
|---|---------|-------|------|-----------|
| 27 | Oat Milk Latte | Oatly | `d1a2b3c4-0001-4000-8000-000000000001` | `Oat Milk Latte_d1a2b3c4-...` |
| 28 | Brown Sugar Shaken Espresso | Starbucks | `d1a2b3c4-0001-4000-8000-000000000002` | `Brown Sugar Shaken Espresso_d1a2b3c4-...` |
| 29 | Pistachio Latte | Lavazza | `d1a2b3c4-0001-4000-8000-000000000003` | `Pistachio Latte_d1a2b3c4-...` |
| 30 | Matcha Espresso Fusion | Illy | `d1a2b3c4-0001-4000-8000-000000000004` | `Matcha Espresso Fusion_d1a2b3c4-...` |
| 31 | Lavender Honey Latte | Peet's Coffee | `d1a2b3c4-0001-4000-8000-000000000005` | `Lavender Honey Latte_d1a2b3c4-...` |
| 32 | Iced Dirty Chai | Nescafé | `d1a2b3c4-0001-4000-8000-000000000006` | `Iced Dirty Chai_d1a2b3c4-...` |
| 33 | Coconut Cold Brew | Dunkin-Donuts | `d1a2b3c4-0001-4000-8000-000000000007` | `Coconut Cold Brew_d1a2b3c4-...` |
| 34 | Salted Caramel Cold Foam Brew | Starbucks | `d1a2b3c4-0001-4000-8000-000000000008` | `Salted Caramel Cold Foam Brew_d1a2b3c4-...` |
| 35 | Cascara Latte | Folgers | `d1a2b3c4-0001-4000-8000-000000000009` | `Cascara Latte_d1a2b3c4-...` |
| 36 | Strawberry Matcha Latte | Illy | `d1a2b3c4-0001-4000-8000-000000000010` | `Strawberry Matcha Latte_d1a2b3c4-...` |

### Brand Distribution

| Brand | Count | Products |
|-------|-------|----------|
| Starbucks | 8 | Flat White, Iced Coffee, Nitro Coffee, Hot Chocolate, Caramel Macchiato, Brown Sugar Shaken Espresso, Salted Caramel Cold Foam Brew |
| Peet's Coffee | 5 | Americano, Cold Brew, Chai Latte, Vanilla Latte, Lavender Honey Latte |
| Nescafé | 5 | Espresso, Cortado, Iced Macchiato, Lemonade Iced Tea, Iced Dirty Chai |
| Lavazza | 5 | Macchiato, Turkish Coffee, Green Tea Latte, Hazelnut Latte, Pistachio Latte |
| Illy | 5 | Cappuccino, Frappuccino, Iced Latte, Matcha Espresso Fusion, Strawberry Matcha Latte |
| Folgers | 4 | Latte, Affogato, Pumpkin Spice Latte, Cascara Latte |
| Dunkin-Donuts | 4 | Mocha, Red Eye, Peppermint Mocha, Coconut Cold Brew |
| Oatly | 1 | Oat Milk Latte |

---

## Git Workflow

The `seed/products/` directory is tracked in git. After generating images:

1. **Raw ChatGPT downloads** land in `seed/products/` with names like `Product #25- Hazelnut Latte (Lavazza).png` or `ChatGPT Image May 6, 2026, 08_46_01 PM.png`.
2. **Move each PNG** into the correct subfolder as `card_logo.png`.
3. **Delete the raw files** — don't commit them.
4. **Unstage any accidentally staged raw files:**
   ```bash
   cd seed/products
   git reset HEAD "ChatGPT Image*.png" "Product #*.png"
   rm -f "ChatGPT Image"*.png "Product #"*.png
   ```
5. **Stage only the card_logo.png files:**
   ```bash
   git add seed/products/*/card_logo.png
   ```

> **Note:** 36 PNGs at ~2 MB each = ~72 MB added to the repo. Consider using Git LFS if the repo size becomes a concern.

---

## Dual Image System

The backend has **two** image resolution mechanisms:

| System | Table | Use case | How it works |
|--------|-------|----------|--------------|
| `file_metadata` | `file_metadata` | Main product card image | S3 bucket index → `productFileUrl` field in API response |
| `product_image` | `product_image` | Product detail gallery (multiple images) | Direct URL stored in DB → `productImageUrls` array in API response |

The `card_logo.png` workflow uses the **first system** (`file_metadata`). The `product_image` table is for future multi-image galleries where URLs are stored directly in the database (not resolved from S3 keys).

---

## Frontend Image Rendering

The frontend uses Next.js `<Image>` component with remote patterns. For Supabase images to load:

- `NEXT_IMAGE_REMOTE_SOURCES` env var must include the Supabase public URL domain
- In prod: `https://fzvwwpzdudxrdzwbucaw.supabase.co`
- The frontend `ProductCard` component uses `aspect-[4/5]` CSS class — this is why we generate 4:5 images
- Product cards render the `productFileUrl` field from the API response
