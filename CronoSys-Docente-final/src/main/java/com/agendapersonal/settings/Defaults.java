package com.agendapersonal.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Valores por defecto de todos los ajustes. Con ellos la app se ve y funciona como antes. */
public final class Defaults {
    private Defaults() {}

    public static Map<String, Object> settings() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("appearance", m(
            "themeId", "claro", "mode", "claro", "accentId", "NARANJA", "colors", m(),   // colors: bg, card, text, accent (vacío = del tema)
            "font", "Plus Jakarta Sans", "fontScale", 100, "density", "comoda", "radius", 12,
            "warmLowContrast", false, "reduceMotion", false, "autoThemeByTime", false,
            "autoDarkFrom", "19:00", "autoDarkTo", "07:00"));
        s.put("sidebar", m("order", new ArrayList<>(), "hidden", new ArrayList<>(), "collapsed", false));
        s.put("home", m("columns", 2, "widgetSize", "mediano", "widgets", new ArrayList<>(), "showGreeting", true, "bigClock", false,
            "lockEdit", false, "userName", "", "phrase", "", "showSeconds", false, "hour24", true));
        s.put("calendar", m("defaultView", "mes", "weekStart", "lunes", "hoursFrom", 6, "hoursTo", 23, "timezone", "", "calendarColors", m(),
            "density", "comoda", "hourHeight", 48, "showWeekends", true, "showWeekNumbers", false, "dimPast", false));
        s.put("timeBlocking", m("presets", new ArrayList<>(), "activePreset", 0, "categories", new ArrayList<>(), "sound", true, "volume", 60,
            "background", "degradado", "backgroundFile", "", "backgroundColor", "#1B1E25", "backgroundLoop", true, "backgroundDim", 35,
            "audioFile", "", "clockSize", "mediano", "showProgress", true, "showDetail", true, "showTasks", true, "todoSource", "ambas",
            "importance", "NORMAL", "pauseOnLeave", false, "notifyEnd", true));
        s.put("study", m("widgets", new ArrayList<>(), "columns", 2));
        s.put("tasks", m("view", "lista", "groupBy", "ninguno", "visibleTabs", List.of("Todas", "Hoy", "Próximas", "Vencidas", "Completadas"),
            "priorityColors", m(), "extraFields", new ArrayList<>(), "subtasks", true, "completedLast", false, "tabCounters", true));
        s.put("notes", m("types", new ArrayList<>(), "defaultTemplate", "", "editorFont", "", "editorWidth", "ancho", "tagColor", "",
            "markdown", true, "autosave", true, "livePreview", false));
        s.put("contacts", m("formFields", new ArrayList<>(), "categories", new ArrayList<>(), "avatar", "iniciales", "sortBy", "nombre",
            "remindEveryDays", 0, "birthdayAlerts", true));
        s.put("habits", m("defaultFrequency", "diaria", "defaultColor", "", "dailyGoal", 3, "view", "semana", "weekStart", "lunes",
            "reminderTime", "", "streakCountsWeekends", true, "confetti", true));
        s.put("finance", m("currency", "S/", "categories", new ArrayList<>(), "monthlyBudget", 0, "numberFormat", "1,234.56",
            "period", "mes", "hideAmounts", false, "budgetAlert", true));
        s.put("health", m("visibleMetrics", new ArrayList<>(), "goals", m("water", 8, "exercise", 30), "weightUnit", "kg",
            "waterUnit", "vasos", "waterReminder", false, "weeklySummary", false));
        s.put("notifications", m("eventStart", true, "birthdays", true, "confirmations", false, "quietHours", m("from", "", "to", "")));
        return s;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> m(Object... kv) {
        Map<String, Object> r = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) r.put((String) kv[i], kv[i + 1]);
        return r;
    }
}
