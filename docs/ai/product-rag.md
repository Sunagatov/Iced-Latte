# Product RAG endpoint

This document describes the first lightweight Retrieval-Augmented Generation (RAG) slice for the backend.

## What it does

It adds a product-specific AI endpoint that:

- loads the target product
- retrieves grounded context from that product's:
  - description
  - AI summary (if present)
  - customer reviews
- ranks the most relevant context snippets for the user question
- sends only that retrieved context to the LLM
- returns both the answer and the retrieved sources

This is intentionally a **small learning-friendly RAG slice** rather than a full site-wide chatbot.

## Endpoint

```http
POST /api/v1/products/{productId}/rag/ask
```

## Request body

```json
{
  "question": "Does this coffee taste chocolatey?"
}
```

## Example response

```json
{
  "productId": "7aef9ecb-7a24-4f38-a8a2-85bf86acaa8c",
  "productName": "Brazilian Espresso Beans",
  "question": "Does this coffee taste chocolatey?",
  "answer": "Yes. The retrieved context mentions chocolate notes in both the product details and customer feedback.",
  "sources": [
    {
      "sourceType": "PRODUCT_DETAILS",
      "sourceLabel": "Product details",
      "excerpt": "Product name: Brazilian Espresso Beans. Description: A rich medium roast with chocolate, caramel, and hazelnut notes...",
      "score": 56.0
    },
    {
      "sourceType": "REVIEW",
      "sourceLabel": "Review #1 — rating 5/5",
      "excerpt": "Rating: 5/5. Review text: Very chocolatey, smooth, and easy to drink.",
      "score": 27.0
    }
  ]
}
```

## Enablement

This endpoint is available when:

- `AI_ENABLED=true`
- a working AI API key and base URL are configured

Optional tuning knobs can be provided without changing code:

- `AI_RAG_MAX_CONTEXT_ITEMS` via `ai.rag.max-context-items` property placeholder support
- `AI_RAG_MAX_CONTEXT_CHARS_PER_ITEM` via `ai.rag.max-context-chars-per-item` property placeholder support

## Why this approach

This first version keeps the implementation simple and easy to understand:

- no extra vector database setup
- no new infrastructure requirement
- context retrieval is transparent because the API returns the snippets it used

That makes it a good base for later upgrades such as embeddings, pgvector, semantic search, or a frontend chat UI.
