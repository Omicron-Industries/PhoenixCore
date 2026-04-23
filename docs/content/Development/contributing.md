# Working in PhoenixCore

Welcome to the development team! This guide will help you get your environment set up and familiarize you with our workflow.

---

## 🛠️ Environment Setup

1.  **JDK**: Ensure you have **JDK 17** installed.
2.  **IDE**: We recommend **IntelliJ IDEA** with the Minecraft Development plugin.
3.  **Clone**: `git clone https://github.com/Phoenixvine32908/PhoenixCore.git`
4.  **Import**: Open the project as a Gradle project in your IDE.

---

## 🚀 Workflow

### 1. Feature Branches
Always create a new branch for your feature or bugfix:
`git checkout -b feature/your-feature-name`

### 2. Code Style
We follow standard Java naming conventions. For GregTech-specific registration, refer to existing classes in `net.phoenix.core.common.machine` for patterns.

### 3. Documentation
If you add a new system, you **must** document it.
- Follow the [Documentation Style Guide](style_guide.md).
- Ensure player-side info is in `docs/content/Gameplay/` and dev-side info is in `docs/content/Development/`.

---

## 📦 Project Structure

- **`src/main/java/net/phoenix/core/api`**: Public APIs and interfaces.
- **`src/main/java/net/phoenix/core/common`**: Core machine, item, and block logic.
- **`src/main/java/net/phoenix/core/integration`**: Third-party mod integrations (AE2, Ars Nouveau, etc.).
- **`src/main/java/net/phoenix/core/mixin`**: Core logic injections.

---

## 🧪 Testing

- **Client**: Use the `runClient` Gradle task.
- **Server**: Use the `runServer` Gradle task to test multi-dimensional and SavedData persistence.

---

## 📢 Communication

If you have questions or want to discuss a feature, reach out on [Our Discord](https://discord.gg/KBNst7hZ4C) or poke Phoenixvine directly.
