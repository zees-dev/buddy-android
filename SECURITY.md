# Security

Buddy lets AI agents use your phone, so its approval rules are its most important part: an agent must not send, post, buy, pay or delete anything without your OK on Buddy's card.

Installers, permission controllers, protected Android Settings, app stores and Buddy's own UI refuse taps, toggles and text entry regardless of app trust. Protected Settings destinations and their direct entry links include accessibility, app permissions, special access (unknown-app installation, overlay, notification access, usage access and device admin), developer/debugging controls, security, lock screen, biometrics, passwords, passkeys and accounts. Settings identifies these destinations with its localized titles and headings. Ordinary pages and links remain usable, including the system Settings search provider. Back, Home, reading and screenshots remain available everywhere on an unlocked phone. Scrolling in Settings and on protected surfaces uses accessibility scroll actions, including long swipes, so it cannot toggle a control on touch-down. These actions stay in the target's owning window, or the active window when no target is supplied. A long swipe with no scroll target does nothing. System shade tiles remain human-only; Back and Home dismiss the shade. Installing or updating an APK always requires the human to use Android's installer.

A plan confirmation grants no authority to a later tool call. Protected actions ask in the executing request; approval cannot be transferred or replayed. Screen actions bind the tool arguments, target identity and readable screen to a server-generated request identity, serialize dispatch and re-check them before acting. A changed target or ended session requires a new approval.

Play Store may be launched for catalog browsing. Search and app details use `open_url` with `market://search?q=...` or `market://details?id=...`, or their HTTPS `play.google.com/store/search` and `/store/apps/details` equivalents. Search accepts only `q` and optional `c=apps`; details accepts only one Android package `id`. Links are pinned to `com.android.vending`; credentials, ports, fragments, duplicate values, other routes and extra parameters are refused. All Store taps, text entry, short strokes, purchases, installs, updates, reviews and account controls remain human-only, regardless of app trust or a plan approval. Reading, screenshots and safe accessibility scrolling remain available. Other app stores gain no launch exception.

Chat authority comes from a temporary credential issued by Buddy to that chat's helper, revoked when the helper ends or the phone-tools token rotates. Caller-supplied chat IDs grant no authority. Each effect and batch substep checks the authenticated request again; token rotation closes connections and interrupts executing requests.

Settings credential editors are protected destinations regardless of their heading. In other application windows, a credential signal anywhere requires a bound human approval for every input action, before app trust or saved exceptions. Signals include password flags and input types, exposed autofill metadata, localized credential labels and resource IDs, and PIN/passcode indicators. This covers taps, long presses, swipes, typing, paste, clipboard writes, key actions and batch substeps. Entered text is not a credential label. Ordinary quantity, phone, amount, search, calculator and dialer inputs carry no credential signal; a numeric input type alone does not require approval. Read-only tools, Back, Home and app launch remain available. Typing labels, approval notes and step logs omit the text, and typing steps have no saved screenshot. Agent vendors may retain tool arguments in their own private transcripts.

## Reporting a vulnerability

Found a way for an agent to act without your approval, to approve for you, or to reach Buddy's phone tools without its token? Please don't post it publicly. Use [private vulnerability reporting](https://github.com/ghorbelhamdi/buddy-android/security/advisories/new) instead.

Helpful details: your Buddy version (GitHub or Play), the agent and model, what you asked, and the steps that let the agent get around the approval.

## What counts

- An agent (or a web page, message or notification it reads) gets a risky action done without an approval card, or with an approval meant for something else.
- Another app on the phone uses Buddy's phone tools server (127.0.0.1:8765) or its events without the per-install token.
- A notification rule's run acts outside what its rule allows.
- Private data (chats, screen text, sign-ins) leaves the phone other than to the AI service you chose.

The files that decide what an agent may do without asking are `Guard.java`, `Reviewer.java`, the approval cards in `Bubble.java`, and `linux/system-prompt.md`.

## Supported versions

Only the latest release, on GitHub and on Google Play.
