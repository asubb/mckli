## MODIFIED Requirements

### Requirement: Full-text search across all tools
The system SHALL provide a `tools search` command that performs a similarity-based fuzzy search for a given query across all tool names and descriptions from all configured MCP servers.

#### Scenario: Fuzzy search with typos
- **WHEN** user runs `mckli tools search serch` (misspelling of "search")
- **THEN** the system SHALL display matches for "search" from all servers, ranked by similarity

### Requirement: Search result format
The system SHALL display each search result in the format `<server>:<tool-name> <preview> [score: <0.XX>]` by default, where `<preview>` is a snippet of the tool's description or name, and `<score>` is the similarity score.
The system SHALL sort results in descending order of similarity score.
The system SHALL also support a `--json` flag to output results as a JSON array of objects, including a `score` field.

#### Scenario: Displaying ranked search results
- **WHEN** user runs `mckli tools search tool`
- **THEN** results with higher similarity SHALL appear first in the output

### Requirement: Handling large descriptions
The system SHALL improve search relevance for long descriptions by breaking them into smaller chunks (sentences) and calculating the similarity for each chunk separately.

#### Scenario: Match in a specific sentence
- **GIVEN** a tool with a multi-sentence description
- **WHEN** the user provides a query that closely matches one specific sentence
- **THEN** that tool MUST receive a high similarity score corresponding to the matching sentence, rather than a low score based on the entire description length
