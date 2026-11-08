#!/bin/sh
set -eu

# Run in Buddy's terminal; settings and keys are provisioned privately by the operator.
config="$HOME/.config/buddy-phone"
test -f "$config/destination" || { echo 'Phone link is not configured.' >&2; exit 1; }
destination=$(cat "$config/destination")
case "$destination" in
  *@100.*) ;;
  *) echo 'Expected the configured Mac tailnet address.' >&2; exit 1 ;;
esac
command -v ssh >/dev/null || { echo 'The approved OpenSSH client is not installed.' >&2; exit 1; }
mkdir -p "$HOME/.buddy"
exec 9>"$HOME/.buddy/phone-link.lock"
busybox flock -n 9 || { echo 'Phone link is already running.'; exit 0; }
child=
trap '[ -z "$child" ] || kill "$child" 2>/dev/null || true; exit 0' INT TERM HUP
delay=5
while :; do
  ssh -N -T -F /dev/null -i "$config/key" \
    -o UserKnownHostsFile="$config/known_hosts" -o StrictHostKeyChecking=yes \
    -o IdentitiesOnly=yes -o BatchMode=yes -o PasswordAuthentication=no \
    -o KbdInteractiveAuthentication=no -o ExitOnForwardFailure=yes \
    -o ServerAliveInterval=20 -o ServerAliveCountMax=3 -o ConnectTimeout=10 \
    -o ConnectionAttempts=1 \
    -L 127.0.0.1:8318:127.0.0.1:8318 \
    -R 127.0.0.1:8766:127.0.0.1:8765 "$destination" 9>&- &
  child=$!
  wait "$child" || true
  child=
  echo "Phone link disconnected; retrying in ${delay}s." >&2
  sleep "$delay"
  [ "$delay" -ge 30 ] || delay=$((delay + 5))
done
