## Example Gameplay Session

This document shows a typical gameplay session with the AI RPG Vibe application.

### Starting the Game

```
Welcome to AI RPG Vibe!

Click 'New Game' to start your adventure.

[User clicks "New Game"]

==================================================
NEW GAME STARTED
==================================================

The sun rises over the Village Square, casting long shadows across 
the cobblestone streets. You are Adventurer, a warrior of considerable 
strength, and destiny has called upon you. Your quest: Slay the Dragon 
of Mount Doom. The villagers speak in hushed tones of the great beast 
that has been terrorizing the countryside for months.

Your stats:
- HP: 140/140
- Mana: 50/50
- Strength: 16, Intelligence: 10, Agility: 12

Type 'help' to see available commands.
```

### Exploring the World

```
> location

The Village Square bustles with morning activity. Merchants set up 
their stalls, guards patrol the perimeter, and an old sage sits by 
the fountain, observing the world with ancient eyes. To the north, 
you can see the path leading to the Dark Forest. To the east, the 
road winds toward the mountains.
```

### Talking to NPCs

```
> talk I need information about the dragon

Old Sage Marcus: "Ah, seeking the great wyrm, are you? The Dragon 
of Mount Doom is no ordinary beast. You'll need more than just 
strength - gather allies, seek powerful artifacts, and learn its 
weaknesses. I sense great darkness in the mountain caves..."
```

### Testing Combat

```
> combat test

Combat begins!
A Goblin Scout leaps from the shadows, brandishing a rusty blade! 
Its eyes gleam with malice as it circles you warily.

> attack

--- Turn 1 ---

Adventurer charges forward with a powerful overhead strike! 
(Roll: 18 + 3 = 21 vs Defense 12)
The blade connects solidly, cleaving through the goblin's leather 
armor for 9 damage! The goblin stumbles backward, blood seeping 
from the wound.

The Goblin Scout retaliates with a desperate slash! 
(Roll: 8 + 1 = 9 vs Defense 12)
The attack goes wide, missing completely as you sidestep.

> attack

--- Turn 2 ---

Adventurer presses the advantage with a swift horizontal cut! 
(Roll: 15 + 3 = 18 vs Defense 12)
Another hit! 7 damage dealt. The goblin's movements become sluggish 
as its life force wanes.

The Goblin Scout attempts one final strike in desperation! 
(Roll: 6 + 1 = 7 vs Defense 12)
The feeble attack is easily parried.

> attack

--- Turn 3 ---

Adventurer delivers the finishing blow with a powerful thrust! 
(Roll: 19 + 3 = 22 vs Defense 12)
The blade pierces true for 8 damage! The goblin collapses, defeated.

VICTORY!

The goblin falls motionless at your feet. You search the body and 
find a few copper coins. Your victory here is but a small step on 
the long road ahead.

Experience gained: 50 XP
```

### Checking Stats

```
> stats

=== Adventurer (Level 1 Warrior) ===

Health: 140/140
Mana: 50/50

Attributes:
- Strength: 16 (modifier: +3)
- Intelligence: 10 (modifier: +0)
- Agility: 12 (modifier: +1)
- Constitution: 14 (modifier: +2)
- Charisma: 10 (modifier: +0)

Combat Stats:
- Defense: 13
- Weapon Bonus: +2
- Armor Bonus: +0

Experience: 50 XP
```

### Natural Language Actions

```
> I search the village for rumors about companions who might join me

The World Narrator responds:

You make your way through the village, listening to conversations 
and asking questions. At the tavern, you hear tales of a skilled 
archer named Elena who's been looking for worthy causes. The 
blacksmith mentions a wandering cleric who recently arrived, 
offering healing services. Your reputation as a dragon-slayer-to-be 
is already spreading, and several adventurers seem interested in 
your quest.

> I approach the tavern to meet Elena

The tavern door swings open to reveal a dimly lit common room filled 
with the scent of ale and roasting meat. At a corner table, a hooded 
figure sits alone, methodically maintaining a elegant longbow. As you 
approach, she looks up - sharp green eyes studying you with the 
precision of a predator assessing prey. "You're the one going after 
the dragon," she states, not asks. "I'm Elena. And I'm in."

[In a full implementation, Elena would be added as a team member here]
```

### Viewing Quests

```
> quests

=== Quests ===

Main Quest: Slay the Dragon of Mount Doom

Active Quests:
- The Sage's Request: Bring back a sample of dragon scale for study
```

### UI Side Panels

Throughout the game, the right sidebar displays:

**Hero Stats Panel:**
```
Name: Adventurer
Class: Warrior
Level: 1
HP: 140/140
Mana: 50/50
STR: 16
INT: 10
AGI: 12
XP: 50
```

**Party Members Panel:**
```
• Elena (Archer)
  HP: 100/100
  Loyalty: 75/100

• Brother Marcus (Cleric)
  HP: 90/90
  Loyalty: 60/100
```

**Active Quests Panel:**
```
Main: Slay the Dragon of Mount Doom

Side Quests:
• The Sage's Request
• Lost Heirloom
```

---

This example demonstrates:
- AI-generated world descriptions
- Dynamic NPC dialogue
- D&D-style combat mechanics
- Natural language action processing
- Integrated quest and party management

The actual AI responses will vary each playthrough, creating a unique 
experience every time!
