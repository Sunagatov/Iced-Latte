# Product Package Remediation Log

This document explains the issues found and fixed in
`com.zufar.icedlatte.product` during the backend review.

The goal is not only to list changed files. It also explains why each issue
was a real bug or code smell, and what the fix changed.

Each issue uses this structure:

- What this feature is for
- How the old system behaved
- Why that behavior was a problem
- What the fix changed
- How it was verified

## 1. Product Search Allowed Unbounded Keyword Input

### What This Feature Is For

The product search endpoint lets clients filter products by name using a
`keyword` query parameter.

### How the Old System Behaved

The validator checked pagination, sorting, price filters, brands, sellers, and
rating filters. It did not validate the keyword length.

That meant a client could send a very large keyword string.

### Why That Behavior Was a Problem

Even if a request is syntactically valid, it can still be too expensive or too
large for normal use.

For a search endpoint, an unbounded keyword can waste memory, create noisy SQL,
and make the backend do unnecessary work.

### What the Fix Changed

`GetProductsRequestValidator` now validates the `keyword` parameter and rejects
values longer than 200 characters with a `BadRequestException`.

### How It Was Verified

`GetProductsRequestValidatorTest` was expanded to cover keyword validation.

## 2. Product Search Treated `%` and `_` as SQL Wildcards

### What This Feature Is For

Keyword search should find products whose names contain the text typed by the
user.

### How the Old System Behaved

The old code built a SQL `LIKE` pattern directly from user input:

```text
%user input%
```

In SQL `LIKE`, `%` means "any number of characters" and `_` means "one
character".

### Why That Behavior Was a Problem

If a user searched for `%`, the database could treat that as a wildcard instead
of a literal percent sign. That makes the search broader than the user asked
for.

Simple example:

```text
User searches for: %
Old meaning: match almost everything
Expected meaning: find product names containing the percent character
```

### What the Fix Changed

`ProductSpecifications.nameContainsSpec(...)` now:

