# Configuration widgets

The field classes the configuration screen is built from: a page-aware element base, boolean, int, float and double fields, a text block, a button with a tooltip, and the scrollable editor for every profile option (the All page).

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `gui/elements/GuiElement` (base: page, x, y; no-op hooks `tick`, `render`, `update`, `setEnabled`, `setBasedOnMode`). `BooleanElement`, `IntElement`, `FloatElement`, `DoubleElement` (one profile attribute each). `ButtonExt` (button with a `tooltip(…)` helper and no narration). `TextExt` (multi-line text with max width and rows; its tooltip is stored but never shown). `WidgetElement` (shows any widget on one page). `ProfileConfigElement` (the All page). |
| 2 | **Inputs** | The screen's selected profile and its `Configuration` view (`02-profiles-dimensions`): names, values, comments, allowed values, min and max. |
| 3 | **Numbers** | See each section. |
| 4 | **Behavior** | See each section. |
| 5 | **Determinism** | Not world generation. |
| 6 | **Data formats** | String lists are edited as comma-separated text. |
| 7 | **Output** | Profile values changed through `GuiLCConfig.updateProfileValue` (validated) or directly (boolean buttons). |
| 8 | **Relationships** | Built and driven by [profile-screen.md](profile-screen.md). |
| 9 | **1.20 dependencies** | `EditBox` (responder, editable, text color, tooltip), `MultiLineTextWidget`, `Button` with a narration supplier, `Tooltip`, `GuiGraphics.drawString`/`fill`, `Font.width`. |
| 10 | **Bugs** | `UI-13`, `UI-14`, `UI-15` |

## Single-attribute fields

| Class | Widget | Size | Text color | Parse |
|---|---|---|---|---|
| `BooleanElement` | button `On` / `Off` | 60 × 16 | default | none |
| `IntElement` | text box | 45 × 16 | `0xFFE0E0E0`, or `0xFFFF5555` when rejected | `Integer.parseInt` |
| `FloatElement` | text box | 45 × 16 | as above | `Float.parseFloat` |
| `DoubleElement` | text box | width given by the page (120 or 45) × 16 | as above | `Double.parseDouble` |

Behavior (client):
1. **Label and prefix.** `label(text)` draws the text at (10, y + 5); `prefix(text)` draws it at (x − 8, y + 5); both white and only while the field is visible.
2. **Number fields, on every text change:** parse; if it does not parse (empty, `-`, half-typed), ignore silently. Else call `updateProfileValue`. On success, normal color and no tooltip; on rejection, red text and the tooltip `Invalid value: <error>`. A rejected value stays in the box while the profile keeps the old one (`UI-14`).
3. **`update`:** write the profile's current value into the box (`Float.toString`, `Double.toString` or `Integer.toString` depending on the stored type). Writing the box fires the change handler, so the value is written back into the selected profile, even when that profile is a shared standard profile (`UI-15`).
4. **Boolean button:** flip the label between `On` and `Off`, then build the profile's configuration, set the attribute to whether the label reads `On`, and copy it back into the profile without range or cross-option checks (`UI-13`); then clear caches.
5. **`setEnabled(b)`:** text boxes become editable or read-only; the boolean button becomes active or inactive.
6. **`setBasedOnMode(page)`:** visible only when the page name matches, ignoring case.
7. **`tick`:** text boxes blink their cursor.

## ProfileConfigElement

A data-driven, scrollable editor for every value of the selected profile's configuration (the All page).

| Item | Value |
|---|---|
| First row Y | 56 |
| Row height | 20 px |
| Bottom margin | 28 px (rows end at `H − 28`) |
| Visible rows | `max(1, (H − 28 − 56) / 20)` |
| Editor column X | `max(170, W / 2)` |
| Editor width | `max(80, W − editorX − 18)`, height 16 |
| Label width | `editorX − 18`, truncated with `...` to fit |
| Header | at (10, 43), color `0xFFAAAAAA`: `All profile settings (<first>-<last> of <count>) - scroll to browse` |
| Label color | white, or `0xFFFF5555` when the last edit was rejected |
| Scroll step | 3 rows per wheel notch |
| Scrollbar | only when there are more options than rows: track x `W − 8` to `W − 5`, from Y 56, height `rows × 20 − 4`, color `0xFF333333`; thumb height `max(12, track × rows / count)`, color `0xFFAAAAAA` |
| Text limit | 32767 characters |

Behavior (client):
1. Each row owns one text box and one button at the same place; a row shows the button for booleans and for options with a list of allowed values, and the text box otherwise.
2. **Bind a row to an option:** remember name, value and allowed values; set the text box or button text to the value (string lists joined with `, `); build the tooltip: the option name, a newline, its comment, then `\nAllowed: a, b, …`, or `\nRange: <min> to <max>`, or for string lists `\nEnter a comma-separated list`. A rejected edit appends `\nInvalid value: <error>`.
3. **Typing:** parse by the old value's type: int, float, double, string list (split on commas, trimmed, empties dropped; blank text is an empty list) or string. Unparseable text is ignored. Otherwise apply through `updateProfileValue`; if accepted with a normalized value that formats differently, rewrite the text.
4. **Button press:** booleans flip; options with allowed values move to the next allowed value (from the first if the current one is not in the list); apply through `updateProfileValue` and show the resulting value.
5. **Update** (profile changed): list all option names of the configuration, clamp the scroll offset, rebind rows; with no profile, unbind every row and show nothing.
6. **Scroll:** only on the All page with more options than rows; move by 3, clamp to `[0, count − rows]`, rebind.
7. **Visibility:** rows show only on the All page and only when bound; editable only when the profile is `customized`; hidden rows lose focus.

## Deviations in the new mod

Not decided here; see `docs/new-spec/` (area 09-client-ui).
