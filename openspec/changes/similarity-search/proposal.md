## Why

The current tool search implementation relies on simple case-insensitive string containment (`contains`). This is highly restrictive and fails when users make typos or use slightly different terminology than what's in the tool name or description. Better search using similarity checks (like Levenshtein distance) will make the CLI more user-friendly and robust.

## What Changes

- Introduce fuzzy/similarity-based filtering for tool searches.
- Calculate a similarity score for each tool based on the user's query.
- **Improved accuracy for long descriptions** by using sentence-based similarity checks.
- Sort search results by relevance/similarity score.
- **BREAKING**: Change `ToolSearchService.filterTools` signature to support scoring or return sorted results.

## Capabilities

### New Capabilities
- `similarity-search-engine`: Implementation of the similarity checking logic (e.g., Levenshtein distance) and result ranking.

### Modified Capabilities
- `tool-search`: Update the tool search requirements to include fuzzy matching and relevance sorting.

## Impact

- `ToolSearchService`: Primary implementation change for search logic.
- `ToolCommands`: UI impact in how search results are presented to the user.
- Performance: Similarity checks are computationally more expensive than simple containment, so efficient implementation is required.
