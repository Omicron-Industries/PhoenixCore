# GTM RecipeHelper Utility

The `RecipeHelper` class in the GTCEu API is an essential tool for developers creating custom machines that require manual control over recipe matching, input consumption, and output generation.

---

## 1. Core Functions

### `matchRecipe(machine, recipe)`
Checks if the machine's current inventories and capabilities (energy, fluids, items) satisfy the requirements of a specific recipe.
- **Usage**: Used to validate if a recipe *can* start without actually consuming anything.
- **Example**:
  ```java
  if (RecipeHelper.matchRecipe(this, myRecipe).isSuccess()) {
      // Recipe can be performed
  }
  ```

### `handleRecipeIO(machine, recipe, io, chanceCaches)`
Manages the actual movement of items and fluids for a recipe.
- **`IO.IN`**: Consumes the required inputs from the machine's hatches/slots.
- **`IO.OUT`**: Generates the outputs into the machine's export hatches/slots.
- **`chanceCaches`**: A map used to track randomized output results across multiple ticks or threads.

### `handleTickRecipeIO(machine, recipe, io, chanceCaches)`
Specifically used for recipes that consume energy per tick (like electric machines).
- It handles the subtraction of EU from the machine's energy buffer.
- Returns a result indicating if the machine has enough energy to continue for that specific tick.

---

## 2. Advanced Use Cases in PhoenixCore

### Threaded Processing (`BasicThreadedMachine`)
Because threaded machines run multiple independent recipes, they cannot rely on GTM's built-in `RecipeLogic`. Instead, they use `RecipeHelper` to manually tick and complete recipes across different threads.

```java
// Ticking a specific thread
var euResult = RecipeHelper.handleTickRecipeIO(this, thread.recipe, IO.IN, thread.chanceCaches);

// Completing a thread and pushing items to output
if (thread.progress >= thread.duration) {
    RecipeHelper.handleRecipeIO(this, thread.recipe, IO.OUT, thread.chanceCaches);
}
```

### Machine-Driven Logic (Fission Reactors)
Fission reactors use `RecipeHelper` to "simulate" recipes for fuel consumption and isotope production. By creating "Dummy" recipes on the fly, the reactor can reuse GTM's IO logic without needing a pre-defined recipe in the registry.

```java
// Generating a dummy recipe for isotope production
GTRecipe dummy = GTRecipeBuilder.ofRaw()
        .outputItems(myIsotopeStack)
        .buildRawRecipe();

// Pushing the generated items out
RecipeHelper.handleRecipeIO(this, dummy, IO.OUT, this.recipeLogic.getChanceCaches());
```

---

## 3. Best Practices
- **Always use `isSuccess()`**: Most `RecipeHelper` methods return a result object. Always check if the operation succeeded before proceeding.
- **Chance Caches**: When working with multiple threads or custom IO, ensure each operation has access to its own `chanceCaches` to avoid unexpected results in randomized recipes.
- **Overclocking**: `RecipeHelper` typically works with *already modified* recipes. Ensure you apply any `RecipeModifier` (like overclocking) before calling these helper methods.
