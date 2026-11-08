# Ep 6 · RAG: giving the agent your knowledge

**Zero to AI Agent · Java lab · 🚧 coming soon**

You can't paste 500 policy documents into every request. **RAG (retrieval-augmented generation)** finds only the paragraph that matters and puts that into the context window.

## Planned

- Embeddings: turning text into numbers so similar meanings sit close together (an idea from Ep 2)
- Chunking documents, embedding them, and searching by similarity
- An in-memory vector store first, then **pgvector** or **Chroma**
- The refund-bot example: about 285 tokens instead of 6,325, with the same right answer

**Tools:** Java 17+, Maven, an embeddings API (or an Ollama embedding model), pgvector or Chroma

[← Ep 5](../ep05-mcp-java-sdk) · [Ep 7 →](../ep07-langchain4j-agent)
