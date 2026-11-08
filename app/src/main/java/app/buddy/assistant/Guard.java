package app.buddy.assistant;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The hard limit behind "Buddy asks before anything risky", for the apps the user puts on it (Settings →
 * App permissions; trusted apps leave asking to the agent). Enforced here, in Buddy: before a tap lands on a
 * send / pay / delete / … button (by its text, icon description or id), or text is typed with "submit", the
 * user must approve that action on the card Buddy shows in the executing call. Plan confirmations cannot
 * authorize later actions. The agent can't tap Buddy's
 * own bubble or approval card, so it can't approve for the user.
 */
final class Guard {
    /** Words on buttons that send, pay, delete or otherwise can't be taken back, and add-to-cart buttons, so every
     * agent asks the same way before shopping, in each of Buddy's languages. */
    private static final Pattern RISKY = Pattern.compile("(?iu)^[^\\p{L}]*(?:(?:proceed|continue|go) to |passer (?:à la|au) |weiter zur |zur )?("
            + "send|post|publish|share now|tweet|reply all|submit|confirm|pay|pay now|buy|buy now|purchase|"
            + "order|place order|checkout|check out|payment|add to cart|add to basket|add to bag|add to trolley|subscribe|transfer|donate|delete|remove|erase|uninstall|"
            + "unfriend|block|report|unsubscribe|deactivate|factory reset|reset|sign out|log out|logout|call|"
            + "envoyer|envoi|publier|partager maintenant|répondre à tous|valider|confirmer|payer|acheter|commander|"
            + "ajouter au panier|passer la commande|passer commande|caisse|s'abonner|virement|transférer|supprimer|effacer|désinstaller|bloquer|signaler|se désabonner|"
            + "désactiver|réinitialiser|se déconnecter|déconnexion|appeler|"
            + "senden|absenden|bezahlen|kaufen|bestellen|löschen|entfernen|in den einkaufswagen|in den warenkorb|kasse|"
            + "enviar|pagar|comprar|eliminar|borrar|añadir al carrito|agregar al carrito|inviare|pagare|acquista|elimina|"
            + "aggiungi al carrello|invia|pubblica|paga|paga ora|compra|acquista ora|ordina|conferma|rimuovi|cancella|"
            + "disinstalla|blocca|segnala|esci|disconnetti|chiama|abbonati|iscriviti|trasferisci|procedi all'acquisto|"
            + "publicar|confirmar|pedir|realizar pedido|finalizar compra|tramitar pedido|suscribirse|transferir|"
            + "desinstalar|bloquear|denunciar|cerrar sesión|llamar|quitar|"
            + "posten|veröffentlichen|jetzt kaufen|zahlungspflichtig bestellen|abonnieren|überweisen|deinstallieren|"
            + "blockieren|melden|abmelden|anrufen|bestätigen|"
            // Portuguese, Dutch, Polish, Russian, Ukrainian, Turkish, Hindi, Indonesian, Japanese, Korean, Chinese
            + "comprar agora|fazer pedido|adicionar ao carrinho|excluir|apagar|remover|assinar|inscrever-se|denunciar|sair|ligar|"
            + "verzenden|versturen|verstuur|plaatsen|publiceren|betalen|nu betalen|kopen|nu kopen|bestellen|afrekenen|"
            + "in winkelwagen|toevoegen aan winkelwagen|verwijderen|wissen|bevestigen|overmaken|abonneren|blokkeren|"
            + "uitloggen|afmelden|bellen|"
            + "wyślij|opublikuj|zapłać|kup|kup teraz|zamów|kupuję|zamawiam|do kasy|dodaj do koszyka|usuń|potwierdź|"
            + "przelej|subskrybuj|odinstaluj|zablokuj|zgłoś|wyloguj|zadzwoń|"
            + "отправить|опубликовать|оплатить|купить|заказать|оформить заказ|в корзину|добавить в корзину|удалить|"
            + "подтвердить|перевести|подписаться|заблокировать|пожаловаться|выйти|позвонить|"
            + "надіслати|відправити|опублікувати|сплатити|купити|замовити|оформити замовлення|у кошик|додати в кошик|"
            + "додати до кошика|видалити|підтвердити|переказати|підписатися|заблокувати|поскаржитися|вийти|зателефонувати|"
            + "gönder|paylaş|yayınla|öde|şimdi öde|satın al|hemen al|sipariş ver|siparişi tamamla|sepete ekle|sil|"
            + "kaldır|onayla|abone ol|engelle|şikayet et|çıkış yap|"
            + "भेजें|पोस्ट करें|भुगतान करें|खरीदें|अभी खरीदें|ऑर्डर करें|कार्ट में जोड़ें|हटाएं|हटाएँ|मिटाएं|"
            + "पुष्टि करें|कॉल करें|"
            + "kirim|bayar|bayar sekarang|beli|beli sekarang|pesan sekarang|buat pesanan|tambah ke keranjang|"
            + "masukkan keranjang|hapus|konfirmasi|berlangganan|blokir|laporkan|telepon|"
            + "送信|送信する|投稿|投稿する|支払う|支払い|購入|購入する|今すぐ購入|注文|注文する|注文を確定する|"
            + "カートに入れる|カートに追加|削除|削除する|確定|確定する|送金|購読|アンインストール|ブロック|報告|"
            + "ログアウト|発信|"
            + "보내기|전송|게시|결제|결제하기|구매|구매하기|바로 구매|주문|주문하기|장바구니 담기|장바구니에 담기|"
            + "삭제|삭제하기|송금|이체|구독|구독하기|차단|신고|로그아웃|전화 걸기|"
            + "发送|发布|支付|付款|立即支付|立即付款|购买|立即购买|下单|提交订单|加入购物车|删除|移除|确认|转账|"
            + "订阅|卸载|拉黑|举报|退出登录|拨打|"
            + "إرسال|ارسال|أرسل|دفع|ادفع|شراء|اشتر|حذف|احذف|تأكيد|اتصال|أضف إلى السلة|إضافة إلى السلة|"
            // Vietnamese ("Đăng nhập" is logging in and "Đăng ký" signing up, not posting)
            + "gửi|gửi đi|đăng(?! nhập| ký| xuất)|đăng bài|chia sẻ ngay|thanh toán|thanh toán ngay|mua|mua ngay|mua hàng|đặt hàng|đặt mua|"
            + "thêm vào giỏ|thêm vào giỏ hàng|xóa|xoá|gỡ|gỡ bỏ|gỡ cài đặt|xác nhận|chuyển tiền|chuyển khoản|đăng ký|"
            + "hủy đăng ký|huỷ đăng ký|chặn|báo cáo|đăng xuất|gọi|gọi điện"
            + ")(?![\\p{L}])");
    /** Of those, the ones that send or post a message, in each of Buddy's languages: a notification rule's countdown
     * may approve these (see Triggers), never paying, buying, deleting and the rest. */
    private static final Pattern SEND = Pattern.compile("(?iu)^[^\\p{L}]*("
            + "send|send now|post|publish|share now|tweet|reply|reply all|"
            + "envoyer|envoi|publier|partager maintenant|répondre|répondre à tous|senden|absenden|posten|veröffentlichen|antworten|"
            + "enviar|publicar|responder|inviare|invia|pubblica|rispondi|verzenden|versturen|verstuur|plaatsen|publiceren|"
            + "beantwoorden|wyślij|opublikuj|odpowiedz|отправить|опубликовать|ответить|надіслати|відправити|опублікувати|"
            + "відповісти|gönder|paylaş|yayınla|yanıtla|भेजें|पोस्ट करें|जवाब दें|kirim|balas|送信|送信する|投稿|投稿する|返信|"
            + "보내기|전송|게시|답장|发送|发布|回复|إرسال|ارسال|أرسل|رد|gửi|gửi đi|đăng(?! nhập| ký| xuất)|đăng bài|trả lời"
            + ")(?![\\p{L}])");
    /** A send button known only by its id (see RISKY_ID), as riskyLabel words it ("send", "btn send", "send button"). */
    private static final Pattern SEND_ID = Pattern.compile("(?i)(^|\\s)(send|sendbutton|post|reply)(\\s|$)");
    /** Resource ids of send buttons that only have an icon (e.g. WhatsApp's "send"). */
    private static final Pattern RISKY_ID = Pattern.compile("(?i)[:/_](send|send_button|sendbutton|btn_send|delete|pay|buy|post|submit|add-to-cart|add_to_cart|addtocart)\\b");
    /** Apps that start on "hard limit": messaging, email, social, calls, shopping, payments, banking. */
    private static final java.util.Set<String> HARD_BY_DEFAULT = new java.util.HashSet<>(java.util.Arrays.asList(
            "com.whatsapp", "com.whatsapp.w4b", "com.google.android.apps.messaging", "com.samsung.android.messaging",
            "com.android.mms", "org.telegram.messenger", "org.thoughtcrime.securesms", "com.facebook.orca",
            "com.facebook.katana", "com.instagram.android", "com.snapchat.android", "com.twitter.android",
            "com.zhiliaoapp.musically", "com.linkedin.android", "com.discord", "com.Slack", "com.viber.voip",
            "com.google.android.gm", "com.microsoft.office.outlook", "com.yahoo.mobile.client.android.mail",
            "com.google.android.dialer", "com.android.dialer", "com.samsung.android.dialer", "com.oneplus.dialer",
            "com.android.vending"));
    private static final Pattern HARD_NAME = Pattern.compile(
            "(?i)(bank|banking|pay|wallet|finance|credit|crypto|trading|broker)");

