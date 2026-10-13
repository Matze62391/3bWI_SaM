package net.pedroksl.ae2addonlib.client.config;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.pedroksl.ae2addonlib.config.ModConfigSpec;

/**
 * Lists all options of one or more {@link ModConfigSpec}s and lets the player change them. Replaces the configuration
 * screen that NeoForge generates automatically; addons open it through Mod Menu. The changed values are saved when
 * the screen is closed.
 */
public class ModConfigScreen extends Screen {
    private static final int VALUE_WIDGET_WIDTH = 120;
    private static final int RESET_BUTTON_WIDTH = 40;
    private static final int INVALID_TEXT_COLOR = 0xFFFF5555;
    private static final int VALID_TEXT_COLOR = 0xFFE0E0E0;

    /**
     * A group of options shown under a title.
     */
    public record Section(Component title, ModConfigSpec spec) {}

    private final Screen lastScreen;
    private final List<Section> sections;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private OptionList list;

    public ModConfigScreen(Screen lastScreen, Component title, List<Section> sections) {
        super(title);
        this.lastScreen = lastScreen;
        this.sections = List.copyOf(sections);
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);

        list = layout.addToContents(new OptionList(minecraft));
        for (var section : sections) {
            list.addSection(section.title(), section.spec());
        }

        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .width(200)
                .build());
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        if (list != null) {
            list.updateSize(width, layout);
        }
    }

    @Override
    public void onClose() {
        for (var section : sections) {
            section.spec().save();
        }
        minecraft.gui.setScreen(lastScreen);
    }

    /**
     * Turns "camelCase" and "snake_case" option names into readable labels.
     */
    public static String humanize(String name) {
        var result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_' || c == '-') {
                result.append(' ');
            } else if (Character.isUpperCase(c) && i > 0 && !Character.isUpperCase(name.charAt(i - 1))) {
                result.append(' ').append(c);
            } else {
                result.append(i == 0 ? Character.toUpperCase(c) : c);
            }
        }
        return result.toString();
    }

    private class OptionList extends ContainerObjectSelectionList<OptionList.Entry> {
        OptionList(Minecraft minecraft) {
            super(minecraft, ModConfigScreen.this.width, layout.getContentHeight(), layout.getHeaderHeight(), 24);
        }

        @Override
        public int getRowWidth() {
            return Math.min(400, width - 40);
        }

        void addSection(Component title, ModConfigSpec spec) {
            addEntry(new HeaderEntry(title.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE)));
            List<String> currentPath = null;
            for (var value : spec.getValues()) {
                if (!value.getPath().equals(currentPath)) {
                    currentPath = value.getPath();
                    if (!currentPath.isEmpty()) {
                        var sectionName = String.join(
                                " › ",
                                currentPath.stream().map(ModConfigScreen::humanize).toList());
                        addEntry(new HeaderEntry(Component.literal(sectionName).withStyle(ChatFormatting.YELLOW)));
                    }
                }
                addEntry(new OptionEntry(value));
            }
        }

        abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {}

        class HeaderEntry extends Entry {
            private final Component text;

            HeaderEntry(Component text) {
                this.text = text;
            }

            @Override
            public void extractContent(
                    GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.text(
                        minecraft.font,
                        text,
                        getContentXMiddle() - minecraft.font.width(text) / 2,
                        getContentBottom() - 12,
                        -1);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of();
            }
        }

        class OptionEntry extends Entry {
            private final ModConfigSpec.ConfigValue<?> value;
            private final Component label;
            private final AbstractWidget valueWidget;
            private final Button resetButton;
            private Runnable refresh = () -> {};

            OptionEntry(ModConfigSpec.ConfigValue<?> value) {
                this.value = value;
                this.label = Component.literal(humanize(value.getName()));
                this.valueWidget = createValueWidget(value);
                var comment = value.getComment();
                if (comment != null && !comment.isBlank()) {
                    valueWidget.setTooltip(Tooltip.create(Component.literal(comment)));
                }
                this.resetButton = Button.builder(Component.literal("Reset"), button -> {
                            resetToDefault(value);
                            refresh.run();
                        })
                        .width(RESET_BUTTON_WIDTH)
                        .build();
            }

            @SuppressWarnings({"unchecked", "rawtypes"})
            private static void resetToDefault(ModConfigSpec.ConfigValue value) {
                value.set(value.getDefault());
            }

            private AbstractWidget createValueWidget(ModConfigSpec.ConfigValue<?> value) {
                if (value instanceof ModConfigSpec.BooleanValue booleanValue) {
                    var button = Button.builder(onOff(booleanValue.get()), b -> {
                                booleanValue.set(!booleanValue.get());
                                b.setMessage(onOff(booleanValue.get()));
                            })
                            .width(VALUE_WIDGET_WIDTH)
                            .build();
                    refresh = () -> button.setMessage(onOff(booleanValue.get()));
                    return button;
                } else if (value instanceof ModConfigSpec.EnumValue<?> enumValue) {
                    return createEnumButton(enumValue);
                } else if (value instanceof ModConfigSpec.IntValue intValue) {
                    return createNumberBox(
                            String.valueOf(intValue.get()),
                            text -> {
                                var parsed = Integer.parseInt(text.trim());
                                if (parsed < intValue.getMin() || parsed > intValue.getMax()) {
                                    throw new NumberFormatException();
                                }
                                intValue.set(parsed);
                            },
                            () -> String.valueOf(intValue.get()));
                } else if (value instanceof ModConfigSpec.DoubleValue doubleValue) {
                    return createNumberBox(
                            String.valueOf(doubleValue.get()),
                            text -> {
                                var parsed = Double.parseDouble(text.trim());
                                if (!(parsed >= doubleValue.getMin() && parsed <= doubleValue.getMax())) {
                                    throw new NumberFormatException();
                                }
                                doubleValue.set(parsed);
                            },
                            () -> String.valueOf(doubleValue.get()));
                }
                throw new IllegalArgumentException("Unsupported config value " + value.getName());
            }

            private <T extends Enum<T>> AbstractWidget createEnumButton(ModConfigSpec.EnumValue<T> enumValue) {
                var constants = enumValue.getEnumClass().getEnumConstants();
                var button = Button.builder(enumName(enumValue.get()), b -> {
                            enumValue.set(constants[(enumValue.get().ordinal() + 1) % constants.length]);
                            b.setMessage(enumName(enumValue.get()));
                        })
                        .width(VALUE_WIDGET_WIDTH)
                        .build();
                refresh = () -> button.setMessage(enumName(enumValue.get()));
                return button;
            }

            private static Component onOff(boolean value) {
                return value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
            }

            private static Component enumName(Enum<?> value) {
                return Component.literal(humanize(value.name().toLowerCase(Locale.ROOT)));
            }

            private EditBox createNumberBox(
                    String initialValue, Consumer<String> applier, Supplier<String> currentValue) {
                var box = new EditBox(minecraft.font, VALUE_WIDGET_WIDTH, 20, label);
                box.setMaxLength(32);
                box.setValue(initialValue);
                box.setResponder(text -> {
                    try {
                        applier.accept(text);
                        box.setTextColor(VALID_TEXT_COLOR);
                    } catch (NumberFormatException e) {
                        box.setTextColor(INVALID_TEXT_COLOR);
                    }
                });
                refresh = () -> box.setValue(currentValue.get());
                return box;
            }

            @Override
            public void extractContent(
                    GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                int right = getContentRight();
                resetButton.setPosition(right - RESET_BUTTON_WIDTH, getContentY());
                resetButton.active = !value.get().equals(value.getDefault());
                resetButton.extractRenderState(graphics, mouseX, mouseY, a);
                valueWidget.setPosition(right - RESET_BUTTON_WIDTH - 4 - VALUE_WIDGET_WIDTH, getContentY());
                valueWidget.extractRenderState(graphics, mouseX, mouseY, a);
                var font = minecraft.font;
                var labelWidth = valueWidget.getX() - 6 - getContentX();
                var text = label.getString();
                if (font.width(text) > labelWidth) {
                    text = font.plainSubstrByWidth(text, labelWidth - font.width("...")) + "...";
                    if (hovered && mouseX < valueWidget.getX()) {
                        graphics.setTooltipForNextFrame(font, label, mouseX, mouseY);
                    }
                }
                graphics.text(font, text, getContentX(), getContentYMiddle() - 4, -1);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(valueWidget, resetButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(valueWidget, resetButton);
            }
        }
    }
}
