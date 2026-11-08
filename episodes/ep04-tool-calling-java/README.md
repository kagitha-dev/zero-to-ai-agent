# Ep 4 · Giving the LLM hands — Tool calling in Java

**Zero to AI Agent · Java lab · 🚧 coming soon**

An LLM can only write text. With **tool calling** it can ask *your* Java code to do something, like look up an order, and then answer using the real result.

## Planned

- How tool calling works: the model asks, your code runs the tool, the result goes back into the context window
- Describing a Java method as a tool (name, description, JSON schema for its arguments)
- The refund-bot example from Ep 3: `get_order(4521)` returns the real order instead of guessing
- Running it locally with Ollama (using a model tagged **tools**), and with a provider's official Java SDK

**Tools:** Java 17+, Maven, Ollama

[← Ep 3](../ep03-talking-to-llm-java-ollama) · [Ep 5 →](../ep05-mcp-java-sdk)
