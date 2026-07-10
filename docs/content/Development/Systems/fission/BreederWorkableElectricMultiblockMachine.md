# Fission Reactor Physics, formula, and Experience Systems guide.

This tries to explain PF to the best of my ability.

---

## 1. Core Mathematical Formulas

### Parallel Computation

Recipe parallels are computed once at structure formation and cached as `lastParallels`.

> `parallels = clamp(1, (rodCount * parallelsPerFuelRod) + sum(moderator.parallelBonus), maxParallels)`

### Moderator Bonuses

EU boost and fuel discount are grabbed linearly across all installed moderators and are hard-capped by config.

> `euBoost = min(sum of all euBoost, maxEUBoostPercent)`  
> `fuelDiscount = min(sum of all fuelDiscount, maxFuelDiscountPercent)`

These feed into the recipe modifier:

> `EUt multiplier = 1.0 + (euBoost / 100)`  
> `duration multiplier = max(0.01, 1.0 - (fuelDiscount / 100))`

### Reactivity Ramping

`reactivityFactor` is a value between 0 and 1 that gates all heat and fuel production. It ramps up or down each tick by `reactivityRampRatePerTick` depending on whether the reactor is running.

> When running: `reactivity = min(1.0, reactivity + rampRate)`  
> Otherwise: `reactivity = max(0.0, reactivity - rampRate)`

### Heat Generation

Each tick, `calculateTickHeat(parallels)` computes the total heat produced across all fuel rods.

> `rodInteraction = (totalRods + 1) / 2`  
> `thermalFactor = (1 + heat / maxSafeHeat) ^ heatGenerationExnent`  
> `heatPerRod = baseHeatProduction * rodInteraction * thermalFactor * parallels * modBonus * reactivity * fuelConductivity`  
> `totalRawHeat = sum over all rods of heatPerRod`  
> `heatGenerated = (totalRawHeat / rodCount) * burnMultiplier`
po
The burn multiplier is a linear ramp that rewards continuous operation.

> `burnMultiplier = 1.0 + (burnBonusMaxPercent / 100) * min(1.0, continuousBurnTicks / (burnBonusRampSeconds * 20))`

### Fuel Consumption

Each tick, each fuel rod type consumes items at a rate determined by heat level and reactivity.

> `heatScalar = (1 + heat / maxSafeHeat) ^ fuelConsumptionExponent * reactivity`  
> `consumptionPerTick = (amountPerCycle * rodInteraction * heatScalar * parallels * discountMult) / durationTicks`

Consumption is fractional and accumulated via a per-type remainder buffer. So we don't get any bad "tries to consume half an item and uh oh we can't do that".

### Thermal Tick (Heat Flow)

Every tick the heat value is updated in this order:

1. **Ambient decay** pulls heat toward `ambientTemperatureHU` at rate `passiveCoolingConductivity`:
   > `delta = (ambientTemp - heat) * passiveCoolingConductivity`

2. **Passive (dry) coolers** subtract a flat amount per block, capped at heat above `minHeat`:
   > `removed = min(flatCoolingHUt, heat - minHeat)` per passive cooler block

3. **Active fluid coolers** only trigger when `coolerTemperature < heat`. If `coolingRequiresCoolant` is enabled, the coolant fluid must be present and is consumed. Cooling is then applied via temperature difference.
   > `delta = (coolerTemperature - heat) * count * activeCoolingConductivity` (only applied when negative, ie actually cooling)

### EU Generation

EU output per tick uses heat activity as its signal. While running, `lastHeatGainedPerTick` is used as the activity value. Otherwise the current heat level is used directly.

> `baseEU = activity * euPerHeatUnit`  
> `heatFraction = heat / maxSafeHeat` (clamped 0–1.5)  
> `curve = ((heatFraction - powerStartFraction) / (1 - powerStartFraction))` (clamped 0–1)  
> `dangerBonus = 1.0 + curve ^ powerCurveExponent * 1.5`  
> `finalEUt = baseEU * dangerBonus`

The result is clamped to `[minGeneratedEUt, maxGeneratedEUt]` if those are set, and injected directly into the energy container.

### Meltdown Timer

The timer starts when heat exceeds `maxSafeHeat`. Grace period shrinks as heat rises further above the threshold.

> `excessFraction = (heat - maxSafeHeat) / maxSafeHeat`  
> `scale = excessFraction * excessHeatSeverity`  
> `gracePeriod = max(minGraceSeconds, baseGraceSeconds - (baseGraceSeconds - minGraceSeconds) * min(1.0, scale))`

If heat reaches `maxHeatClamp`, the grace period is immediately forced to `minGraceSeconds`. While a SCRAM is active the countdown is frozen. Dropping back below `maxSafeHeat` resets the timer if `clearTimerWhenSafe` is enabled.

### Explosion Scaling

When the timer hits zero:

> `explosionPower = baseExplosionPower + (rodCount * explosionPowerPerFuelRod) + (avgRodBaseHeat * explosionPowerPerHeatUnit)`

If `destructiveExplosion` is enabled, the structure's casing, fuel rod, and moderator blocks are vaporized first, then a standard Minecraft explosion fires at the controller position. (havent tested this in a while just fyi)

---

## 2. Necessary vs. Optional Mechanics

### Necessary Mechanics

**Fuel rod count vs. cooling capacity** is the core engineering loop. Players need to balance how many rods they install, which scales heat generation, parallels, and fuel consumption together, against the cooling throughput their cooler blocks and fluid supply can handle. Without this tension the reactor has no meaningful challenge.

**Passive dry cooling as a safety buffer** prevents instant failure during interruptions. Passive cooler blocks subtract a flat amount of heat every tick with no fluid nessecary, giving players time to notice a problem and react before the meltdown timer runs out.

**Reactivity ramping** prevents instant full-power startup. The reactor eases into full heat production when first started, giving players a brief window to confirm everything is working before max stable temp. It also means a SCRAM gracefully winds the reactor down rather than snapping to zero.

**Meltdown grace period with SCRAM freeze** turns overheating into a recoverable situation rather than an instant loss. The timer scaling (longer grace when barely over the threshold, shorter when severely over) rewards players who catch problems early, and SCRAM freezing the countdown gives them a genuine chance to fix the issue. Aka high reward high risk loveliness.

**Burn multiplier (continuous run bonus)** gives players a long-term goal to work toward. Keeping a reactor running continuously builds up a heat and EU production bonus over time, rewarding stable designs and punishing constant restarts.

### Optional Mechanics

**Fractional fuel accumulation** (the per-type remainder buffer in `consumeFuelTick`) ensures fuel consumption stays accurate even when the per-tick rate rounds to zero. It's necessary for correctness at low consumption rates, but players never see it.

**`coolantUsageAdditive` mode** controls whether every active cooler type contributes its fluid demand independently or only the primary cooler is processed. The additive mode is more realistic and rewards installing diverse cooler blocks, but non-additive mode is considerably simpler to balance and design around.

**Danger bonus in EU generation** (`dangerBonus = 1 + curve^exponent * 1.5`) means the reactor produces substantially more EU as heat approaches `maxSafeHeat`. This creates an interesting risk/reward tradeoff for players willing to run hot, but if the pack wants straightforward linear EU output this curve exponent can simply be set to 0.

**`excessHeatSeverity` and grace period tuning** control how quickly the meltdown countdown accelerates as heat climbs beyond the safe threshold. The mechanic works fine at its default values, this is purely a balancing place.