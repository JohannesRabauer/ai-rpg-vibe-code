package com.airpg.ui;

import com.airpg.domain.GameState;
import com.airpg.domain.Hero;
import com.airpg.domain.TeamMember;
import com.airpg.services.GameEngine;
import com.airpg.services.GamePersistenceService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main Vaadin UI view for the AI RPG game.
 * Provides text-based interface for player input and game output.
 */
@Route("")
public class GameView extends VerticalLayout {
    
    @Inject
    private GameEngine gameEngine;
    
    // UI Components
    private final TextArea storyArea;
    private final TextField inputField;
    private final Button submitButton;
    private final ComboBox<String> actionDropdown;
    private final Button newGameButton;
    private final Button saveGameButton;
    private final Button loadGameButton;
    
    // Side panels
    private final VerticalLayout statsPanel;
    private final VerticalLayout teamPanel;
    private final VerticalLayout questPanel;
    
    // Game state tracking
    private boolean gameStarted = false;
    
    // Available quick actions
    private static final List<String> QUICK_ACTIONS = List.of(
            "Look around",
            "Check inventory",
            "Talk to nearby NPC",
            "Explore the area",
            "Rest and recover",
            "Check quest log",
            "Attack enemy",
            "Defend",
            "Use healing potion",
            "Cast spell"
    );
    
    public GameView() {
        // Configure main layout
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        // Title
        H1 title = new H1("AI RPG Vibe - Fantasy Adventure");
        add(title);
        
        // Main content area (story display + input)
        VerticalLayout mainContent = new VerticalLayout();
        mainContent.setWidth("70%");
        mainContent.setHeightFull();
        
        // Story/output area
        storyArea = new TextArea("Story");
        storyArea.setWidthFull();
        storyArea.setHeight("500px");
        storyArea.setReadOnly(true);
        storyArea.setValue("Welcome to AI RPG Vibe!\n\nClick 'New Game' to start your adventure.");
        
        // Action dropdown with free text support
        actionDropdown = new ComboBox<>("Quick Actions");
        actionDropdown.setItems(QUICK_ACTIONS);
        actionDropdown.setPlaceholder("Select action or type custom...");
        actionDropdown.setAllowCustomValue(true);
        actionDropdown.setWidthFull();
        actionDropdown.addCustomValueSetListener(e -> {
            String customValue = e.getDetail();
            if (customValue != null && !customValue.trim().isEmpty()) {
                handlePlayerAction(customValue.trim());
                actionDropdown.clear();
            }
        });
        actionDropdown.addValueChangeListener(e -> {
            if (e.getValue() != null && e.isFromClient()) {
                handlePlayerAction(e.getValue());
                actionDropdown.clear();
            }
        });
        
        // Input area
        HorizontalLayout inputLayout = new HorizontalLayout();
        inputLayout.setWidthFull();
        inputLayout.setAlignItems(FlexComponent.Alignment.END);
        
        inputField = new TextField("Custom Action");
        inputField.setPlaceholder("Or type your own action...");
        inputField.setWidthFull();
        inputField.addKeyPressListener(Key.ENTER, e -> handlePlayerInput());
        
        submitButton = new Button("Submit", e -> handlePlayerInput());
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        inputLayout.add(inputField, submitButton);
        inputLayout.setFlexGrow(1, inputField);
        
        // Game control buttons
        newGameButton = new Button("New Game", e -> showNewGameDialog());
        newGameButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        saveGameButton = new Button("Save Game", new Icon(VaadinIcon.DOWNLOAD), e -> showSaveGameDialog());
        saveGameButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        saveGameButton.setEnabled(false);

        loadGameButton = new Button("Load Game", new Icon(VaadinIcon.UPLOAD), e -> showLoadGameDialog());
        loadGameButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttonRow = new HorizontalLayout(newGameButton, saveGameButton, loadGameButton);
        buttonRow.setSpacing(true);

        mainContent.add(storyArea, actionDropdown, inputLayout, buttonRow);
        
        // Right sidebar (stats, team, quests)
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setWidth("30%");
        sidebar.setHeightFull();
        sidebar.setSpacing(true);
        
        // Stats Panel
        statsPanel = new VerticalLayout();
        statsPanel.setSpacing(false);
        statsPanel.setPadding(true);
        statsPanel.getStyle().set("border", "1px solid #ccc");
        statsPanel.getStyle().set("border-radius", "4px");
        H3 statsTitle = new H3("Hero Stats");
        statsPanel.add(statsTitle);
        
        // Team Panel
        teamPanel = new VerticalLayout();
        teamPanel.setSpacing(false);
        teamPanel.setPadding(true);
        teamPanel.getStyle().set("border", "1px solid #ccc");
        teamPanel.getStyle().set("border-radius", "4px");
        H3 teamTitle = new H3("Party Members");
        teamPanel.add(teamTitle);
        
        // Quest Panel
        questPanel = new VerticalLayout();
        questPanel.setSpacing(false);
        questPanel.setPadding(true);
        questPanel.getStyle().set("border", "1px solid #ccc");
        questPanel.getStyle().set("border-radius", "4px");
        H3 questTitle = new H3("Active Quests");
        questPanel.add(questTitle);
        
        sidebar.add(statsPanel, teamPanel, questPanel);
        
        // Combine main content and sidebar
        HorizontalLayout contentLayout = new HorizontalLayout(mainContent, sidebar);
        contentLayout.setSizeFull();
        contentLayout.setFlexGrow(2, mainContent);
        contentLayout.setFlexGrow(1, sidebar);
        
        add(contentLayout);
        
        // Disable game controls until game starts
        setGameControlsEnabled(false);
    }
    
