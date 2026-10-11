package com.glodblock.github.extendedae.client.gui.config;

import com.glodblock.github.extendedae.config.EAEConfig;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Lists ExtendedAE's options ({@code config/extendedae.json}) and lets the player change them. Replaces the config
 * screen that NeoForge generates automatically; it is opened through Mod Menu. Numbers are typed in, lists are
 * comma separated. The values are saved when the screen is closed.
 */
public class EAEConfigScreen extends Screen {
    private static final int VALUE_WIDGET_WIDTH = 150;
    private static final int RESET_BUTTON_WIDTH = 40;
    private static final int INVALID_TEXT_COLOR = 0xFFFF5555;
    private static final int VALID_TEXT_COLOR = 0xFFE0E0E0;

    @Nullable
    private final Screen lastScreen;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private OptionList list;

    public EAEConfigScreen(@Nullable Screen lastScreen) {
        super(Component.literal("ExtendedAE"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);
        list = layout.addToContents(new OptionList(minecraft));
        // Grouped by section, like in the config file
        var sections = new LinkedHashMap<String, List<EAEConfig.Entry>>();
        for (var entry : EAEConfig.entries()) {
            var section = entry.name().substring(0, entry.name().indexOf('.'));
            sections.computeIfAbsent(section, k -> new ArrayList<>()).add(entry);
        }
        sections.forEach((section, entries) -> {
            list.addHeader(Component.literal(humanize(section)).withStyle(ChatFormatting.YELLOW));
            entries.forEach(list::addOption);
        });
        list.addHeader(Component.literal("Some options only apply after a restart")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
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
        minecraft.gui.setScreen(lastScreen);
    }

    @Override
    public void removed() {
        // Saved whenever the screen goes away, also when another screen replaces it
        EAEConfig.save();
    }

    /**
     * Turns "snake_case" option names into readable labels.
     */
    static String humanize(String name) {
        var text = name.replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private class OptionList extends ContainerObjectSelectionList<OptionList.Entry> {
        OptionList(Minecraft minecraft) {
            super(minecraft, EAEConfigScreen.this.width, layout.getContentHeight(), layout.getHeaderHeight(), 24);
        }

        @Override
        public int getRowWidth() {
            return Math.min(440, width - 40);
        }

        void addHeader(Component text) {
            addEntry(new HeaderEntry(text));
        }

        void addOption(EAEConfig.Entry option) {
            addEntry(new OptionEntry(option));
        }

        abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        }

        class HeaderEntry extends Entry {
            private final Component text;

            HeaderEntry(Component text) {
                this.text = text;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered,
                    float a) {
                graphics.text(minecraft.font, text, getContentXMiddle() - minecraft.font.width(text) / 2,
                        getContentBottom() - 12, -1);
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
            private final EAEConfig.Entry option;
            private final Component label;
            private final AbstractWidget valueWidget;
            private final Button resetButton;
            private Runnable refresh = () -> {
            };

            OptionEntry(EAEConfig.Entry option) {
                this.option = option;
                var key = option.name().substring(option.name().indexOf('.') + 1);
                this.label = Component.literal(humanize(key));
                this.valueWidget = option.kind() == EAEConfig.Kind.BOOLEAN ? createToggle() : createTextBox();
                this.valueWidget.setTooltip(Tooltip.create(Component.literal(option.comment())));
                this.resetButton = Button.builder(Component.literal("Reset"), button -> {
                    option.resetToDefault();
                    refresh.run();
                }).width(RESET_BUTTON_WIDTH).build();
            }

            private AbstractWidget createToggle() {
                var button = Button.builder(onOff(), b -> {
                    option.setFromText(String.valueOf(!Boolean.parseBoolean(option.valueAsText())));
                    b.setMessage(onOff());
                }).width(VALUE_WIDGET_WIDTH).build();
                refresh = () -> button.setMessage(onOff());
                return button;
            }

            private Component onOff() {
                return Boolean.parseBoolean(option.valueAsText()) ? CommonComponents.OPTION_ON
                        : CommonComponents.OPTION_OFF;
            }

            private EditBox createTextBox() {
                var box = new EditBox(minecraft.font, VALUE_WIDGET_WIDTH, 20, label);
                box.setMaxLength(1024);
                box.setValue(option.valueAsText());
                box.setResponder(text -> {
                    try {
                        option.setFromText(text);
                        box.setTextColor(VALID_TEXT_COLOR);
                    } catch (IllegalArgumentException e) {
                        box.setTextColor(INVALID_TEXT_COLOR);
                    }
                });
                refresh = () -> box.setValue(option.valueAsText());
                return box;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered,
                    float a) {
                int right = getContentRight();
                resetButton.setPosition(right - RESET_BUTTON_WIDTH, getContentY());
                resetButton.active = !option.isDefault();
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
