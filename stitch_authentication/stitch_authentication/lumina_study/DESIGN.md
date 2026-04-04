# Design System Strategy: The Cognitive Sanctuary

This design system is built to transform the often-stressful experience of TOEIC preparation into a focused, high-end "Cognitive Sanctuary." We are moving away from the cluttered, "gamified" aesthetics of traditional learning apps toward a sophisticated editorial experience. 

The goal is to provide a sense of calm authority through **Soft Minimalism**. We achieve this by replacing rigid structural lines with tonal layering, utilizing expansive white space, and employing a high-contrast typography scale that feels more like a premium magazine than a utility tool.

---

## 1. Creative North Star: "The Fluid Scholar"
The "Fluid Scholar" principle dictates that the UI should never feel static or boxed-in. By using large-scale radii and avoiding harsh borders, the interface feels organic and approachable. We use **intentional asymmetry**—such as off-center typography in hero sections and overlapping flashcard stacks—to break the "template" look and provide a bespoke, curated feel.

---

## 2. Color & Surface Architecture
We rely on the Material Design 3 logic of tonal surfaces. The core of this system is the **"No-Line" Rule**: 1px solid borders are strictly prohibited for defining sections. 

### Surface Hierarchy & Nesting
Instead of lines, use background shifts to define zones:
*   **App Canvas:** `surface` (#f8f9fa).
*   **Main Content Area:** `surface_container_lowest` (#ffffff).
*   **Sidebar/Navigation:** `surface_container_low` (#f3f4f5).
*   **Active Elements/Cards:** `surface_container_high` (#e7e8e9) or `primary_container` (#2170e4) for high-emphasis focus.

### The Glass & Gradient Rule
To inject "soul" into the minimalist aesthetic:
*   **Hero CTAs:** Use a subtle linear gradient from `primary` (#0058be) to `primary_container` (#2170e4) at a 135-degree angle.
*   **Floating Navigation:** Use glassmorphism. Set the background to a semi-transparent `surface_container_lowest` (80% opacity) with a `20px` backdrop-blur. This creates a "frosted glass" effect that keeps the learner grounded in the context of the page.

---

## 3. Typography: Editorial Authority
We pair **Plus Jakarta Sans** (Headlines) with **Inter** (Body) to balance modern playfulness with technical precision.

*   **Display (Plus Jakarta Sans):** Use `display-lg` for daily streaks or score summaries. The generous tracking and large scale convey a sense of achievement.
*   **Headlines (Plus Jakarta Sans):** Use `headline-md` for flashcard terms. The slightly rounder terminals of Jakarta Sans add the "playful" touch requested without losing professionalism.
*   **Body & Labels (Inter):** Use `body-md` for definitions and `label-sm` for meta-data (e.g., "Word Frequency"). Inter’s high x-height ensures legibility during long study sessions.

---

## 4. Elevation & Depth: Tonal Layering
Traditional shadows are often "dirty." In this system, depth is a physical property of light and layering.

*   **The Layering Principle:** A flashcard (`surface_container_lowest`) sitting on a study tray (`surface_container_low`) creates natural depth. 
*   **Ambient Shadows:** For floating elements like active flashcards or tooltips, use a shadow with a 40px blur and 4% opacity. The shadow color must be a tinted version of `surface_tint` (#005ac2) rather than pure black, ensuring the shadow feels like a reflection of the blue primary brand.
*   **The Ghost Border:** If a boundary is required for accessibility (e.g., input fields), use a "Ghost Border"—the `outline_variant` token at 15% opacity. It should be felt, not seen.

---

## 5. Components

### Flashcard UI (The Hero Component)
*   **Structure:** Use `xl` (3rem/48px) rounded corners. 
*   **Visuals:** Front side uses `surface_container_lowest`. The flip animation should be a "Spring" physics-based 3D rotation (0.6s duration).
*   **Interaction:** On hover, the card should lift using a soft ambient shadow and scale up by 2% (1.02x).

### Sidebar Navigation
*   **Styling:** No vertical divider. Use a `surface_container_low` background to distinguish it from the main `surface_bright` canvas.
*   **Active State:** Use a "Pill" indicator—a `primary_fixed` background with `on_primary_fixed` text.

### Stats Cards & Progress Bars
*   **Cards:** Use `lg` (2rem) corner radius. Use `surface_container_high` for the background.
*   **Progress Bars:** The track should be `surface_variant`. The indicator should be a gradient from `primary` to `primary_container`. Ensure the ends of the bar are `full` (999px) rounded for a soft, friendly feel.

### Buttons
*   **Primary:** High-elevation. Background: `primary`. Text: `on_primary`. Radius: `md` (1.5rem).
*   **Tertiary (Ghost):** No background or border. Use `primary` text color. Use for low-priority actions like "View Examples."

---

## 6. Do’s and Don’ts

### Do:
*   **Do** use vertical white space (Token `12` or `16`) to separate content modules instead of horizontal lines.
*   **Do** use `tertiary` (#924700) for "Urgency" or "Hard Words" to provide a sophisticated contrast to the primary blue.
*   **Do** ensure all micro-interactions use "Ease-Out-Expo" timing for a premium, snappy feel.

### Don’t:
*   **Don’t** use pure black (#000000) for text. Use `on_surface` (#191c1d) to maintain the soft editorial look.
*   **Don’t** use standard `16px` (1rem) rounding for cards. Stick to `lg` (2rem) or `xl` (3rem) to maintain the signature "Fluid" look.
*   **Don’t** stack more than three levels of surface nesting. (e.g., Surface > Container Low > Container Lowest). Any more will muddy the visual hierarchy.