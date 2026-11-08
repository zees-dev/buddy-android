# Security

Buddy lets AI agents use your phone, so its approval rules are its most important part: an agent must not send, post, buy, pay or delete anything without your OK on Buddy's card.

Installers, permission controllers, Android Settings and security controls, app stores, the system shade and Buddy's own UI are human-only. Phone input stops while these surfaces are present, including coordinate gestures and keyboard actions, regardless of saved app trust. Installing or updating an APK always requires the human to use Android's installer.

A plan confirmation grants no authority to a later tool call. Protected actions ask in the executing request; approval cannot be transferred or replayed. Screen actions bind the tool arguments, target identity and readable screen to a server-generated request identity, serialize dispatch and re-check them before acting. A changed target or ended session requires a new approval.

Chat authority comes from a temporary credential issued by Buddy to that chat's helper, revoked when the helper ends or the phone-tools token rotates. Caller-supplied chat IDs grant no authority. Each effect and batch substep checks the authenticated request again; token rotation closes connections and interrupts executing requests.

Native text entry checks password flags, input types, accessibility sensitivity, field hints and exposed autofill metadata. Sensitive entry always needs an explicit, bound human approval, even in trusted apps. Its text is shown only on the approval card; typing labels, approval notes and step logs omit the text, and typing steps have no saved screenshot. Agent vendors may retain tool arguments in their own private transcripts.

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
