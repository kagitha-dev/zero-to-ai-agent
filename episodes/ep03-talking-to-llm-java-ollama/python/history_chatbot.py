import json
import urllib.request


def get_body(messages):
    body = {
        "model": "qwen3.5:4b",
        "messages": messages,
        "think": False,
        "stream": False,
    }
    return json.dumps(body)


def chat(body):
    request = urllib.request.Request(
        "http://localhost:11434/api/chat",
        data=body.encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request) as response:
        # Pull just the reply text out of Ollama's JSON
        return json.loads(response.read())["message"]["content"]


def main():
    history = []
    while True:
        history.append({"role": "user", "content": input()})
        body = get_body(history)
        reply = chat(body)
        print(reply)
        history.append({"role": "assistant", "content": reply})


if __name__ == "__main__":
    main()
