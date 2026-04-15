## Context

The current tool search in `mckli` uses a simple `String.contains` check in `ToolSearchService`. This implementation is limited and lacks robustness against typos or conceptual matches. To improve user experience, we need to introduce a similarity-based ranking system.

## Goals / Non-Goals

**Goals:**
- Implement a similarity metric (Levenshtein distance) that works in Kotlin Multiplatform.
- Update `ToolSearchService` to rank tools based on similarity to the user query.
- Maintain existing search functionality (substring matching) but with ranking.
- Keep performance acceptable for a CLI tool (hundreds of tools max).

**Non-Goals:**
- Implementing a full vector database or embedding-based search (out of scope for now).
- Persistent search indexing (keep it in-memory as tools are fetched dynamically).

## Decisions

### 1. Similarity Metric: Levenshtein Distance
- **Decision**: Use the Levenshtein distance algorithm to calculate string similarity.
- **Rationale**: It's well-understood, simple to implement in pure Kotlin (KMP compatible), and effective for catching typos in tool names/descriptions.
- **Alternatives**: 
  - Jaro-Winkler: Better for short strings like names, but Levenshtein is more general for descriptions.
  - Cosine Similarity: Requires vectorization (TF-IDF), which is overkill for this CLI's scale.

### 2. Normalization of Scores
- **Decision**: Normalize the distance into a score between 0.0 and 1.0 using the formula: `1.0 - (distance / max(query.length, target.length))`.
- **Rationale**: A normalized score is easier to display and use for thresholding.

### 3. Thresholding and Filtering
- **Decision**: Keep items with a similarity score > 0.3 or those that satisfy the existing `contains` check.
- **Rationale**: We don't want to show completely irrelevant results, but we should prioritize exact substring matches by giving them a high base score.

### 4. Sentence-based Similarity for Descriptions
- **Decision**: For descriptions, split the text into sentences (e.g., using `.` as delimiter) and calculate similarity between the query and each sentence. The final similarity score for the description will be the maximum of these individual scores.
- **Rationale**: A short user query will always have a very low Levenshtein distance similarity score when compared against a very long description. Breaking the description into sentences/chunks allows for identifying relevance within a specific context of a tool's description.

## Risks / Trade-offs

- **[Risk] Performance on large tool lists** → **Mitigation**: Tool lists are typically small (< 500 tools per server). Similarity calculation is O(N*M). If performance becomes an issue, we can limit the number of tools processed or use a faster heuristic first.
- **[Risk] Scoring relevance** → **Mitigation**: We will weight name matches higher than description matches to ensure tools with the query in their name appear first.
