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
