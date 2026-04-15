## ADDED Requirements

### Requirement: Levenshtein distance calculation
The system SHALL provide a utility to calculate the Levenshtein distance between two strings to measure their similarity.

#### Scenario: Identical strings
- **WHEN** calculating distance between "apple" and "apple"
- **THEN** the result MUST be 0

#### Scenario: Single character difference
- **WHEN** calculating distance between "apple" and "apply"
- **THEN** the result MUST be 1

### Requirement: Normalized similarity score
The system SHALL provide a normalized similarity score between 0.0 and 1.0, where 1.0 is an exact match and 0.0 is completely different.

#### Scenario: Partial match score
- **WHEN** calculating similarity between "tool" and "tools"
- **THEN** the result MUST be approximately 0.8 (4/5)

### Requirement: Relevance ranking
The system SHALL support ranking a list of items based on their similarity score to a given query.

#### Scenario: Ranking multiple items
- **WHEN** querying "search" against ["research", "searching", "banana"]
- **THEN** "searching" SHOULD rank higher than "research", and "banana" SHOULD be at the bottom
