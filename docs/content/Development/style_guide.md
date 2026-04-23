# Documentation Style Guide

To maintain consistency and high quality across the PhoenixCore wiki, please follow these guidelines when creating or editing pages.

---

## 🏗️ Page Structure

1.  **Title**: Use a clear `# H1` header at the top.
2.  **Introduction**: A brief 1-2 sentence overview of the system or tool.
3.  **Key Concepts**: Use `## H2` headers for major sections.
4.  **Source References**: Always link to the relevant Java files in the GitHub repository.

---

## 💻 Code Blocks

Use code blocks to illustrate implementation details. Keep them concise.

-   **Language**: Always specify `java` or `json`.
-   **Context**: Include enough surrounding code to make the snippet understandable, but avoid pasting entire files.
-   **Annotations**: Use comments to explain complex logic within the block.

**Example:**
```java
// Logic for handling custom soul growth
if (recipe.data.contains("soul_growth")) {
    SoulGrowthHook.handle(recipe, level, pos);
}
```

---

## 📝 Formatting & Admonitions

Use MkDocs Material features to highlight important information.

-   **Important Info**: Use `!!! info "Title"` for useful tips.
-   **Warnings**: Use `!!! warning "Title"` for dangerous mechanics (e.g., meltdowns).
-   **Links**: Use `!!! link "Title"` for external references or source code links.

---

## 🔗 Linking to Source Code

At the bottom of technical pages, provide links to the primary source files.

**Format:**
```markdown
!!! link "Source Code"
    [:material-github: FissionMachine.java](https://github.com/Phoenixvine32908/PhoenixCore/blob/main/src/main/java/net/phoenix/core/integration/phoenix_fission/common/data/multiblock/fission/FissionWorkableElectricMultiblockMachine.java)
```

---

## 🎨 Visuals (Optional)

-   **Screenshots**: Place in `assets/img/` and use standard markdown: `![Alt text](../../assets/img/example.png)`.
-   **Icons**: Use Material Design Icons where possible (e.g., `:material-cog:`).

---

## ✅ Checklist before Submitting

- [ ] Does the page have a clear H1 title?
- [ ] Are all Java classes mentioned linked to their source file?
- [ ] Is the language specified for all code blocks?
- [ ] Is the page added to the relevant `.pages` file for navigation?
