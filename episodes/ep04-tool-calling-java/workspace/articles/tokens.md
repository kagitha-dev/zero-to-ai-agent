# Tokens: how an LLM reads text

An LLM never sees letters or words. Before your text reaches the model, a tokenizer cuts it into small pieces called tokens. A token can be a whole word ("the"), part of a word ("Pras" + "ad"), or a single symbol like "?".

Each token is then turned into a number, and the model only works with those numbers. It reads all the tokens in its context window and predicts the next token, one at a time, until the answer is complete.

This is why token counts matter so much. The context window has a limit in tokens, cloud providers charge per token, and longer prompts take longer to process. It also explains a few odd habits: a model can struggle to count letters or words, because it never sees them directly.
