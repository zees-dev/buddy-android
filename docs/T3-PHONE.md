# T3 phone connection

Add the existing folder `~/z-sandbox/buddy-android` to T3. In a session opened in this repo, invoke `$phone` with Codex or `/phone` with Claude Code. `.claude/skills/phone` links to the canonical `.agents/skills/phone` skill. The skill needs the operator's connection and private credential; a new clone on another Mac is not provisioned automatically.

## Start or recover the connection

Keep Tailscale connected on the phone and Mac, and keep the Mac awake with SSH and the AI gateway running. In **Buddy → Sessions → Terminal**, run:

```sh
sh /root/bin/phone-link
```

Leave that terminal session running; return Home without closing its tab. It retries after network failures. If the Mac retains an old half-open SSH session, that session can hold port 8766 and delay recovery until it times out or an operator closes it. Reboot and network-switch recovery have not been tested, and no boot service is installed.

**After killing/restarting Buddy:** Sol 6.1, high reasoning and its gateway credential remain saved. However, killing Buddy can stop its terminal and SSH tunnel. Start the command above before using either the bubble or T3. You do not need to pair T3 or re-enter credentials each time. A phone reboot, Android killing the terminal, or the known popup crash can also require this recovery step.

The installed app includes local `develop` fixes. Publishing this skill does not publish an APK or the earlier app commits.

## Transport and credential

The phone's SSH client connects outward to the Mac over Tailscale. One connection carries:

- Mac `127.0.0.1:8766` → phone `127.0.0.1:8765`: Buddy MCP for T3.
- Phone `127.0.0.1:8318` → Mac `127.0.0.1:8318`: AI gateway for the bubble.

Buddy stays on phone localhost; no LAN/public listener or router forwarding is needed. The dedicated SSH key permits only those forwards, with shell access disabled and the Mac host identity pinned. Private key, known host and destination files are under `/root/.config/buddy-phone/` on the phone, outside git. `/root/bin/phone-link` comes from `mcp-bridge/phone-link.sh`; the approved client is `openssh-client-default=10.3_p1-r2`.

The repo helper accepts `BUDDY_TOKEN` or reads the operator-owned, mode-0600 file `~/.config/buddy-phone/token` on this Mac. Provision it privately from Buddy's Phone tools setup; never paste it into a prompt, repo or shell command. Native Codex MCP registration can use localhost `8766` and `bearer_token_env_var = "BUDDY_TOKEN"`; this repo does not change global harness configuration.

## Verified scope and remaining limits

With physical wireless debugging off and its ADB mappings absent, SSH carried MCP initialization, all 40 tools, screen reads, opening Clock and returning Home. A phone Codex inference passed through SSH with ADB mappings already absent, before debugging was switched off. The emulator's separate forward on Mac `8765` was preserved.

After recovery, verify `initialize`, `tools/list`, `read_screen` and a bubble reply. Fast mode is requested as `priority`, but the measured response reported `default`; actual Fast processing is unconfirmed. The automatic radial-menu hint caused a `BadTokenException` during the first trial. These limitations are not fixed by the skill.
