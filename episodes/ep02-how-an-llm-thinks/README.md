# Ep 2 · How an LLM actually "thinks"

**Zero to AI Agent · Foundations**

Every answer ChatGPT ever gave you was written one piece at a time: guess → add → guess → add. In this episode we build a tiny language model, train it, open its weights and watch it predict.

📺 **Video:** _link coming soon_ · 📄 **Slides:** [PDF](slides/ep02-how-an-llm-thinks.pdf) · [PowerPoint](slides/ep02-how-an-llm-thinks.pptx)

## Lab · Tiny LLM Lab (runs in your browser)

▶️ **Try it:** [kagitha-dev.github.io/tiny-llm-lab](https://kagitha-dev.github.io/tiny-llm-lab/) · source: [kagitha-dev/tiny-llm-lab](https://github.com/kagitha-dev/tiny-llm-lab)

A real language model small enough to see every weight, trained on *"My puppy name is River. River likes Ruby."*

| Lab | What you do |
|---|---|
| 1 · Tiny LLM Lab | Train it, save its weights to a file, then freeze them and predict the next token |
| 2 · Text to numbers | Watch a sentence become tokens → IDs → embedding rows → a grid of numbers |
| 3 · Transformer | Follow a prompt through a real one-block transformer |
| 4 · How a weight is made | Follow one number, "my → puppy", from random to learned |

The lab has no server, install or AI library, and it includes a Python version (`tiny_llm.py`, numpy only). It lives in its own repo so it can be used on its own.

## What you'll learn

| Term | In one line |
|---|---|
| **Model** | Learned weights plus fixed maths: text in, a chance for every possible next token out |
| **Weights (parameters)** | The learned numbers, saved as files like `.safetensors` or `.gguf` (lab: 64 · GPT-2: 124M · Llama 3 8B: 8B) |
| **Tokens and the tokenizer** | Text cut into pieces, each with an ID |
| **Embedding** | The row of learned numbers a token's ID picks from the embedding table |
| **Next-token prediction** | Scores → softmax → chances → pick one → add it → repeat |
| **Sampling and temperature** | Picking by chance; temperature makes the choice more predictable or more random |
| **Context window** | How many tokens the model can see at once |
| **Training vs inference** | Training changes the weights; chatting only reads them |

Also covered: **pre-training** (base model), **post-training and fine-tuning** (helpful assistant), the life of an LLM, and why it **hallucinates**: it predicts what is *likely*, not what is *true*.

## Next

[Ep 3 · Talking to an LLM with code →](../ep03-talking-to-llm-java-ollama)
