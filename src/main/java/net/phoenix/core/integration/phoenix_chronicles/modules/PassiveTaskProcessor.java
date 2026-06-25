package net.phoenix.core.integration.phoenix_chronicles.modules;

// PassiveTaskProcessor intentionally left empty.
// All passive tasks (XP, inventory checks, advancement checks) are evaluated
// natively via isCompletedFor() during QuestProgressTracker.onPlayerTick.
// Event-driven tasks (kill, craft, block, dimension) have dedicated event hooks in ChronicleEvents.
public class PassiveTaskProcessor {}
