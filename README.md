# SLML - Screen Layout Markup Language

> Documentation in this project was made using a local LLM, meaning it's likely incorrect at times, and might be a bit wordy.
> I'm sorry in advance; it takes way too long to design, develop, and test this code.
> Documentation on top of that, besides inline comments, is low on the priority list.
> 
> At some point in the future, these docs may be updated manually.
> They have been, however, manually reviewed for accuracy of implementation examples.

SLML (Screen Layout Markup Language) is a Minecraft modding library designed to simplify the creation of complex, dynamic user interfaces. Instead of hardcoding UI elements in Java, SLML allows developers to define their layouts in XML files, providing a much more intuitive and flexible workflow.

## Key Features

* **XML-based Layouts:** Define your UI structure, positioning, and styling in XML.
* **Dynamic Placeholders:** Easily inject dynamic data (like player names, item counts, etc.) into your UI using the `{{placeholder}}` syntax.
* **Powerful UI Builder:** A robust Java API for managing and interacting with your UI elements.
* **NeoForge Compatible:** Built specifically for modern Minecraft modding environments.

## Implementation Example

### 1. Create an XML Layout

Place your layout file in `src/main/resources/data/<your_namespace>/ui/<layout_name>.xml`.

```xml
<!-- example: src/main/resources/data/mymod/ui/my_screen.xml -->
<Container width="200" height="100" x="50" y="50">
    <Text id="title" text="Hello, {{player_name}}!" x="10" y="10" />
    <Button id="close_button" text="Close" x="10" y="50" width="80" height="20" />
</Container>
```

### 2. Implement the Custom Screen in Java

You can use the `CustomScreen` class to load and display your XML layout.

```java
import com.github.syren_dev_tech.slml.CustomScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

// ... inside your client-side code (e.g., an event handler or command) ...

public void openMyCustomScreen(Player player) {
    // 1. Instantiate the CustomScreen
    // Parameters: (Player, Namespace, Layout Name)
    CustomScreen myScreen = new CustomScreen(player, "mymod", "my_screen");

    // 2. Define dynamic placeholders
    // These will replace {{player_name}} in your XML
    myScreen.definePlaceholder("player_name", player.getName().getString());

    // 3. Optional: Set callbacks
    myScreen.setOnInitCallback(uiBuilder -> {
        // This runs after the layout is loaded into the UIBuilder
        // You can use uiBuilder to find elements by ID and modify them
        return true;
    });

    myScreen.setOnCloseCallback(minecraft -> {
        // This runs when the screen is closed
        return true;
    });

    // 4. Open the screen
    Minecraft.getInstance().setScreen(myScreen);
}
```

## Getting Started

1. Add SLML as a dependency to your `build.gradle`.
2. Create your XML layout files in your mod's data folder.
3. Use `CustomScreen` to display your layouts.