    // ------------------------------------------------------------------ the user's button lists

    /** A rule: a button label, in one app (pkg) or in all apps (pkg null). */
    static final class Rule {
        final String label, pkg, app;

        Rule(String label, String pkg, String app) {
            this.label = label;
            this.pkg = pkg;
            this.app = app;
        }

        org.json.JSONObject json() throws org.json.JSONException {
            return new org.json.JSONObject().put("label", label).put("pkg", pkg == null ? "" : pkg).put("app", app == null ? "" : app);
        }

        static Rule of(org.json.JSONObject o) {
            String pkg = o.optString("pkg");
            return new Rule(o.optString("label"), pkg.isEmpty() ? null : pkg, o.optString("app"));
        }

        String describe() {
            return pkg == null ? BuddyApp.str(R.string.guard_rule_all_apps, label) : label + " · " + (app == null || app.isEmpty() ? pkg : app);
        }
    }

    /** "allow" (never ask) or "ask" (always ask), or "seen" (buttons the agent tapped, newest first). */
    static java.util.List<Rule> rules(android.content.Context c, String list) {
        java.util.List<Rule> out = new java.util.ArrayList<>();
        try {
            org.json.JSONArray a = new org.json.JSONArray(Prefs.guardList(c, list));
            for (int i = 0; i < a.length(); i++) out.add(Rule.of(a.getJSONObject(i)));
        } catch (Exception ignored) {
        }
        return out;
    }