- trims the keyword
- lowercases it with `Locale.ROOT`
- escapes `\`, `%`, and `_`
- passes an explicit escape character to Criteria API `like(...)`

### How It Was Verified

`ProductSpecificationsTest` and product filter tests were updated to cover
keyword behavior.

## 3. Product Image Ordering Was Not Stable

### What This Feature Is For

Products can have multiple image URLs. The backend should return them in a
predictable order so clients render the same product consistently.

### How the Old System Behaved

The image repository ordered single-product images only by `position`, and
batch image lookups only by `position`.

If two images had the same position, their relative order was not guaranteed.
Batch results were also not grouped with a stable product-id order.

### Why That Behavior Was a Problem

Database rows with equal sort values can come back in different orders. That can
make UI image order flicker or make tests flaky.

### What the Fix Changed

`ProductImageRepository` now orders images by deterministic tie-breakers:

- single product: `position ASC, id ASC`
- batch lookup: `productId ASC, position ASC, id ASC`

`ProductImageReceiver` was updated to call the new repository methods.

### How It Was Verified

`ProductImageReceiverTest` was updated for deterministic batch image behavior.

## 4. Product Lists by ID Could Return Ambiguous Results

### What This Feature Is For

The backend has APIs that load several products by a list of product IDs. This
is used by other features that need product snapshots.

### How the Old System Behaved

The service loaded products with `findAllById(...)`, converted them, and then
rebuilt the result list in the same order as the input IDs.

The missing-product check was useful, but duplicate input IDs were not rejected.

### Why That Behavior Was a Problem

A product-id list is normally a set of requested products. Duplicate IDs make
the request ambiguous and can hide client mistakes.

Simple example:

```text
Request ids: [A, A, B]
```

The backend could return duplicated product data instead of telling the client
that the request is malformed.

### What the Fix Changed

`ProductService.validateProductIds(...)` now rejects duplicate and null IDs with
`BadRequestException`.

The service still preserves the caller's requested order for valid unique IDs.

### How It Was Verified

`ProductServiceTest` was expanded for:

- empty ID list
- missing products
- duplicate IDs
- null IDs
- preserving requested order

## 5. Product Cache Could Become Stale After Review Aggregate Updates

### What This Feature Is For

Product DTOs are cached by product ID in the `productById` cache. Product
reviews can change product aggregate fields, such as average rating and review
count.

### How the Old System Behaved

`ProductReviewProductGateway.refreshReviewAggregates(...)` updated the product
row in the database, but cached product DTOs were not evicted.

### Why That Behavior Was a Problem

If a product was cached before review aggregates changed, users could keep
seeing the old average rating or old review count until the cache expired.

Simple example:

```text
1. Product page is loaded and cached with rating 4.0.
2. New review changes average rating to 4.5.
3. Database is updated.
4. Cache still returns rating 4.0.
```

### What the Fix Changed

The review gateway now evicts the product cache when review-driven product data
changes:

- `refreshReviewAggregates(productId)` evicts `productById` for that product ID
- `updateAiSummary(productId, summary)` evicts `productById` for that product ID
- `refreshAllReviewAggregates()` evicts all `productById` entries

### How It Was Verified

`ProductReviewProductGatewayTest` was added to cover review aggregate refresh
and AI summary update behavior.

## 6. Product Entity Constraints Were Weaker Than the Business Model

### What This Feature Is For

`ProductInfo` maps the Java product entity to the `product` database table.

### How the Old System Behaved

Several fields that are required by the business model were not marked
`nullable = false` in the JPA mapping.

Examples included:

- `quantity`
- `active`
- `averageRating`
- `reviewsCount`
- dimensions and weight
- `soldProductsCount`
- `discount`
- `popularityScore`

### Why That Behavior Was a Problem

The Java entity should describe the same required fields as the database and
business model.

If the mapping is loose, future code can accidentally treat required data as
optional, and schema generation or validation tools get weaker information.

### What the Fix Changed

`ProductInfo` now marks required product fields as non-null in the JPA column
mapping.

A Liquibase migration was added to harden database constraints for existing
product catalog fields.

### How It Was Verified

`ProductInfoTest` was added, and product-focused tests were run after the
change.

## 7. Entity Equality Treated Two New Products as Equal

### What This Feature Is For

`ProductInfo.equals(...)` defines when two product entity objects should be
considered the same.

### How the Old System Behaved

The old implementation compared only IDs:

```text
Objects.equals(id, other.id)
```

For two new unsaved entities, both IDs can be `null`. That made two different
new products look equal.

### Why That Behavior Was a Problem

Two unsaved products are not the same product just because neither has a
database ID yet.

Simple example:

```text
Product A id = null
Product B id = null
Old equals result = true
```

That can break sets, maps, tests, and persistence logic that depends on entity
identity.

### What the Fix Changed

`ProductInfo.equals(...)` now returns `false` when either entity ID is `null`,
unless both references point to the exact same object.

### How It Was Verified

`ProductInfoTest` covers equality for transient and persisted-style entities.

## 8. Active Product Filtering Was Removed as Overengineering

### What This Feature Was For

The product package briefly added "active product" filtering. The idea was that
inactive products would be hidden from catalog reads and product existence
checks.

### How the System Behaved After That Change

Product reads and review gateway existence checks used active-only repository
methods such as:

- `findByIdAndActiveTrue(...)`
- `findAllByIdInAndActiveTrue(...)`
- `existsByIdAndActiveTrue(...)`

Product listing also included an `activeSpec()`.

### Why That Behavior Was Removed

For this pet project, the feature added extra business rules without a clear
current workflow for managing active/inactive products.

That made the backend more complex than needed.

### What the Fix Changed

The active-only behavior was removed:

- normal product reads use `findById(...)`
- product ID batch reads use `findAllById(...)`
- existence checks use `existsById(...)`
- `activeSpec()` was removed
- active-only repository methods were removed
- tests expecting active-only filtering were removed or adjusted

The `active` column can still exist as product data, but it no longer controls
catalog visibility.

### How It Was Verified

The product test suite was run after removal:

```text
mvn -Dspotless.check.skip=true -Dtest='ProductServiceTest,ProductReviewProductGatewayTest,ProductFiltersIntegrationTest,ProductsEndpointTest,ProductSpecificationsTest' test
```

Result: all selected product tests passed.

## 9. Product ID Null Validation Needed to Match Runtime JSON Input

### What This Feature Is For

`ProductService.validateProductIds(...)` validates lists of product IDs before
loading products in batch.

### How the Old System Behaved

During cleanup, an IDE warning reported that this condition was always false:

```text
ids.stream().anyMatch(Objects::isNull)
```

### Why That Behavior Was a Problem

The warning was valid from the static type system's point of view:
`List<UUID>` means a list of non-null UUIDs in this project.

But JSON input can still deserialize to a list that contains a null element
unless the API boundary explicitly rejects it.

Simple example:

```json
{
  "productIds": ["418499f3-d951-40bf-9414-5cb90ab21ecb", null]
}
```

If the service trusts that every list element is non-null, this malformed
request can fail later as a less clear server error.

### What the Fix Changed

The validation helper now accepts `List<@Nullable UUID>` internally and checks
for null elements before duplicate detection.

That keeps the runtime protection without lying to the nullness model.

The validation now rejects:

- null product ID elements
- duplicate product IDs

### How It Was Verified

`ProductServiceTest` covers null and duplicate product ID rejection.

## 10. Dead Static-Analysis Warning Was Removed Correctly

### What This Feature Is For

The product ID validator should be readable and should not carry code that the
static analyzer considers impossible.

### How the Old System Behaved

The first cleanup removed the null-element check completely to satisfy the IDE
warning.

### Why That Behavior Was a Problem

That removed useful runtime protection for malformed JSON input.

The better fix was not to delete the validation. The better fix was to express
the helper method's real job: it validates possibly-null values received from an
external boundary.

### What the Fix Changed

The helper signature was changed to `List<@Nullable UUID>`, and the null check
was restored.

### How It Was Verified

The focused product service test was run:

```text
mvn -Dspotless.check.skip=true -Dtest=ProductServiceTest test
```

Result: product service tests passed.

## Verification Summary

The product package changes were verified with focused product tests, including:

```text
mvn -Dspotless.check.skip=true -Dtest='ProductServiceTest,ProductReviewProductGatewayTest,ProductFiltersIntegrationTest,ProductsEndpointTest,ProductSpecificationsTest' test
mvn -Dspotless.check.skip=true -Dtest=ProductServiceTest test
```

The backend was also restarted locally against production-configured resources
from Vault and checked with:

```text
GET http://127.0.0.1:8083/actuator/health -> 200
GET http://127.0.0.1:8083/api/v1/products -> 200
```
