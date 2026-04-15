## 1. Core Similarity Engine

- [x] 1.1 Implement `LevenshteinDistance` utility in `src/commonMain/kotlin/com/mckli/tools/SimilarityUtils.kt`
- [x] 1.2 Implement `calculateSimilarityScore` function with normalization
- [x] 1.3 Add sentence/chunking logic for similarity scoring in long strings
- [x] 1.4 Add unit tests for similarity calculations, including multi-sentence descriptions, in `src/commonTest/kotlin/com/mckli/tools/SimilarityUtilsTest.kt`

## 2. Tool Search Service Enhancements

- [x] 2.1 Update `SearchResult` data class to include an optional `score: Double`
- [x] 2.2 Modify `ToolSearchService.filterTools` to calculate similarity scores for each tool
- [x] 2.3 Implement relevance-based sorting in `ToolSearchService.searchAcrossServers`
- [x] 2.4 Add unit tests for ranked tool search in `src/commonTest/kotlin/com/mckli/tools/ToolSearchTest.kt`

## 3. CLI Command Updates

- [x] 3.1 Update `ToolsSearchCommand` in `src/commonMain/kotlin/com/mckli/tools/ToolCommands.kt` to display similarity scores in text output
- [x] 3.2 Ensure `--json` output in `ToolsSearchCommand` includes the similarity score
- [x] 3.3 Verify UI formatting for ranked results

## 4. Verification

- [x] 4.1 Run all tool search related tests
- [x] 4.2 Perform manual verification with typo-based queries (e.g., `mckli tools search serch`)
