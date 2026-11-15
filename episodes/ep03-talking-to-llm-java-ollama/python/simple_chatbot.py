import json
import urllib.request

# The exact JSON the curl command sent
body = {
    "model": "qwen3.5:4b",
    "messages": [
        {"role": "user", "content": "Explain RAG in two sentences."}
    ],
    "think": False,
    "stream": False,
}

request = urllib.request.Request(
    "http://localhost:11434/api/chat",
    data=json.dumps(body).encode("utf-8"),
    headers={"Content-Type": "application/json"},
    method="POST",
)

with urllib.request.urlopen(request) as response:
    # Print everything Ollama sent back: raw JSON
    print(response.read().decode("utf-8"))
