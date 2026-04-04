# Design System Strategy: The Adaptive Scholar

## 1. Overview & Creative North Star
**Creative North Star: "The Cognitive Architect"**

This design system moves away from the "learning management system" cliché of rigid grids and heavy borders. Instead, it treats the interface as a sophisticated, living environment that adapts to the learner’s cognitive state. We are building a "Cognitive Architect"—an experience that feels both editorially curated and technologically advanced. 

To break the "template" look, we utilize **Asymmetric Information Density**. Important study metrics are presented with grand, display-scale typography, while secondary navigation and meta-data are tucked into high-chroma, minimal labels. We use overlapping elements—such as language toggles that float over content boundaries—to create a sense of depth and fluidity, signaling that the platform is "smart" enough to handle global context effortlessly.

## 2. Colors: Tonal Depth & The "No-Line" Rule

Our palette is rooted in professional blues and architectural grays, moving beyond flat layouts into a world of layered light.

### The "No-Line" Rule
**Explicit Instruction:** 1px solid borders are prohibited for sectioning. Structural boundaries must be defined solely through background color shifts or subtle tonal transitions.
*   **Implementation:** A `surface-container-low` sidebar sitting against a `surface` main content area provides all the separation required. 

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers. Use the `surface-container` tiers to create a "stacked" hierarchy:
*   **Base Layer:** `surface` (#f7f9fb) for the primary application canvas.
*   **Intermediate Layer:** `surface-container-low` (#f2f4f6) for secondary modules or sidebar navigation.
*   **Top Layer (Interactive):** `surface-container-lowest` (#ffffff) for the most critical study cards or active test questions.

### The "Glass & Gradient" Rule
To evoke a "smart" and premium feel, use Glassmorphism for floating UI elements like the **Language Switcher**. 
*   Apply `surface-container-lowest` at 80% opacity with a `24px` backdrop-blur. 
*   **Signature Textures:** Use a subtle linear gradient from `primary` (#4d556a) to `primary-container` (#656d84) for primary CTA backgrounds. This adds a "brushed steel" professional finish that flat color cannot replicate.

## 3. Typography: The Editorial Edge

We pair the geometric precision of **Plus Jakarta Sans** with the functional clarity of **Manrope**.

*   **Display & Headline (Plus Jakarta Sans):** These are your "Editorial Voices." Use `display-lg` for progress percentages and `headline-sm` for lesson titles. The tight tracking and modern apertures of Plus Jakarta Sans lend a "high-tech" authority to the TOEIC content.
*   **Body & Title (Manrope):** These are your "Functional Voices." Manrope is used for the core learning content (reading passages, multiple-choice options). The slightly taller x-height ensures readability during long study sessions.
*   **Language Labels:** Use `label-md` in All-Caps with `0.05em` letter spacing for language codes (e.g., EN, JP, KR). This transforms simple metadata into a premium design element.

## 4. Elevation & Depth: Atmospheric Layering

Hierarchy is achieved through light and layering, not structural lines.

*   **The Layering Principle:** Place a `surface-container-lowest` card on top of a `surface-container-low` section. This creates a soft, natural lift that mimics fine paper stacked on a desk.
*   **Ambient Shadows:** For floating elements (like language dropdowns), use "Atmospheric Shadows":
    *   `blur: 40px`, `spread: -5px`, `opacity: 6%`.
    *   **Color:** Use a tinted shadow based on `on-surface` (#191c1e) to ensure the shadow feels like a natural obstruction of light.
*   **Ghost Borders:** If a boundary is required for accessibility (e.g., in high-contrast modes), use a "Ghost Border": the `outline-variant` (#c3c6d7) at **15% opacity**. Never use 100% opaque outlines.

## 5. Components

### Language Switcher (The Signature Component)
*   **Visuals:** A "Pill" toggle using `surface-container-high`. The active language is housed in a `surface-container-lowest` glassmorphic chip that slides horizontally with a spring animation.
*   **Imagery:** Use circular, high-fidelity flag icons or minimalist ISO codes. When inactive, the language code should use `on-surface-variant`.

### Progress & Scoring Cards
*   **Layout:** Forbid dividers. Use `surface-container-highest` for the background of "Current Score" and `surface-container-low` for "Target Score" to create immediate visual priority.
*   **Corner Radius:** Use `xl` (1.5rem) for main dashboard cards and `md` (0.75rem) for inner interactive elements to create a nested, organic feel.

### Interactive Inputs (TOEIC Practice)
*   **Multiple Choice:** Forbid checkboxes. Use large-format `surface-container-lowest` tiles. On hover, transition to `secondary-fixed` (#d8e2ff) with a `2px` "Ghost Border."
*   **Text Inputs:** Use a "Minimalist Underline" style where the input is a solid block of `surface-container-low` with a 2px `primary` bottom-border that grows from the center only when focused.

### Action Chips
*   **Variants:** Use `tertiary` (#006242) for "Correct" feedback and `error` (#ba1a1a) for "Incorrect." These chips should be semi-transparent with a 10% fill and 100% opaque text to maintain the sophisticated, non-jarring aesthetic.

## 6. Do's and Don'ts

### Do
*   **Do** use asymmetrical spacing. Allow more "breathing room" (32px+) on the left side of headlines to create an editorial feel.
*   **Do** use `secondary` (#0058be) as your primary "Action" color to signal intelligence and trustworthiness.
*   **Do** allow elements to overlap (e.g., a "Time Remaining" chip floating half-on, half-off the main exam container).

### Don't
*   **Don't** use 1px dividers between list items. Use 12px of vertical `surface` space instead.
*   **Don't** use pure black (#000000). Always use `on-surface` (#191c1e) for text to maintain the soft-professional palette.
*   **Don't** use default browser focus states. Use a soft `primary-fixed` glow to maintain the "smart" interface identity.