    static void saveRules(android.content.Context c, String list, java.util.List<Rule> rules) {
        org.json.JSONArray a = new org.json.JSONArray();
        try {
            for (Rule r : rules) a.put(r.json());
        } catch (Exception ignored) {
        }
        Prefs.setGuardList(c, list, a.toString());
    }

    static void addRule(android.content.Context c, String list, Rule r) {
        java.util.List<Rule> rules = rules(c, list);
        for (java.util.Iterator<Rule> it = rules.iterator(); it.hasNext(); ) {
            Rule x = it.next();
            if (norm(x.label).equals(norm(r.label)) && java.util.Objects.equals(x.pkg, r.pkg)) it.remove();
        }
        rules.add(0, r);
        saveRules(c, list, rules);
        // a button is on one list at most
        String other = "allow".equals(list) ? "ask" : "allow";
        java.util.List<Rule> o = rules(c, other);
        boolean changed = false;
        for (java.util.Iterator<Rule> it = o.iterator(); it.hasNext(); ) {
            Rule x = it.next();
            if (norm(x.label).equals(norm(r.label)) && java.util.Objects.equals(x.pkg, r.pkg)) {
                it.remove();
                changed = true;
            }
        }
        if (changed) saveRules(c, other, o);
    }

    /** Remember a button the agent tapped, for the lists' suggestions. */
    static void remember(android.content.Context c, String label, String pkg, String app) {
        if (label == null || pkg == null) return;
        java.util.List<Rule> seen = rules(c, "seen");
        for (java.util.Iterator<Rule> it = seen.iterator(); it.hasNext(); ) {
            Rule x = it.next();
            if (norm(x.label).equals(norm(label)) && pkg.equals(x.pkg)) it.remove();
        }
        seen.add(0, new Rule(label, pkg, app));
        while (seen.size() > 150) seen.remove(seen.size() - 1);
        saveRules(c, "seen", seen);
    }

