# AI RPG Vibe - Custom Theme Documentation

## Overview
This custom Vaadin theme provides an immersive dark fantasy aesthetic with comprehensive styling for the AI RPG game interface. The theme features rich color palettes, smooth animations, and responsive design optimized for both desktop and mobile gameplay.

## Theme Structure

```
src/main/frontend/themes/airpg/
├── styles.css          # Main theme styles
├── comparison.css      # Model comparison UI styles (Codex vs Opus)
└── theme.json          # Theme configuration
```

## Color Palette

### Primary Colors
- **Primary Brown**: `#8b4513` - Main UI elements, borders
- **Primary Dark**: `#6b3410` - Darker accents
- **Primary Light**: `#a0522d` - Highlights
- **Secondary Green**: `#4a5d23` - Alternative accents
- **Accent Gold**: `#d4af37` - Important elements, headers
- **Accent Glow**: `#ffd700` - Highlights, hover states

### Background Colors
- **Main BG**: `#1a1410` - Main background
- **Surface**: `#2d2419` - Panel backgrounds
- **Panel**: `#3a2f1f` - Content panels
- **Elevated**: `#4a3d2a` - Raised elements

### Text Colors
- **Primary**: `#f5e6d3` - Main text
- **Secondary**: `#d4c4a8` - Secondary text
- **Muted**: `#9a8b73` - Disabled/muted text
- **Accent**: `#ffd700` - Highlighted text

### Status Colors
- **Health**: `#dc143c` - HP bars
- **Mana**: `#1e90ff` - Mana bars
- **Success**: `#4caf50` - Success states
- **Warning**: `#ff9800` - Warning states
- **Danger**: `#f44336` - Error/danger states

## Key Features

### 1. Enhanced Typography
- Fantasy-themed serif fonts (Georgia, Times New Roman)
- Gradient text effects for titles
- Uppercase headers with letter-spacing
- Icon integration (emoji-based)

### 2. Visual Components

#### Stat Bars
Visual progress bars for HP, Mana, and other stats with smooth animations:
```java
HorizontalLayout healthBar = createStatBar("❤ HP", current, max, "var(--airpg-health)");
```

#### Panels
Three main side panels with enhanced styling:
- **Stats Panel**: Character statistics with visual bars
- **Team Panel**: Companion information with loyalty indicators
- **Quest Panel**: Main and side quest tracking

#### Combat Indicator
Animated indicator when in combat with glowing effect.

### 3. Button Themes

- **Primary** (Gold gradient): Main actions
- **Contrast** (Brown): New game, major actions
- **Success** (Green): Save game
- **Tertiary** (Outlined gold): Load game
- **Error** (Red): Delete actions

### 4. Dialog Enhancement
- Fantasy-themed borders
- Gradient headers
- Glowing shadows

### 5. Animations

#### Fade In
Applied to main content on load:
```css
@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}
```

#### Glow
Used for combat indicator:
```css
@keyframes glow {
    0%, 100% { box-shadow: var(--airpg-shadow-md); }
    50% { box-shadow: var(--airpg-shadow-lg), var(--airpg-shadow-glow); }
}
```

## Model Comparison Features (comparison.css)

### Purpose
Provides styling for comparing outputs from different AI models (GPT Codex vs Claude Opus 4.6).

### Components

#### 1. Model Themes
- **Codex Theme**: Blue/Tech aesthetic
- **Opus Theme**: Purple/Premium aesthetic

#### 2. Comparison Container
Side-by-side panels with shimmer effects:
```html
<div class="comparison-container">
    <div class="comparison-panel model-codex">...</div>
    <div class="comparison-panel model-opus">...</div>
</div>
```

#### 3. Performance Metrics
Grid-based metric cards showing:
- Response time
- Token count
- Quality score
- Accuracy metrics

#### 4. Quality Indicators
Visual bars showing response quality with gradient fills.

#### 5. Diff View
Line-by-line comparison with:
- Green highlights for additions
- Red highlights for removals
- Muted unchanged lines

#### 6. Winner Badge
Golden crown badge for the better-performing model.

## Usage in Java Components

### Applying Styles in GameView.java

```java
// Add CSS class
component.addClassName("model-codex");

// Inline styles with CSS variables
component.getStyle()
    .set("background", "var(--airpg-bg-panel)")
    .set("border", "2px solid var(--airpg-primary)")
    .set("border-radius", "var(--airpg-border-radius)");
```

### Creating Stat Bars
```java
HorizontalLayout healthBar = createStatBar("❤ HP", 
    hero.getCurrentHealth(), 
    hero.getMaxHealth(), 
    "var(--airpg-health)");
```

### Using Icons
Emoji-based icons for visual enhancement:
- `⚔` - Strength/Attack
- `📖` - Intelligence/Magic
- `⚡` - Agility/Speed
- `❤` - Health
- `✦` - Mana
- `🗡` - Team members
- `🎯` - Main quest
- `📜` - Side quests
- `⭐` - Level
- `✨` - Experience

## Responsive Design

The theme automatically adapts for smaller screens:

```css
@media (max-width: 768px) {
    /* Single column layout */
    /* Smaller title */
    /* Full-width panels */
}
```

## Customization

### Changing Color Scheme
Edit the `:root` variables in `styles.css`:

```css
:root {
    --airpg-primary: #your-color;
    --airpg-accent: #your-color;
    /* ... */
}
```

### Adding New Themes
1. Define color variables
2. Create CSS classes
3. Apply to components in Java

### Animation Adjustments
Modify animation timing in CSS:
```css
.comparison-panel::before {
    animation: shimmer 3s infinite; /* Change duration */
}
```

## Browser Compatibility
- Chrome/Edge: Full support
- Firefox: Full support
- Safari: Full support (with webkit prefixes)

## Performance Considerations
- CSS animations use GPU acceleration
- Shadow DOM optimization for Vaadin components
- Minimal JavaScript (auto-scroll only)
- Lazy loading for dialogs and grids

## Future Enhancements
- Light theme variant
- Additional color schemes (fire, ice, nature)
- More animation effects
- Sound effect integration hooks
- Mobile-specific optimizations

## Credits
Theme designed for AI RPG Vibe - A Quarkus/Vaadin/LangChain4j fantasy RPG game.
