# Dependencies and Data Structures

This reflects what is currently in the codebase.

## Domain Types

- `Conversation`: session metadata and message list
- `ChatMessage`: single message
- `ToolCall`: tool invocation payload
- `Role`: message role enum

## Service Layer

- `OpenAiService`: OpenAI chat completions with tool loop
- `Tool`: tool contract
- `WeatherForecastTool`: Open-Meteo weather tool

## Dependencies

- tui4j
- openai-java

## Persistence

No persistence layer yet.
