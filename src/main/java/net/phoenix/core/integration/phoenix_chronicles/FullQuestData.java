package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.network.chat.Component;

import java.util.List;

public record FullQuestData(Component title, Component description, List<String> tasks) {}