    /** Is this button on the list, for this app or all apps? */
    static boolean listed(android.content.Context c, String list, String label, String pkg) {
        if (label == null) return false;
        String l = norm(label);
        for (Rule r : rules(c, list)) {
            if (r.pkg != null && !r.pkg.equals(pkg)) continue;
            String x = norm(r.label);
            if (!x.isEmpty() && (l.equals(x) || l.startsWith(x + " "))) return true;
        }
        return false;
    }

    static String norm(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Labels that say nothing about what a button does (web form buttons are often just "Submit"). */
    private static final Pattern GENERIC = Pattern.compile("(?i)submit|button|btn|input|ok|go|enter|envoyer|absenden|enviar|invia");

    /** A short name for what a tap would press. A generic one ("Submit") is replaced by the button's id
     * ("add-to-cart-button" → "add to cart") or the visible text next to it, so the card says what it does. */
    static String buttonLabel(AccessibilityNodeInfo n) {
        if (n == null) return null; // nothing under a tap's point (a screen changing)
        String l = rawButtonLabel(n);
        if (l != null && !GENERIC.matcher(l.trim()).matches()) return l;
        AccessibilityNodeInfo cur = n;
        for (int i = 0; cur != null && i < 4; i++) {
            String id = cur.getViewIdResourceName();
            if (id != null) {
                String w = id.substring(id.lastIndexOf('/') + 1).replaceAll("[-_.]+", " ")
                        .replaceAll("(?i)\\b(submit|button|btn|input)\\b", " ").trim().replaceAll("\\s+", " ");
                if (w.matches("[\\p{L} ]{3,}") && !w.contains("autoid")) return w; // not generated ids ("a-autoid-3")
            }
            if (cur.isClickable()) break;
            cur = cur.getParent();
        }
        AccessibilityNodeInfo p = n.getParent(); // the visible text beside it (a form button and its label)
        for (int up = 0; p != null && up < 2; up++, p = p.getParent()) {
            for (int c = 0; c < Math.min(p.getChildCount(), 8); c++) {
                AccessibilityNodeInfo ch = p.getChild(c);
                String h = ch == null ? null : shortLabel(ch);
                if (h != null && !GENERIC.matcher(h).matches()) return h;
            }
        }
        return l;
    }

    private static String rawButtonLabel(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo cur = n;
        for (int i = 0; cur != null && i < 4; i++) {
            String l = shortLabel(cur);
            if (l != null) return l;
            if (cur.isClickable()) {
                for (int c = 0; c < Math.min(cur.getChildCount(), 6); c++) {
                    AccessibilityNodeInfo ch = cur.getChild(c);
                    String h = ch == null ? null : shortLabel(ch);
                    if (h != null) return h;
                }
                String id = cur.getViewIdResourceName();
                if (id != null) return id.substring(id.lastIndexOf('/') + 1).replace('_', ' ');
                break;
            }
            cur = cur.getParent();
        }
        return null;
    }

    private static String shortLabel(AccessibilityNodeInfo n) {
        if (n.isEditable() && sensitiveInput(n)) return "Sensitive field";
        for (CharSequence cs : new CharSequence[]{n.getText(), n.getContentDescription()}) {
            if (cs == null) continue;
            String s = cs.toString().trim().replaceAll("\\s+", " ");
            if (s.isEmpty()) continue;
            return s.length() > 40 ? s.substring(0, 40) + "…" : s;
        }
        return null;
    }

    /** Trust: the agent works on its own in this app, without asking the user. */
    static boolean autonomous(android.content.Context c, String pkg) {
        return pkg != null && "trust".equals(mode(c, pkg, null));
    }

    /** "hard": Buddy enforces approval in this app; "smart": the lists, then a reviewer AI for every tap;
     *  "trust": the agent works on its own, without asking. */
    static String mode(android.content.Context c, String pkg, CharSequence label) {
        return modeWhy(c, pkg, label)[0];
    }

    /** {mode, why}: the user's choice, or the suggestion and its reason. */
    static String[] modeWhy(android.content.Context c, String pkg, CharSequence label) {
        if (pkg == null) return new String[]{"trust", ""};
        String set = Prefs.appSafety(c, pkg);
        if (set != null) return new String[]{set, c.getString(R.string.guard_why_choice)};
        if (HARD_BY_DEFAULT.contains(pkg)) return new String[]{"hard", c.getString(R.string.guard_why_category)};
        java.util.regex.Matcher m = HARD_NAME.matcher(pkg);
        if (m.find()) return new String[]{"hard", c.getString(R.string.guard_why_name, m.group(1))};
        if (label != null && (m = HARD_NAME.matcher(label)).find()) return new String[]{"hard", c.getString(R.string.guard_why_name, m.group(1))};
        try {
            android.content.pm.ApplicationInfo ai = c.getPackageManager().getApplicationInfo(pkg, 0);
            if (ai.category == android.content.pm.ApplicationInfo.CATEGORY_SOCIAL) return new String[]{"hard", c.getString(R.string.guard_why_social)};
            if (label == null) {
                CharSequence l = c.getPackageManager().getApplicationLabel(ai);
                if (l != null && (m = HARD_NAME.matcher(l)).find()) return new String[]{"hard", c.getString(R.string.guard_why_name, m.group(1))};
            }
        } catch (Exception ignored) {
        }
        // the rest follow the first-setup choice: work on its own, or ask first
        return Prefs.autonomousDefault(c) ? new String[]{"trust", c.getString(R.string.guard_why_suggested)}
                : new String[]{"hard", c.getString(R.string.guard_why_setup)};
    }

    private Guard() {
    }

    /**
     * Does tapping this send or post a message (and nothing more, like paying or deleting)? Never in a payment, bank or
     * crypto app, where "Send" can mean sending money.
     */
    static boolean sends(android.content.Context c, AccessibilityNodeInfo n) {
        String l = n == null ? null : riskyLabel(n);
        if (l == null || moneyApp(c, n.getPackageName())) return false;
        return SEND.matcher(l.trim()).find() || SEND_ID.matcher(l.trim()).find();
    }

    /** A payment, banking or crypto app by its name (or one Buddy can't tell). */
    static boolean moneyApp(android.content.Context c, CharSequence pkg) {
        if (pkg == null) return true;
        String p = pkg.toString();
        if (HARD_NAME.matcher(p).find()) return true;
        try {
            android.content.pm.PackageManager pm = c.getPackageManager();
            CharSequence label = pm.getApplicationLabel(pm.getApplicationInfo(p, 0));
            return label != null && HARD_NAME.matcher(label).find();
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------ a chat's run rule

    /** Apps a run rule never covers, whatever the user picks: settings, permissions, app stores and installers. */
    private static final Pattern NEVER_ALLOWED = Pattern.compile(
            "(?i)(settings|permission|security|installer|vending|store|market|^com\\.google\\.android\\.gms$)");

    /**
     * Can this app go on a chat's "Allowed without asking"? Not payment, bank or crypto apps (see moneyApp), nor
     * Settings, permission screens, app stores or installers: those approvals are always the user's.
     */
    static boolean allowable(android.content.Context c, String pkg) {
        return pkg != null && !NEVER_ALLOWED.matcher(pkg).find() && !moneyApp(c, pkg)
                && !BuildInfo.class.getPackage().getName().equals(pkg);
    }

    /**
     * The calling chat's run rule: the user allowed this app without asking until the run ends (ChatHub.allowedFor).
     * It covers only what a notification rule's countdown may approve (sending a message) and a smart reviewer's
     * "ask" on a tap that isn't a risky button; Tools decides which an action is. Never in a notification rule's run.
     */
    static boolean runRule(android.content.Context c, String pkg) {
        String chat = McpServer.CALLER_CHAT.get();
        if (chat == null || Triggers.ruleRun(chat) || !allowable(c, pkg)) return false;
        return ChatHub.get(c).allowedFor(chat).contains(pkg);
    }

    /** What a tap at (x, y) would hit: the topmost app window's deepest node there. */
    static AccessibilityNodeInfo nodeAt(List<AccessibilityWindowInfo> windows, int x, int y) {
        AccessibilityWindowInfo top = null;
        Rect r = new Rect();
        for (AccessibilityWindowInfo w : windows) {
            // Buddy's floating bubble and panel let taps through while a gesture runs (see
            // BuddyService.setBubblePassThrough), so what counts is the app underneath
            if (w.getType() == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) continue;
            w.getBoundsInScreen(r);
            if (!r.contains(x, y)) continue;
            if (top == null || w.getLayer() > top.getLayer()) top = w;
        }
        if (top == null) return null;
        AccessibilityNodeInfo n = top.getRoot();
        if (n == null) return null;
        for (int depth = 0; depth < 60; depth++) {
            AccessibilityNodeInfo next = null;
            for (int i = n.getChildCount() - 1; i >= 0; i--) { // later children draw on top
                AccessibilityNodeInfo ch = n.getChild(i);
                if (ch == null || !ch.isVisibleToUser()) continue;
                ch.getBoundsInScreen(r);
                if (r.contains(x, y)) {
                    next = ch;
                    break;
                }
            }
            if (next == null) break;
            n = next;
        }
        return n;
    }

    /** Is this Buddy's own UI (its screens, the bubble, its approval card)? */
    static boolean isBuddy(AccessibilityNodeInfo n) {
        return n != null && n.getPackageName() != null
                && BuildInfo.class.getPackage().getName().contentEquals(n.getPackageName());
    }

    /** Installers, security settings and Buddy are human-only, regardless of app trust or saved exceptions. */
    static boolean sensitivePackage(android.content.Context c, CharSequence pkg) {
        if (pkg == null) return true;
        String p = pkg.toString();
        if (settingsPackage(c, p)) return false; // Settings is checked by page and destination, below.
        if (NEVER_ALLOWED.matcher(p).find() || p.equals(c.getPackageName()) || p.equals("android")
                || p.equals("com.android.systemui")) return true;
        android.content.pm.PackageManager pm = c.getPackageManager();
        for (String action : new String[]{"android.intent.action.MANAGE_PERMISSIONS", "android.intent.action.MANAGE_APP_PERMISSIONS",
                "android.settings.MANAGE_UNKNOWN_APP_SOURCES", "android.settings.MANAGE_OVERLAY_PERMISSION",
                "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS", "android.app.action.ADD_DEVICE_ADMIN",
                "android.settings.SECURITY_SETTINGS", "android.settings.ACCESSIBILITY_SETTINGS"}) {
            for (android.content.pm.ResolveInfo handler : pm.queryIntentActivities(new android.content.Intent(action), 0)) {
                if (handler.activityInfo != null && p.equals(handler.activityInfo.packageName)) return true;
            }
        }
        android.content.Intent install = new android.content.Intent(android.content.Intent.ACTION_VIEW)
                .setDataAndType(android.net.Uri.parse("content://" + c.getPackageName() + "/install.apk"),
                        "application/vnd.android.package-archive");
        for (android.content.pm.ResolveInfo to : pm.queryIntentActivities(install, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY))
            if (to.activityInfo != null && p.equals(to.activityInfo.packageName)) return true;
        return false;
    }

    static boolean settingsPackage(android.content.Context c, String pkg) {
        android.content.pm.ResolveInfo to = c.getPackageManager().resolveActivity(
                new android.content.Intent(android.provider.Settings.ACTION_SETTINGS), 0);
        return to != null && to.activityInfo != null && pkg.equals(to.activityInfo.packageName);
    }

    private static final String[] PROTECTED_SETTINGS = {"accessibility_settings", "accessibility_settings_title",
            "app_permissions", "special_access", "install_unknown_applications",
            "development_settings_title", "security_settings_title", "security_dashboard_title", "lockscreen_settings_title",
            "lock_settings_title", "account_settings_title", "user_settings_title", "biometric_settings_title",
            "fingerprint_settings_title", "face_settings_title", "passwords_settings_title", "credentials_settings_title",
            "usage_access", "usage_access_title", "overlay_settings", "device_admin_settings_title", "manage_device_admin",
            "adb_wireless_settings", "account_dashboard_title_with_passkeys", "install_apps", "manage_notification_access_title",
            "build_number", "notification_access_settings_title"};
    private static final Pattern PROTECTED_SETTING = Pattern.compile(
            "(?iu)accessibility|permission|special.?access|unknown.?apps|install|developer|development|debug|security|"
            + "lock.?screen|screen.?lock|biometric|fingerprint|face.?unlock|password|passcode|passkey|account|build.?number|"
            + "overlay|display.over.other.apps|notification.?access|usage.?access|device.?admin|credential|\\bpin\\b");

    /** Protected destinations use the system app's localized resources. */
    private static boolean protectedSetting(BuddyService c, CharSequence packageName, CharSequence text) {
        if (text == null || text.length() == 0) return false;
        if (PROTECTED_SETTING.matcher(text).find()) return true;
        String pkg = String.valueOf(packageName);
        try {
            android.content.res.Resources resources = c.getPackageManager().getResourcesForApplication(pkg);
            for (String name : PROTECTED_SETTINGS) {
                int id = resources.getIdentifier(name, "string", pkg);
                if (id != 0 && text.toString().equals(resources.getString(id))) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    private static boolean protectedSettingsPage(BuddyService svc, AccessibilityNodeInfo n) {
        if (n == null) return false;
        if (n.isHeading() && (protectedSetting(svc, n.getPackageName(), n.getText())
                || protectedSetting(svc, n.getPackageName(), n.getContentDescription()))) return true;
        if (protectedSetting(svc, n.getPackageName(), n.getPaneTitle())) return true;
        for (int i = 0; i < n.getChildCount(); i++)
            if (protectedSettingsPage(svc, n.getChild(i))) return true;
        return false;
    }

    static boolean settingsTarget(BuddyService svc, AccessibilityNodeInfo target) {
        if (target == null || target.getPackageName() == null || !settingsPackage(svc, target.getPackageName().toString())) return true;
        AccessibilityNodeInfo button = target;
        while (button.getParent() != null && !button.isClickable()) button = button.getParent();
        return !protectedSettingsLink(svc, button, 0);
    }

    private static boolean protectedSettingsLink(BuddyService svc, AccessibilityNodeInfo n, int depth) {
        if (n == null || depth > 5) return false;
        String pkg = String.valueOf(n.getPackageName());
        if (protectedSetting(svc, pkg, n.getText())
                || protectedSetting(svc, pkg, n.getContentDescription())
                || PROTECTED_SETTING.matcher(n.getText() + " " + n.getContentDescription() + " " + n.getViewIdResourceName()).find()) return true;
        for (int i = 0; i < n.getChildCount(); i++) if (protectedSettingsLink(svc, n.getChild(i), depth + 1)) return true;
        return false;
    }

    static boolean offLimits(android.content.Context c, AccessibilityNodeInfo n) {
        if (n == null) return false;
        AccessibilityWindowInfo window = n.getWindow();
        AccessibilityNodeInfo root = window == null ? null : window.getRoot();
        return root == null || sensitivePackage(c, root.getPackageName());
    }

    private static final Pattern SENSITIVE_INPUT = Pattern.compile(
            "(?iu)password|pass.?code|access.?code|one.?time|otp|verification.?code|credit.?card|card.?number|cc-|cvc|cvv|security.?code|(?:^|[^a-z])pin(?:[_-]?(?:entry|code|dots?|display|pad))?(?:$|[^a-z])|"
            + "密码|密碼|口令|验证码|驗證碼|校验码|校驗碼|动态码|動態碼|認証コード|暗証番号|パスワード|비밀번호|인증.?번호|인증.?코드|"
            + "mot.de.passe|code.d.accès|code.de.vérification|passwort|kennwort|einmalcode|bestätigungscode|contraseña|código.de.acceso|código.de.verificación|"
            + "senha|código.de.verificação|codice.di.verifica|wachtwoord|verificatiecode|hasło|kod.weryfikacyjny|пароль|код.подтверждения|"
            + "парол|код.підтвердження|şifre|doğrulama.kodu|पासवर्ड|ओटीपी|पिन|सत्यापन|kata.sandi|kode.verifikasi|mật.khẩu|mã.xác.minh|كلمة.المرور|رمز.التأكيد|رمز.التحقق");
    /** One credential signal protects every input path in the application window. */
    private static boolean sensitiveWindow(AccessibilityNodeInfo n) {
        if (n == null) return true;
        if (sensitiveInput(n)) return true;
        for (int i = 0; i < n.getChildCount(); i++)
            if (sensitiveWindow(n.getChild(i))) return true;
        return false;
    }

    /** Accessibility does not expose View autofill hints uniformly; also check input types, hints and exposed extras. */
    static boolean sensitiveInput(AccessibilityNodeInfo n) {
        if (n == null || n.isPassword()) return true;
        if (android.os.Build.VERSION.SDK_INT >= 34 && n.isAccessibilityDataSensitive()) return true;
        int type = n.getInputType(), cls = type & android.text.InputType.TYPE_MASK_CLASS;
        int variation = type & android.text.InputType.TYPE_MASK_VARIATION;
        if (cls == android.text.InputType.TYPE_CLASS_TEXT && (variation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
                || cls == android.text.InputType.TYPE_CLASS_NUMBER && variation == android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD) return true;
        String hints = n.getText() + " " + n.getHintText() + " " + n.getViewIdResourceName() + " " + n.getContentDescription();
        for (String key : n.getExtras().keySet()) {
            if (!key.toLowerCase(Locale.ROOT).contains("autofill")) continue;
            Object value = n.getExtras().get(key);
            hints += " " + (value instanceof Object[] ? java.util.Arrays.toString((Object[]) value) : value);
        }
        return SENSITIVE_INPUT.matcher(hints).find() || hints.matches("(?s).*[●•○◦]{3,}.*");
    }

    static boolean sensitiveDestination(BuddyService svc) {
        for (AccessibilityWindowInfo w : svc.getWindows()) {
            if (w.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) continue;
            AccessibilityNodeInfo root = w.getRoot();
            if (root == null) return true;
            // Settings uses the stricter protected-destination mutation policy.
            if (!isBuddy(root) && !settingsPackage(svc, String.valueOf(root.getPackageName())) && sensitiveWindow(root)) return true;
        }
        return false;
    }

    /** Refuse mutations on protected destinations, including input through the keyboard. */
    static boolean humanOnly(BuddyService svc) {
        if (Triggers.locked(svc) || sensitivePackage(svc, svc.foregroundPkg())) return true;
        for (AccessibilityWindowInfo w : svc.getWindows()) {
            int type = w.getType();
            if (type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY || type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) continue;
            if (type != AccessibilityWindowInfo.TYPE_APPLICATION && !w.isActive() && !w.isFocused()) continue;
            AccessibilityNodeInfo root = w.getRoot();
            if (root == null || offLimits(svc, root)) return true;
            if (root.getPackageName() != null && settingsPackage(svc, root.getPackageName().toString()) && (protectedSetting(svc, root.getPackageName(), w.getTitle()) || protectedSettingsPage(svc, root))) return true;
        }
        return false;
    }

    /**
     * The risky label of what would be tapped (the node, the button it belongs to, and that button's own
     * labels), or null when it looks harmless.
     */
    static String riskyLabel(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo cur = n;
        for (int i = 0; cur != null && i < 4; i++) {
            String hit = risky(cur);
            if (hit != null) return hit;
            if (cur.isClickable()) {
                for (int c = 0; c < Math.min(cur.getChildCount(), 6); c++) { // an icon button's label child
                    AccessibilityNodeInfo ch = cur.getChild(c);
                    String h = ch == null ? null : risky(ch);
                    if (h != null) return h;
                }
                break; // the button itself: don't look at its surroundings
            }
            cur = cur.getParent();
        }
        return null;
    }

    /** A web page's button label (the background browser, see BrowserTools), when it's a risky one; else null. */
    static String riskyText(String label) {
        if (label == null) return null;
        String s = label.trim();
        // as for a button on the phone: starts with the word and is short
        if (s.isEmpty() || s.length() > 30 || s.split("\\s+").length > 4) return null;
        return RISKY.matcher(s).find() ? s : null;
    }

    /** A web page button's id, name or test id says it sends, pays, buys or deletes ("add-to-cart-button"). */
    static boolean riskyId(String ids) {
        if (ids == null) return false;
        java.util.regex.Matcher m = RISKY_ID.matcher("_" + ids.toLowerCase(Locale.ROOT).replace(' ', '_'));
        while (m.find()) if (!"submit".equals(m.group(1))) return true; // on the web, every form's button is "submit"
        return false;
    }

    private static String risky(AccessibilityNodeInfo n) {
        for (CharSequence cs : new CharSequence[]{n.getText(), n.getContentDescription(), n.getTooltipText()}) {
            if (cs == null) continue;
            String s = cs.toString().trim();
            // a button label ("Send", "Pay now", "Delete chat"): starts with the word and is short, so a chat
            // preview like "Sarah, the audio call ended" isn't mistaken for a Call button
            if (s.isEmpty() || s.length() > 30 || s.split("\\s+").length > 4) continue;
            if (RISKY.matcher(s).find()) return s;
        }
        String id = n.getViewIdResourceName();
        if (id != null && RISKY_ID.matcher(id.toLowerCase(Locale.ROOT)).find()) {
            return id.substring(id.lastIndexOf('/') + 1).replace('_', ' ');
        }
        return null;
    }
}
