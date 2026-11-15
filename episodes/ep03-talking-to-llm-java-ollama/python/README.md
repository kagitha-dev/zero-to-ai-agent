# Ep 3 · Python version

The same two programs as the Java `ai_simplest` package, written in Python.
They use only the standard library (`urllib` + `json`), so there is nothing to `pip install`.

| File | Java original | What it shows |
|---|---|---|
| `simple_chatbot.py` | `SimpleChatbot.java` | One call to `/api/chat`, printing the raw JSON reply |
| `history_chatbot.py` | `HistoryChatbot.java` | A terminal chat loop that keeps a `history` list and resends it each turn |

## Run

Needs **Python 3.8+** and Ollama running with the model pulled (`ollama pull qwen3.5:4b`).

```bash
cd episodes/ep03-talking-to-llm-java-ollama/python
python simple_chatbot.py
python history_chatbot.py
```

In `history_chatbot.py`, type a message and press Enter. Press `Ctrl+C` to quit.
