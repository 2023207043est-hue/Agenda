package com.agendapersonal.settings;

import com.agendapersonal.infrastructure.db.ConfigDAO;

/** Guarda el JSON de ajustes en la tabla config existente (una sola fila). */
final class ConfigStorage implements SettingsStore.Storage {
    private final ConfigDAO dao = new ConfigDAO();
    @Override public String get(String key, String def) { return dao.get(key, def); }
    @Override public void set(String key, String value) { dao.set(key, value); }
}
