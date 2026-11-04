package io.honeydelight.manager;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Optional release notifier/downloader. Never requests root or reads learned models. */
public final class MainActivity extends Activity {
 static final String KEYBOARD = "com.samsung.android.honeyboard";

 enum State { IDLE, BUSY, FOUND, READY, EMPTY, ERROR }

 final ExecutorService worker = Executors.newSingleThreadExecutor();
 Updates.Release latest;
 State state = State.IDLE;
 boolean dark;

 // Palette
 int bg, surface, ink, muted, accent, onAccent, tonal, onTonal, outline, good, goodBg, bad, badBg;

 // Views that change with the state
 ImageView badgeIcon;
 FrameLayout badge;
 TextView status, detail, checked, compatValue, notifyHint;
 ProgressBar progress;
 Button primary, secondary, notifyButton;

 // ---------- helpers ----------

 int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }

 Drawable icon(String name, int tint) {
  int id = getResources().getIdentifier(name, "drawable", getPackageName());
  if (id == 0) return null;
  Drawable d = getDrawable(id).mutate();
  d.setTint(tint);
  return d;
 }

 GradientDrawable shape(int color, float radius) {
  GradientDrawable d = new GradientDrawable();
  d.setColor(color);
  d.setCornerRadius(dp(radius));
  return d;
 }

 TextView text(String value, float sp, int color, boolean bold) {
  TextView t = new TextView(this);
  t.setText(value);
  t.setTextSize(sp);
  t.setTextColor(color);
  t.setLineSpacing(0, 1.12f);
  if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
  return t;
 }

 LinearLayout.LayoutParams wide(int top) {
  LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
  p.topMargin = dp(top);
  return p;
 }

 LinearLayout card(LinearLayout parent) {
  LinearLayout c = new LinearLayout(this);
  c.setOrientation(LinearLayout.VERTICAL);
  c.setPadding(dp(20), dp(20), dp(20), dp(20));
  c.setBackground(shape(surface, 26));
  parent.addView(c, wide(12));
  return c;
 }

 /** Card header: tinted round icon + title. */
 void header(LinearLayout card, String iconName, String title) {
  LinearLayout row = new LinearLayout(this);
  row.setGravity(Gravity.CENTER_VERTICAL);
  FrameLayout circle = new FrameLayout(this);
  circle.setBackground(shape(tonal, 20));
  ImageView i = new ImageView(this);
  i.setImageDrawable(icon(iconName, onTonal));
  circle.addView(i, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
  row.addView(circle, new LinearLayout.LayoutParams(dp(40), dp(40)));
  TextView t = text(title, 19, ink, true);
  LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
  p.setMarginStart(dp(14));
  row.addView(t, p);
  card.addView(row);
 }

 TextView body(LinearLayout card, String value) {
  TextView t = text(value, 15, muted, false);
  card.addView(t, wide(10));
  return t;
 }

 /** Pill button. filled = primary honey, otherwise tonal. */
 Button pill(String label, String iconName, boolean filled, Runnable action) {
  Button b = new Button(this);
  b.setAllCaps(false);
  b.setTextSize(16);
  b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
  b.setStateListAnimator(null);
  b.setGravity(Gravity.CENTER);
  b.setMinHeight(dp(54));
  b.setMinimumHeight(dp(54));
  b.setPadding(dp(22), 0, dp(22), 0);
  int fill = filled ? accent : tonal, fg = filled ? onAccent : onTonal;
  b.setTextColor(fg);
  b.setBackground(new RippleDrawable(ColorStateList.valueOf((fg & 0x00ffffff) | 0x29000000), shape(fill, 27), shape(0xff000000, 27)));
  b.setCompoundDrawablePadding(dp(10));
  setLabel(b, label, iconName, fg);
  b.setOnClickListener(v -> action.run());
  return b;
 }

 void setLabel(Button b, String label, String iconName, int tint) {
  b.setText(label);
  Drawable d = iconName == null ? null : icon(iconName, tint);
  if (d != null) d.setBounds(0, 0, dp(20), dp(20));
  b.setCompoundDrawablesRelative(d, null, null, null);
 }

 void enable(View v, boolean on) {
  v.setEnabled(on);
  v.setAlpha(on ? 1f : 0.45f);
 }

 Button addButton(LinearLayout parent, Button b, int top) {
  parent.addView(b, wide(top));
  return b;
 }

 TextView infoRowView(LinearLayout card, String label, String value, boolean last) {
  LinearLayout row = new LinearLayout(this);
  row.setGravity(Gravity.CENTER_VERTICAL);
  row.setPadding(0, dp(12), 0, dp(12));
  TextView l = text(label, 15, muted, false);
  row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
  TextView v = text(value, 15, ink, true);
  v.setGravity(Gravity.END);
  LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f);
  row.addView(v, vp);
  card.addView(row, wide(0));
  if (!last) {
   View line = new View(this);
   line.setBackgroundColor(outline);
   card.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(0.7f))));
  }
  return v;
 }

 // ---------- screen ----------

 @Override public void onCreate(Bundle saved) {
  super.onCreate(saved);
  getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("updates", "Actualizaciones de HoneyDelight", NotificationManager.IMPORTANCE_DEFAULT));
  dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
  palette();

  getWindow().setStatusBarColor(bg);
  getWindow().setNavigationBarColor(bg);
  if (!dark) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

  ScrollView scroll = new ScrollView(this);
  scroll.setFillViewport(true);
  scroll.setBackgroundColor(bg);
  scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
  LinearLayout column = new LinearLayout(this);
  column.setOrientation(LinearLayout.VERTICAL);
  column.setPadding(dp(16), dp(20), dp(16), dp(36));
  scroll.addView(column);
  setContentView(scroll);

  hero(column);
  updatesCard(column);
  deviceCard(column);
  automaticCard(column);
  installCard(column);
  privacyCard(column);

  TextView footer = text("HoneyDelight Updates " + versionName() + " · SuperProgramer64", 13, muted, false);
  footer.setGravity(Gravity.CENTER);
  column.addView(footer, wide(22));

  restore();
  UpdateJob.schedule(this, Updates.prefs(this).getBoolean("automatic", true));
 }

 @Override protected void onResume() {
  super.onResume();
  refreshNotifications();
  refreshChecked();
 }

 void palette() {
  if (dark) {
   bg = 0xff12100d; surface = 0xff1f1b16; ink = 0xfff8eedd; muted = 0xffbfb29c;
   accent = 0xfff2be5c; onAccent = 0xff2b1d10; tonal = 0xff3a2f1f; onTonal = 0xffffd98a;
   outline = 0xff332b21; good = 0xff8fd9a8; goodBg = 0xff1e3626; bad = 0xffffb4a9; badBg = 0xff4a2420;
  } else {
   bg = 0xfff6f1e8; surface = 0xffffffff; ink = 0xff2b1d10; muted = 0xff6e5e48;
   accent = 0xffefb44f; onAccent = 0xff2b1d10; tonal = 0xfffbeccb; onTonal = 0xff6b4a12;
   outline = 0xffeee4d3; good = 0xff1e7a43; goodBg = 0xffdcf2e3; bad = 0xffb3261e; badBg = 0xfffbe0dc;
  }
 }

 void hero(LinearLayout column) {
  FrameLayout hero = new FrameLayout(this);
  GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xffffe7a6, 0xfff5c15f, 0xffd9962c});
  g.setCornerRadius(dp(30));
  hero.setBackground(g);
  hero.setClipToOutline(true);

  // Soft light wash, like the banner
  View wash = new View(this);
  GradientDrawable w = new GradientDrawable();
  w.setShape(GradientDrawable.OVAL);
  w.setColor(0x33ffffff);
  wash.setBackground(w);
  FrameLayout.LayoutParams wp = new FrameLayout.LayoutParams(dp(300), dp(220));
  wp.leftMargin = -dp(90);
  wp.topMargin = -dp(110);
  hero.addView(wash, wp);

  sparkle(hero, 26, Gravity.TOP | Gravity.END, 22, 22, 0xffffffff, 0.7f);
  sparkle(hero, 16, Gravity.TOP | Gravity.END, 58, 18, 0xffffffff, 0.75f);
  sparkle(hero, 54, Gravity.BOTTOM | Gravity.END, 24, 22, 0xffffffff, 0.35f);

  LinearLayout inner = new LinearLayout(this);
  inner.setOrientation(LinearLayout.VERTICAL);
  inner.setPadding(dp(22), dp(22), dp(22), dp(22));

  ImageView logo = new ImageView(this);
  logo.setImageDrawable(getDrawable(getResources().getIdentifier("ic_honey", "drawable", getPackageName())));
  inner.addView(logo, new LinearLayout.LayoutParams(dp(46), dp(46)));

  LinearLayout titleRow = new LinearLayout(this);
  titleRow.setGravity(Gravity.CENTER_VERTICAL);
  TextView title = text("HoneyDelight", 32, 0xff2b1d10, true);
  title.setLetterSpacing(-0.01f);
  titleRow.addView(title);
  ImageView spark = new ImageView(this);
  spark.setImageDrawable(getDrawable(getResources().getIdentifier("ic_spark_blue", "drawable", getPackageName())));
  LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(34), dp(34));
  sp.setMarginStart(dp(8));
  sp.bottomMargin = dp(10);
  titleRow.addView(spark, sp);
  inner.addView(titleRow, wide(14));
  TextView slogan = text("Se ve como Samsung. Predice con Delight.", 16, 0xff4a3519, false);
  inner.addView(slogan, wide(2));

  TextView chip = text("ALPHA · ACTUALIZACIONES", 11, 0xffffe6a0, true);
  chip.setLetterSpacing(0.12f);
  chip.setPadding(dp(12), dp(6), dp(12), dp(6));
  chip.setBackground(shape(0xff2b1d10, 14));
  LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
  cp.topMargin = dp(16);
  inner.addView(chip, cp);

  hero.addView(inner, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
  column.addView(hero, wide(0));
 }

 void sparkle(FrameLayout parent, int size, int gravity, int marginX, int marginY, int color, float alpha) {
  ImageView s = new ImageView(this);
  s.setImageDrawable(icon("ic_sparkle", color));
  s.setAlpha(alpha);
  FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(size), dp(size), gravity);
  p.setMarginEnd(dp(marginX));
  if ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM) p.bottomMargin = dp(marginY); else p.topMargin = dp(marginY);
  parent.addView(s, p);
 }

 void updatesCard(LinearLayout column) {
  LinearLayout c = card(column);
  LinearLayout row = new LinearLayout(this);
  row.setGravity(Gravity.CENTER_VERTICAL);
  badge = new FrameLayout(this);
  badgeIcon = new ImageView(this);
  badge.addView(badgeIcon, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
  row.addView(badge, new LinearLayout.LayoutParams(dp(48), dp(48)));
  LinearLayout texts = new LinearLayout(this);
  texts.setOrientation(LinearLayout.VERTICAL);
  status = text("", 20, ink, true);
  texts.addView(status);
  checked = text("", 13, muted, false);
  texts.addView(checked, wide(2));
  LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
  tp.setMarginStart(dp(14));
  row.addView(texts, tp);
  c.addView(row);

  detail = body(c, "");

  progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
  progress.setIndeterminate(true);
  progress.setIndeterminateTintList(ColorStateList.valueOf(accent));
  progress.setVisibility(View.GONE);
  LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
  pp.topMargin = dp(14);
  c.addView(progress, pp);

  primary = addButton(c, pill("", null, true, this::primaryAction), 16);
  secondary = addButton(c, pill("", null, false, this::secondaryAction), 10);
 }

 void deviceCard(LinearLayout column) {
  LinearLayout c = card(column);
  header(c, "ic_phone", "Tu teléfono");
  body(c, "El módulo se publica para una compilación exacta. Estos datos deben coincidir con el ZIP.");
  LinearLayout rows = new LinearLayout(this);
  rows.setOrientation(LinearLayout.VERTICAL);
  c.addView(rows, wide(6));
  infoRowView(rows, "Modelo", Build.MODEL, false);
  infoRowView(rows, "Android", Build.VERSION.RELEASE + " · API " + Build.VERSION.SDK_INT, false);
  infoRowView(rows, "Samsung Keyboard", keyboardVersion(), false);
  compatValue = infoRowView(rows, "Compatibilidad", "Calculando…", true);
  TextView hint = text("Toca el identificador para copiarlo si vas a reportar un fallo.", 13, muted, false);
  c.addView(hint, wide(4));
  compatValue.setOnClickListener(v -> {
   CharSequence id = compatValue.getText();
   if (id.length() != 12) return;
   getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("HoneyDelight", id));
   Toast.makeText(this, "Identificador copiado", Toast.LENGTH_SHORT).show();
  });
  worker.execute(() -> {
   String id;
   try {
    String apk = getPackageManager().getApplicationInfo(KEYBOARD, 0).sourceDir;
    id = UpdatePolicy.compatibilityId(Build.FINGERPRINT, Updates.hash(new File(apk)));
   } catch (Exception e) { id = "No disponible"; }
   final String value = id;
   runOnUiThread(() -> { if (!isDestroyed()) compatValue.setText(value); });
  });
 }

 void automaticCard(LinearLayout column) {
  LinearLayout c = card(column);
  header(c, "ic_bell", "Sin estar pendiente");
  LinearLayout row = new LinearLayout(this);
  row.setGravity(Gravity.CENTER_VERTICAL);
  LinearLayout texts = new LinearLayout(this);
  texts.setOrientation(LinearLayout.VERTICAL);
  texts.addView(text("Descargar automáticamente", 16, ink, true));
  texts.addView(text("Cada ~6 horas, solo por Wi-Fi o redes sin medición. La instalación siempre la decides tú.", 14, muted, false), wide(4));
  row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
  Switch toggle = new Switch(this);
  int[][] states = {{android.R.attr.state_checked}, {}};
  toggle.setThumbTintList(new ColorStateList(states, new int[]{accent, dark ? 0xffcfc4b3 : 0xffffffff}));
  toggle.setTrackTintList(new ColorStateList(states, new int[]{(accent & 0x00ffffff) | 0x80000000, dark ? 0xff4a4136 : 0xffd9cfbf}));
  toggle.setChecked(Updates.prefs(this).getBoolean("automatic", true));
  LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
  sp.setMarginStart(dp(12));
  row.addView(toggle, sp);
  row.setOnClickListener(v -> toggle.toggle());
  c.addView(row, wide(14));
  toggle.setOnCheckedChangeListener((b, on) -> {
   Updates.prefs(this).edit().putBoolean("automatic", on).apply();
   UpdateJob.schedule(this, on);
   if (on) askNotifications();
  });
  notifyHint = text("Activa los avisos para enterarte cuando el ZIP esté listo.", 14, muted, false);
  c.addView(notifyHint, wide(14));
  notifyButton = addButton(c, pill("Activar avisos", "ic_bell", false, this::askNotifications), 10);
 }

 void installCard(LinearLayout column) {
  LinearLayout c = card(column);
  header(c, "ic_package", "Instalar el módulo");
  body(c, "La app es opcional: también puedes bajar el ZIP desde GitHub e instalarlo sin ella.");
  step(c, "1", "Descarga el ZIP verificado para tu teléfono.");
  step(c, "2", "Ábrelo en KernelSU o Magisk con «Instalar desde almacenamiento».");
  step(c, "3", "Reinicia cuando el gestor te lo pida.");
  addButton(c, pill("Ver publicaciones en GitHub", "ic_link", false, () -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Updates.PAGE)))), 16);
 }

 void step(LinearLayout card, String n, String value) {
  LinearLayout row = new LinearLayout(this);
  row.setGravity(Gravity.CENTER_VERTICAL);
  TextView num = text(n, 14, onAccent, true);
  num.setGravity(Gravity.CENTER);
  num.setBackground(shape(accent, 14));
  row.addView(num, new LinearLayout.LayoutParams(dp(28), dp(28)));
  TextView t = text(value, 15, ink, false);
  LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
  p.setMarginStart(dp(12));
  row.addView(t, p);
  card.addView(row, wide(14));
 }

 void privacyCard(LinearLayout column) {
  LinearLayout c = card(column);
  header(c, "ic_shield", "Tus palabras se quedan contigo");
  body(c, "Esta app no usa root ni lee historiales, contactos o modelos aprendidos. Solo se conecta a GitHub para consultar y descargar publicaciones, y comprueba el SHA-256 de cada ZIP antes de guardarlo.");
 }

 // ---------- state ----------

 void restore() {
  String uri = Updates.prefs(this).getString("uri", null);
  if (uri != null && Updates.exists(this, uri)) {
   render(State.READY, "Módulo listo", Updates.prefs(this).getString("asset", "ZIP verificado") + "\nGuardado en Descargas/HoneyDelight.");
  } else {
   render(State.IDLE, "Busca tu versión", "Consulta las publicaciones de HoneyDelight y descarga solo el módulo hecho para tu compilación.");
  }
 }

 void render(State s, String title, String message) {
  state = s;
  status.setText(title);
  detail.setText(message);
  progress.setVisibility(s == State.BUSY ? View.VISIBLE : View.GONE);

  int bgc = tonal, fg = onTonal; String ic = "ic_search";
  switch (s) {
   case BUSY: ic = "ic_refresh"; break;
   case FOUND: ic = "ic_download"; break;
   case READY: ic = "ic_check"; bgc = goodBg; fg = good; break;
   case ERROR: ic = "ic_alert"; bgc = badBg; fg = bad; break;
   case EMPTY: ic = "ic_package"; break;
   default: break;
  }
  badge.setBackground(shape(bgc, 24));
  badgeIcon.setImageDrawable(icon(ic, fg));

  boolean hasFile = Updates.exists(this, Updates.prefs(this).getString("uri", null));
  switch (s) {
   case BUSY:
    setLabel(primary, "Un momento…", null, onAccent); enable(primary, false);
    secondary.setVisibility(View.GONE);
    break;
   case FOUND:
    setLabel(primary, "Descargar " + latest.tag, "ic_download", onAccent); enable(primary, true);
    showSecondary(hasFile ? "Abrir el ZIP anterior" : "Buscar de nuevo", hasFile ? "ic_open" : "ic_refresh");
    break;
   case READY:
    setLabel(primary, "Abrir con el gestor root", "ic_open", onAccent); enable(primary, true);
    showSecondary("Buscar actualizaciones", "ic_refresh");
    break;
   default:
    setLabel(primary, s == State.IDLE ? "Buscar actualizaciones" : "Buscar de nuevo", s == State.IDLE ? "ic_search" : "ic_refresh", onAccent); enable(primary, true);
    if (hasFile) showSecondary("Abrir el ZIP descargado", "ic_open"); else secondary.setVisibility(View.GONE);
  }
  refreshChecked();
 }

 void showSecondary(String label, String ic) {
  setLabel(secondary, label, ic, onTonal);
  enable(secondary, true);
  secondary.setVisibility(View.VISIBLE);
 }

 void primaryAction() {
  if (state == State.BUSY) return;
  if (state == State.FOUND) download();
  else if (state == State.READY) open();
  else check();
 }

 void secondaryAction() {
  if (state == State.BUSY) return;
  String label = secondary.getText().toString();
  if (label.startsWith("Abrir")) open(); else check();
 }

 void refreshChecked() {
  if (checked == null) return;
  long at = Updates.prefs(this).getLong("checked", 0);
  long ago = System.currentTimeMillis() - at;
  checked.setText(at == 0 ? "Aún no has buscado" : ago < 60_000 ? "Comprobado ahora mismo" : "Última comprobación " +DateUtils.getRelativeTimeSpanString(at, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString().toLowerCase());
 }

 void refreshNotifications() {
  if (notifyButton == null) return;
  boolean granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
  notifyButton.setVisibility(granted ? View.GONE : View.VISIBLE);
  notifyHint.setText(granted ? "Los avisos están activados. Te escribiremos cuando haya un ZIP verificado." : "Activa los avisos para enterarte cuando el ZIP esté listo.");
 }

 @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
  super.onRequestPermissionsResult(code, permissions, results);
  refreshNotifications();
 }

 void askNotifications() {
  if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
   requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
  else Toast.makeText(this, "Los avisos ya están activados", Toast.LENGTH_SHORT).show();
 }

 void check() {
  render(State.BUSY, "Buscando actualizaciones…", "Consultando las publicaciones oficiales en GitHub.");
  worker.execute(() -> {
   Updates.Release release = null; String error = null;
   try { release = Updates.latest(this); Updates.prefs(this).edit().putLong("checked", System.currentTimeMillis()).apply(); }
   catch (Exception e) { error = e.getMessage(); }
   final Updates.Release found = release; final String issue = error;
   runOnUiThread(() -> {
    if (isFinishing() || isDestroyed()) return;
    latest = found;
    if (issue != null) render(State.ERROR, "No se pudo completar", issue);
    else if (found == null) render(State.EMPTY, "Todavía no hay publicaciones", "Cuando salga la primera versión compatible con tu teléfono aparecerá aquí.");
    else if (found.tag.equals(Updates.prefs(this).getString("tag", null)) && found.name.equals(Updates.prefs(this).getString("asset", null)) && Updates.exists(this, Updates.prefs(this).getString("uri", null)))
     render(State.READY, "Ya tienes la última", found.name + "\nEstá en Descargas/HoneyDelight.");
    else render(State.FOUND, "Disponible: " + found.tag, found.name + "\nCoincide con tu compilación y se verificará antes de guardarlo.");
   });
  });
 }

 void download() {
  if (latest == null) return;
  final Updates.Release release = latest;
  render(State.BUSY, "Descargando " + release.tag + "…", "Se comprobará el SHA-256 antes de guardar el ZIP.");
  worker.execute(() -> {
   String error = null;
   try { Updates.download(this, release); } catch (Exception e) { error = e.getMessage(); }
   final String issue = error;
   runOnUiThread(() -> {
    if (isFinishing() || isDestroyed()) return;
    if (issue == null) render(State.READY, "Módulo verificado", release.name + "\nGuardado en Descargas/HoneyDelight. Ábrelo con tu gestor root cuando quieras.");
    else render(State.ERROR, "Descarga no completada", issue);
   });
  });
 }

 void open() {
  String value = Updates.prefs(this).getString("uri", null);
  if (value == null) return;
  try {
   Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(value), "application/zip").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
   startActivity(Intent.createChooser(i, "Abrir módulo con…"));
  } catch (Exception e) {
   Toast.makeText(this, "Abre Descargas/HoneyDelight desde tu gestor root", Toast.LENGTH_LONG).show();
  }
 }

 String keyboardVersion() {
  try { return getPackageManager().getPackageInfo(KEYBOARD, 0).versionName; }
  catch (Exception e) { return "No instalado"; }
 }

 String versionName() {
  try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
  catch (Exception e) { return ""; }
 }

 @Override protected void onDestroy() { super.onDestroy(); worker.shutdown(); }
}
