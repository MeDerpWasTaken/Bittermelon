package com.site21.bittermelon.common.content.entities.scp939.lure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.site21.bittermelon.common.systems.character.Character;
import com.site21.bittermelon.common.systems.character.CharacterUtil;
import com.site21.bittermelon.util.LocalMessageUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LureSystem {
    public static final Codec<LureSystem> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.list(UUIDUtil.CODEC)
                            .fieldOf("characters")
                            .forGetter(LureSystem::getCharacters),
                    Codec.LONG
                            .fieldOf("lastScene")
                            .forGetter(LureSystem::getLastScene)
            ).apply(instance, LureSystem::new)
    );

    private static final List<FakeCharacter> FALLBACK_CHARACTERS = List.of(
            new FakeCharacter("John", 0xFF0000),
            new FakeCharacter("Bob", 0x00FF00),
            new FakeCharacter("Jim", 0x0000FF),
            new FakeCharacter("Tim", 0xFFFF00)
    );

    private static final float SOUND_CHANCE = 0.1f;
    private static final int COOLDOWN_TICKS = 4000;

    private static final Map<LureType, List<LurePool>> pools;
    private final List<UUID> characters;
    private @Nullable LureScene activeScene;
    private long lastLure = 0;
    private int interval = 0;
    private long lastScene = 0;

    public LureSystem(List<UUID> characters, long lastScene) {
        this.characters = new ArrayList<>(characters);
        this.lastScene = lastScene;
    }

    public LureSystem() {
        characters = new ArrayList<>();
    }

    public LureScene createScene(Entity entity, LureType type) {
        RandomSource random = entity.getRandom();
        List<LurePool> poolList = pools.getOrDefault(type, pools.get(LureType.GENERIC));
        LurePool pool = poolList.get(random.nextInt(poolList.size()));
        List<LureDialogue> lines = new ArrayList<>();

        for (int i = 0; i < pool.dialogue().length; i++) {
            LureDialogue dialogue;
            List<String> messages = new ArrayList<>(Arrays.asList(pool.dialogue()[i]));
            if (characters.isEmpty()) {
                dialogue = getFallbackDialogue(random, messages);
            } else {
                dialogue = getDialogueForCharacter(random, messages);
            }
            lines.add(dialogue);
        }
        return new LureScene(type, lines, pool);
    }

    private LureDialogue getDialogueForCharacter(RandomSource random, List<String> messages) {
        UUID uuid = characters.get(random.nextInt(characters.size()));
        Character character = CharacterUtil.getCharacter(null, uuid);
        if (character == null) {
            return getFallbackDialogue(random, messages);
        }
        return new LureDialogue(character.getName(), character.getEmoteColor(), messages);
    }

    private LureDialogue getFallbackDialogue(RandomSource random, List<String> messages) {
        FakeCharacter character = FALLBACK_CHARACTERS.get(random.nextInt(FALLBACK_CHARACTERS.size()));
        return new LureDialogue(character.name(), character.color(), messages);
    }

    public void attemptLure(Entity entity, LureType type) {
        if (activeScene == null || activeScene.type() != type) {
            activeScene = createScene(entity, type);
            if (activeScene == null) return;
        }

        Level level = entity.level();
        LurePool pool = activeScene.pool();
        if (level.getGameTime() - lastLure < interval) return;

        RandomSource random = entity.getRandom();
        if (random.nextFloat() < SOUND_CHANCE && pool.sounds().length > 0) {
            playRandomSound(entity, random, pool);
        } else {
            makeRandomLure(entity, random);
        }

        lastLure = level.getGameTime();
        interval = pool.interval() + random.nextInt(Math.max(1, pool.additionalRandomInterval()));
    }

    private static void playRandomSound(Entity entity, RandomSource random, LurePool pool) {
        SoundEvent[] sounds = pool.sounds();
        SoundEvent sound = sounds[random.nextInt(sounds.length)];
        entity.playSound(sound);
    }

    private void makeRandomLure(Entity entity, RandomSource random) {
        if (activeScene == null) return;
        int i = random.nextInt(activeScene.lines().size());
        LureDialogue dialogue = activeScene.lines().get(i);
        String message = dialogue.messages().remove(random.nextInt(dialogue.messages().size()));

        Component component = Component.literal(dialogue.characterName() + " says, ")
                .withColor(dialogue.emoteColor())
                .append(Component.literal("\"" + message + "\"").withStyle(ChatFormatting.WHITE));
        LocalMessageUtil.sendLocalMessage(entity, 16, component);

        if (dialogue.messages().isEmpty()) {
            activeScene.lines().remove(i);
        }

        activeScene.incrementBeat();

        if (activeScene.isFinished()) {
            activeScene = null;
            lastScene = entity.level().getGameTime();
        }
    }

    public long getLastScene() {
        return lastScene;
    }

    public List<UUID> getCharacters() {
        return characters;
    }

    public boolean isOnCooldown(Level level) {
        return level.getGameTime() - lastScene < COOLDOWN_TICKS;
    }

    static {
        pools = new HashMap<>();
        pools.put(LureType.GENERIC, List.of(
                new LurePool(
                        new String[][]{
                                {"Holy fuck?!", "WHAT IS THAT?!!", "Be careful!!"},
                                {"I heard something behind us!", "WAIT!", "It's coming from over there!"}
                        },
                        new SoundEvent[]{
                        },
                        100,
                        50,
                        6
                ),
                new LurePool(
                        new String[][]{
                                {"Is anyone out there?", "Hello?", "I heard that."}
                        },
                        new SoundEvent[]{
                        },
                        100,
                        50,
                        3
                ),
                new LurePool(
                        new String[][]{
                                {"I'm entering a new section.", "Goddamn, it's dark here.", "I don't like that sound.", "Yeah.", "No.", "What's the status?", "Copy that."}
                        },
                        new SoundEvent[]{
                        },
                        200,
                        200,
                        5
                )
        ));
    }

    private record FakeCharacter(String name, int color) {
    }
}
