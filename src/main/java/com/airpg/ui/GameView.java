package com.airpg.ui;

import com.airpg.domain.GameState;
import com.airpg.domain.Hero;
import com.airpg.domain.TeamMember;
import com.airpg.services.GameEngine;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

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
    
    // Side panels
    private final VerticalLayout statsPanel;
    private final VerticalLayout teamPanel;
    private final VerticalLayout questPanel;
    
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
        
        // Input area
        HorizontalLayout inputLayout = new HorizontalLayout();
        inputLayout.setWidthFull();
        
        inputField = new TextField();
        inputField.setPlaceholder("Enter your action...");
        inputField.setWidthFull();
        inputField.addKeyPressListener(Key.ENTER, e -> handlePlayerInput());
        
        submitButton = new Button("Submit", e -> handlePlayerInput());
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        inputLayout.add(inputField, submitButton);
        inputLayout.setFlexGrow(1, inputField);
        
        // New Game button
        Button newGameButton = new Button("New Game", e -> showNewGameDialog());
        newGameButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        
        mainContent.add(storyArea, inputLayout, newGameButton);
        
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
    }
    
    /**
     * Called after CDI injection is complete
     */
    @PostConstruct
    private void init() {
        updateSidePanels();
    }
    
    /**
     * Show dialog for creating a new game
     */
    private void showNewGameDialog() {
        // Simplified: Start game with default values
        // In a full implementation, you'd show a dialog for name/class selection
        String heroName = "Adventurer";
        String heroClass = "Warrior";
        
        String gameStart = gameEngine.startNewGame(heroName, heroClass);
        appendToStory("\n" + "=".repeat(50) + "\n");
        appendToStory("NEW GAME STARTED\n");
        appendToStory("=".repeat(50) + "\n\n");
        appendToStory(gameStart);
        
        updateSidePanels();
    }
    
    /**
     * Handle player input submission
     */
    private void handlePlayerInput() {
        String input = inputField.getValue().trim();
        
        if (input.isEmpty()) {
            return;
        }
        
        // Show player input
        appendToStory("\n> " + input + "\n\n");
        
        // Process input through game engine
        String response = gameEngine.processPlayerInput(input);
        appendToStory(response + "\n");
        
        // Clear input field
        inputField.clear();
        inputField.focus();
        
        // Update side panels
        updateSidePanels();
    }
    
    /**
     * Append text to story area and auto-scroll
     */
    private void appendToStory(String text) {
        String currentText = storyArea.getValue();
        storyArea.setValue(currentText + text);
        
        // Auto-scroll to bottom (workaround for Vaadin)
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "arguments[0].scrollTop = arguments[0].scrollHeight",
                storyArea.getElement()
        ));
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
}
