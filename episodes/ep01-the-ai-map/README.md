# Ep 1 · The AI Map — What is AI, really?

**Zero to AI Agent · Foundations**

What do AI, machine learning, neural networks, generative AI and LLMs actually mean, and how do they fit inside each other? This episode starts with a no-code lab and ends with a map of the whole field.

📺 **Video:** _link coming soon_ · 🖥️ **Slides:** [open in your browser](https://kagitha-dev.github.io/zero-to-ai-agent/episodes/ep01-the-ai-map/slides/ep01-what-is-ai-slides.html) · [source](slides/ep01-what-is-ai-slides.html)

## Lab · Google Teachable Machine (no code)

Teach a computer **left hand vs right hand** using only examples, at [teachablemachine.withgoogle.com](https://teachablemachine.withgoogle.com) → *Pose project*.

1. **Show examples:** record photos with your left hand up, then your right hand up.
2. **Train:** one click. The computer finds what makes "left" different from "right".
3. **Test it:** raise a hand in the preview and watch it predict live.

Then break it: move your left hand to the right side of the frame. The model says "Right", because it learned *where* the hand was, not *which* hand. The fix is **better examples, not new code**: we change the data, and the program stays the same.

## What you'll learn

- **Train → Learn → Predict:** the loop every modern AI follows
- **The AI map**, each one inside the one before:

  ```
  Artificial intelligence
  └── Machine learning            learns patterns from examples, not hand-written rules
      └── Neural networks         neurons in layers, connected by weights
          └── Generative AI       creates new content instead of picking a label
              └── LLMs            trained on huge amounts of text to predict what comes next
  ```

- **How training works:** a weight is a number on each connection. Input × weight, added up, gives each answer a score. A wrong guess nudges weights toward the right answer.
- **How recognizing works:** after training the weights are frozen. A new input is multiplied by them and the biggest score wins.
- **An LLM is a neural network** built for text, whose knowledge is stored as billions of weights.

## Next

[← Ep 0 · The Roadmap](../ep00-series-roadmap) · [Ep 2 · How an LLM actually "thinks" →](../ep02-how-an-llm-thinks)