    /**
     * Called after CDI injection is complete
     */
    @PostConstruct
    private void init() {
        updateSidePanels();
    }
    
    /**
     * Enable or disable game controls based on game state
     */
    private void setGameControlsEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        submitButton.setEnabled(enabled);
        actionDropdown.setEnabled(enabled);
        gameStarted = enabled;
        // Save button enabled when game is active and not in combat
        saveGameButton.setEnabled(enabled && !gameEngine.isInCombat());
    }
    
    /**
     * Show dialog for creating a new game with hero name and class selection
     */
    private void showNewGameDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create Your Hero");
        dialog.setCloseOnOutsideClick(false);
        dialog.setCloseOnEsc(false);
        dialog.setWidth("400px");
        
        VerticalLayout dialogContent = new VerticalLayout();
        dialogContent.setPadding(false);
        dialogContent.setSpacing(true);
        
        // Hero name input
        TextField nameField = new TextField("Hero Name");
        nameField.setWidthFull();
        nameField.setPlaceholder("Enter your hero's name...");
        nameField.setRequired(true);
        nameField.setRequiredIndicatorVisible(true);
        nameField.focus();
        
        // Class selection
        ComboBox<String> classSelect = new ComboBox<>("Character Class");
        classSelect.setItems("Warrior", "Mage", "Rogue", "Ranger", "Cleric");
        classSelect.setValue("Warrior");
        classSelect.setWidthFull();
        classSelect.setRequired(true);
        
        // Class descriptions
        Paragraph classDescription = new Paragraph("Warrior: Strong melee fighter with high health");
        classSelect.addValueChangeListener(e -> {
            String description = switch (e.getValue()) {
                case "Warrior" -> "Warrior: Strong melee fighter with high health";
                case "Mage" -> "Mage: Powerful spellcaster with high intelligence";
                case "Rogue" -> "Rogue: Agile fighter skilled in stealth and critical hits";
                case "Ranger" -> "Ranger: Balanced fighter with ranged combat abilities";
                case "Cleric" -> "Cleric: Support class with healing and protection spells";
                default -> "";
            };
            classDescription.setText(description);
        });
        
        dialogContent.add(nameField, classSelect, classDescription);
        dialog.add(dialogContent);
        
        // Start button
        Button startButton = new Button("Begin Adventure", e -> {
            String heroName = nameField.getValue().trim();
            String heroClass = classSelect.getValue();
            
            if (heroName.isEmpty()) {
                nameField.setInvalid(true);
                nameField.setErrorMessage("Please enter a name for your hero");
                return;
            }
            
            if (heroClass == null || heroClass.isEmpty()) {
                classSelect.setInvalid(true);
                classSelect.setErrorMessage("Please select a class");
                return;
            }
            
            dialog.close();
            startGame(heroName, heroClass);
        });
        startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        // Cancel button
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        // Allow Enter key to start game
        nameField.addKeyPressListener(Key.ENTER, e -> startButton.click());
        
        dialog.getFooter().add(cancelButton, startButton);
        dialog.open();
    }
    
    /**
     * Start a new game with the given hero details
     */
    private void startGame(String heroName, String heroClass) {
        // Clear previous story
        storyArea.setValue("");

        // Show header immediately
        appendToStory("=".repeat(50) + "\n");
        appendToStory("NEW GAME STARTED\n");
        appendToStory(String.format("Hero: %s the %s\n", heroName, heroClass));
        appendToStory("=".repeat(50) + "\n\n");

        // Start the game with streaming
        gameEngine.startNewGameStreaming(heroName, heroClass, new com.airpg.services.StreamingResponseHandler() {
            @Override
            public void onToken(String token) {
                // Update UI on UI thread with each token
                UI ui = getUI().orElse(null);
                if (ui != null) {
                    ui.access(() -> {
                        appendToStory(token);
                        ui.push();
                    });
                }
            }

            @Override
            public void onComplete(String fullResponse) {
                // Enable controls and update UI when streaming is complete
                UI ui = getUI().orElse(null);
                if (ui != null) {
                    ui.access(() -> {
                        appendToStory("\n");
                        updateSidePanels();
                        setGameControlsEnabled(true);
                        inputField.focus();
                        ui.push();
                    });
                }
            }

            @Override
            public void onError(Throwable error) {
                // Handle error and still enable controls
                UI ui = getUI().orElse(null);
                if (ui != null) {
                    ui.access(() -> {
                        appendToStory("\n[Error starting game: " + error.getMessage() + "]\n");
                        updateSidePanels();
                        setGameControlsEnabled(true);
                        inputField.focus();
                        ui.push();
                    });
                }
            }
        });
    }
    
    /**
     * Handle player action from dropdown selection
     */
    private void handlePlayerAction(String action) {
        if (!gameStarted || action == null || action.isEmpty()) {
            return;
        }
        
        // Disable input during processing
        setGameControlsEnabled(false);
        
        // Show player input
        appendToStory("\n> " + action + "\n\n");
        
        // Process input through game engine with streaming
        gameEngine.processPlayerInputStreaming(action, new com.airpg.services.StreamingResponseHandler() {
            @Override
            public void onToken(String token) {
                // Update UI on UI thread with each token
                UI ui = getUI().orElse(null);
                if (ui != null) {
                    ui.access(() -> {
                        appendToStory(token);
                        ui.push();
                    });
                }
            }
            
            @Override
            public void onComplete(String fullResponse) {
                // Re-enable controls and update UI when streaming is complete
                UI ui = getUI().orElse(null);
                if (ui != null) {
                    ui.access(() -> {
                        appendToStory("\n");
                        updateSidePanels();
                        setGameControlsEnabled(true);
                        inputField.focus();
                        ui.push();
                    });
                }
            }
            
            @Override
            public void onError(Throwable error) {
                // Handle error and re-enable controls
                UI ui = getUI().orElse(null);
                if (ui != null) {
                    ui.access(() -> {
                        appendToStory("\n[Error: " + error.getMessage() + "]\n");
                        updateSidePanels();
                        setGameControlsEnabled(true);
                        inputField.focus();
                        ui.push();
                    });
                }
            }
        });
    }
    
    /**
     * Handle player input from text field submission
     */
    private void handlePlayerInput() {
        String input = inputField.getValue().trim();
        
        if (input.isEmpty() || !gameStarted) {
            return;
        }
        
        // Clear input field first
        inputField.clear();
        
        // Process the action
        handlePlayerAction(input);
    }
    
    /**
     * Append text to story area and auto-scroll
     */
    private void appendToStory(String text) {
        String currentText = storyArea.getValue();
        storyArea.setValue(currentText + text);
        scrollToBottom();
    }
    
    /**
     * Scroll the story area to the bottom
     * Uses Shadow DOM-aware JavaScript for Vaadin TextArea
     */
    private void scrollToBottom() {
        storyArea.getElement().executeJs(
                "setTimeout(() => { " +
                "  if (this.inputElement) { " +
                "    this.inputElement.scrollTop = this.inputElement.scrollHeight; " +
                "  } else if (this.shadowRoot) { " +
                "    const textarea = this.shadowRoot.querySelector('textarea'); " +
                "    if (textarea) textarea.scrollTop = textarea.scrollHeight; " +
                "  } " +
                "}, 50)"
        );
    }
    
    /**
     * Update all side panels with current game state
     */
    private void updateSidePanels() {
        GameState state = gameEngine.getGameState();
        
        if (state == null || state.getHero() == null) {
            return;
        }
        
        updateStatsPanel(state.getHero());
        updateTeamPanel(state);
        updateQuestPanel(state);
    }
    
    /**
     * Update stats panel
     */
    private void updateStatsPanel(Hero hero) {
        // Clear existing stats (keep title)
        while (statsPanel.getComponentCount() > 1) {
            statsPanel.remove(statsPanel.getComponentAt(1));
        }
        
        statsPanel.add(
                new Paragraph(String.format("Name: %s", hero.getName())),
                new Paragraph(String.format("Class: %s", hero.getCharacterClass())),
                new Paragraph(String.format("Level: %d", hero.getLevel())),
                new Paragraph(String.format("HP: %d/%d", hero.getCurrentHealth(), hero.getMaxHealth())),
                new Paragraph(String.format("Mana: %d/%d", hero.getCurrentMana(), hero.getMaxMana())),
                new Paragraph(String.format("STR: %d", hero.getStrength())),
                new Paragraph(String.format("INT: %d", hero.getIntelligence())),
                new Paragraph(String.format("AGI: %d", hero.getAgility())),
                new Paragraph(String.format("XP: %d", hero.getExperience()))
        );
    }
    
    /**
     * Update team panel
     */
    private void updateTeamPanel(GameState state) {
        // Clear existing team members (keep title)
        while (teamPanel.getComponentCount() > 1) {
            teamPanel.remove(teamPanel.getComponentAt(1));
        }
        
        if (state.getTeamMembers().isEmpty()) {
            teamPanel.add(new Paragraph("None"));
        } else {
            for (TeamMember member : state.getTeamMembers()) {
                VerticalLayout memberLayout = new VerticalLayout();
                memberLayout.setSpacing(false);
                memberLayout.setPadding(false);
                memberLayout.add(
                        new Paragraph(String.format("• %s (%s)", member.getName(), member.getCharacterClass())),
                        new Paragraph(String.format("  HP: %d/%d", member.getCurrentHealth(), member.getMaxHealth())),
                        new Paragraph(String.format("  Loyalty: %d/100", member.getLoyalty()))
                );
                teamPanel.add(memberLayout);
            }
        }
    }
    
    /**
     * Update quest panel
     */
    private void updateQuestPanel(GameState state) {
        // Clear existing quests (keep title)
        while (questPanel.getComponentCount() > 1) {
            questPanel.remove(questPanel.getComponentAt(1));
        }

        questPanel.add(new Paragraph(String.format("Main: %s", state.getMainGoal())));

        if (!state.getActiveQuests().isEmpty()) {
            questPanel.add(new Paragraph("\nSide Quests:"));
            state.getActiveQuests().forEach(quest ->
                    questPanel.add(new Paragraph(String.format("• %s", quest.getTitle())))
            );
        }
    }

    /**
     * Show dialog for saving the game
     */
    private void showSaveGameDialog() {
        if (gameEngine.isInCombat()) {
            Notification.show("Cannot save during combat!", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Save Game");
        dialog.setWidth("400px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        TextField saveNameField = new TextField("Save Name");
        saveNameField.setWidthFull();
        saveNameField.setPlaceholder("Enter a name for this save...");
        saveNameField.setRequired(true);
        saveNameField.focus();

        // Generate default save name
        GameState state = gameEngine.getGameState();
        if (state != null && state.getHero() != null) {
            String defaultName = String.format("%s - %s",
                    state.getHero().getName(),
                    state.getCurrentLocation());
            saveNameField.setValue(defaultName);
        }

        content.add(saveNameField);
        dialog.add(content);

        Button saveButton = new Button("Save", e -> {
            String saveName = saveNameField.getValue().trim();
            if (saveName.isEmpty()) {
                saveNameField.setInvalid(true);
                saveNameField.setErrorMessage("Please enter a save name");
                return;
            }

            GamePersistenceService.SaveResult result = gameEngine.saveGame(saveName);
            dialog.close();

            if (result.success()) {
                Notification.show("Game saved successfully!", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                appendToStory("\n[Game saved: " + saveName + "]\n");
            } else {
                Notification.show("Save failed: " + result.message(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        saveNameField.addKeyPressListener(Key.ENTER, e -> saveButton.click());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    /**
     * Show dialog for loading a saved game
     */
    private void showLoadGameDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Load Game");
        dialog.setWidth("600px");
        dialog.setHeight("500px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setSizeFull();

        List<GamePersistenceService.SaveMetadata> saves = gameEngine.listSaves();

        if (saves.isEmpty()) {
            content.add(new Paragraph("No saved games found."));
        } else {
            Grid<GamePersistenceService.SaveMetadata> grid = new Grid<>();
            grid.setItems(saves);
            grid.setWidthFull();
            grid.setHeight("300px");

            grid.addColumn(GamePersistenceService.SaveMetadata::saveName)
                    .setHeader("Save Name")
                    .setFlexGrow(2);

            grid.addColumn(save -> String.format("%s (Lv.%d %s)",
                            save.heroName(), save.heroLevel(), save.heroClass()))
                    .setHeader("Hero")
                    .setFlexGrow(2);

            grid.addColumn(GamePersistenceService.SaveMetadata::location)
                    .setHeader("Location")
                    .setFlexGrow(1);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            grid.addColumn(save -> save.updatedAt().format(formatter))
                    .setHeader("Saved")
                    .setFlexGrow(1);

            // Delete button column
            grid.addComponentColumn(save -> {
                Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
                deleteBtn.addClickListener(e -> showDeleteConfirmation(save, grid, saves));
                return deleteBtn;
            }).setHeader("").setWidth("60px").setFlexGrow(0);

            // Load on double click
            grid.addItemDoubleClickListener(e -> {
                loadSelectedSave(e.getItem(), dialog);
            });

            Button loadButton = new Button("Load Selected", e -> {
                GamePersistenceService.SaveMetadata selected = grid.asSingleSelect().getValue();
                if (selected == null) {
                    Notification.show("Please select a save to load", 3000, Notification.Position.MIDDLE);
                    return;
                }
                loadSelectedSave(selected, dialog);
            });
            loadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            loadButton.setEnabled(false);

            grid.asSingleSelect().addValueChangeListener(e -> {
                loadButton.setEnabled(e.getValue() != null);
            });

            content.add(grid);
            dialog.getFooter().add(loadButton);
        }

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelButton);

        dialog.add(content);
        dialog.open();
    }

    /**
     * Show confirmation dialog before deleting a save
     */
    private void showDeleteConfirmation(GamePersistenceService.SaveMetadata save,
                                         Grid<GamePersistenceService.SaveMetadata> grid,
                                         List<GamePersistenceService.SaveMetadata> saves) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Delete Save?");
        confirmDialog.setText(String.format("Are you sure you want to delete \"%s\"? This cannot be undone.",
                save.saveName()));
        confirmDialog.setCancelable(true);
        confirmDialog.setConfirmText("Delete");
        confirmDialog.setConfirmButtonTheme("error primary");
        confirmDialog.addConfirmListener(e -> {
            gameEngine.deleteSave(save.id());
            saves.remove(save);
            grid.getDataProvider().refreshAll();
            Notification.show("Save deleted", 2000, Notification.Position.MIDDLE);
        });
        confirmDialog.open();
    }

    /**
     * Load the selected save and update UI
     */
    private void loadSelectedSave(GamePersistenceService.SaveMetadata save, Dialog dialog) {
        boolean loaded = gameEngine.loadGame(save.id());
        dialog.close();

        if (loaded) {
            // Clear story area and show loaded game info
            storyArea.setValue("");
            appendToStory("=".repeat(50) + "\n");
            appendToStory("GAME LOADED\n");
            appendToStory(String.format("Save: %s\n", save.saveName()));
            appendToStory(String.format("Hero: %s the %s (Level %d)\n",
                    save.heroName(), save.heroClass(), save.heroLevel()));
            appendToStory(String.format("Location: %s\n", save.location()));
            appendToStory("=".repeat(50) + "\n\n");
            appendToStory("Your adventure continues...\n\n");
            appendToStory("Type 'help' to see available commands.\n");

            updateSidePanels();
            setGameControlsEnabled(true);
            inputField.focus();

            Notification.show("Game loaded successfully!", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            Notification.show("Failed to load game", 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
